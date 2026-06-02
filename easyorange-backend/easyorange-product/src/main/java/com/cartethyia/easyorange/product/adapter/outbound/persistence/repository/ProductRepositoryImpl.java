package com.cartethyia.easyorange.product.adapter.outbound.persistence.repository;

import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.framework.exception.ConcurrentUpdateException;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.converter.ProductConverter;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductConverter converter;

    public ProductRepositoryImpl(ProductMapper productMapper,
                                  ProductDetailMapper productDetailMapper,
                                  ProductImageMapper productImageMapper,
                                  ProductConverter converter) {
        this.productMapper = productMapper;
        this.productDetailMapper = productDetailMapper;
        this.productImageMapper = productImageMapper;
        this.converter = converter;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        ProductDO productDO = productMapper.selectById(id.value());
        if (productDO == null) {
            return Optional.empty();
        }
        ProductDetailDO detailDO = productDetailMapper.selectById(productDO.getId());
        List<ProductImageDO> imageDOs = ChainWrappers.lambdaQueryChain(productImageMapper)
                .eq(ProductImageDO::getProductId, productDO.getId())
                .orderByAsc(ProductImageDO::getSortOrder)
                .list();
        return Optional.of(converter.toDomain(productDO, detailDO, imageDOs));
    }

    @Override
    public List<Product> findByIds(List<ProductId> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> idValues = ids.stream().map(ProductId::value).collect(Collectors.toList());
        List<ProductDO> productDOs = productMapper.selectBatchIds(idValues);
        if (productDOs.isEmpty()) {
            return List.of();
        }
        return batchConvertProducts(productDOs);
    }

    @Override
    public List<Product> findBySellerId(SellerId sellerId) {
        List<ProductDO> productDOs = ChainWrappers.lambdaQueryChain(productMapper)
                .eq(ProductDO::getUserId, sellerId.value())
                .orderByDesc(ProductDO::getCreateTime)
                .list();
        if (productDOs.isEmpty()) {
            return List.of();
        }
        return batchConvertProducts(productDOs);
    }

    private List<Product> batchConvertProducts(List<ProductDO> productDOs) {
        List<Long> productIds = productDOs.stream()
                .map(ProductDO::getId)
                .collect(Collectors.toList());

        Map<Long, ProductDetailDO> detailMap = productDetailMapper
                .selectDetailsByProductIds(productIds).stream()
                .collect(Collectors.toMap(ProductDetailDO::getProductId, d -> d, (a, b) -> a));

        Map<Long, List<ProductImageDO>> imagesByProduct = ChainWrappers.lambdaQueryChain(productImageMapper)
                .in(ProductImageDO::getProductId, productIds)
                .orderByAsc(ProductImageDO::getSortOrder)
                .list().stream().collect(Collectors.groupingBy(ProductImageDO::getProductId));

        return productDOs.stream()
                .map(productDO -> converter.toDomain(
                        productDO,
                        detailMap.get(productDO.getId()),
                        imagesByProduct.getOrDefault(productDO.getId(), List.of())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Product save(Product product) {
        ProductDO productDO = converter.toDataObject(product);
        productMapper.insert(productDO);
        Product saved = product.assignId(productDO.getId());

        ProductDetailDO detailDO = converter.toDetailDO(saved.getId(), saved.getDescription());
        if (detailDO != null) {
            productDetailMapper.insert(detailDO);
        }

        List<ProductImageDO> imageDOs = converter.toImageDOs(saved.getId(), saved.getImages());
        if (!imageDOs.isEmpty()) {
            productImageMapper.batchInsert(imageDOs);
        }

        return saved;
    }

    @Override
    public void update(Product product) {
        if (productMapper.updateById(converter.toDataObject(product)) == 0) {
            throw new ConcurrentUpdateException("商品更新冲突: id=" + product.getId().value());
        }
        updateProductDetail(product);
        updateImagesDifferentially(product);
    }

    private void updateProductDetail(Product product) {
        ProductDetailDO detailDO = converter.toDetailDO(product.getId(), product.getDescription());
        if (detailDO == null) {
            return;
        }
        ProductDetailDO existingDetail = productDetailMapper.selectById(product.getId().value());
        if (existingDetail != null) {
            existingDetail.setDescription(detailDO.getDescription());
            productDetailMapper.updateById(existingDetail);
        } else {
            productDetailMapper.insert(detailDO);
        }
    }

    private void updateImagesDifferentially(Product product) {
        Long productId = product.getId().value();

        List<ProductImageDO> existingImages = ChainWrappers.lambdaQueryChain(productImageMapper)
                .eq(ProductImageDO::getProductId, productId)
                .list();

        List<ProductImageDO> newImages = converter.toImageDOs(product.getId(), product.getImages());

        Set<String> existingUrls = existingImages.stream()
                .map(ProductImageDO::getImageUrl)
                .collect(Collectors.toSet());

        Set<String> newUrls = newImages.stream()
                .map(ProductImageDO::getImageUrl)
                .collect(Collectors.toSet());

        Set<String> urlsToDelete = new HashSet<>(existingUrls);
        urlsToDelete.removeAll(newUrls);

        if (!urlsToDelete.isEmpty()) {
            productImageMapper.deleteByProductIdAndUrls(productId, new ArrayList<>(urlsToDelete));
        }

        Set<String> urlsToAdd = new HashSet<>(newUrls);
        urlsToAdd.removeAll(existingUrls);

        if (!urlsToAdd.isEmpty()) {
            List<ProductImageDO> imagesToAdd = newImages.stream()
                    .filter(img -> urlsToAdd.contains(img.getImageUrl()))
                    .collect(Collectors.toList());
            productImageMapper.batchInsert(imagesToAdd);
        }
    }

    @Override
    public void delete(ProductId id) {
        productMapper.deleteById(id.value());
        productDetailMapper.deleteById(id.value());
        ChainWrappers.lambdaUpdateChain(productImageMapper)
                .eq(ProductImageDO::getProductId, id.value())
                .remove();
    }

    @Override
    public boolean existsById(ProductId id) {
        return productMapper.selectById(id.value()) != null;
    }

    @Override
    public void updateStatus(ProductId id, ProductStatus status) {
        ProductDO productDO = productMapper.selectById(id.value());
        if (productDO == null) {
            throw new ProductNotFoundException(id);
        }
        productDO.setStatus(status.getCode());
        if (productMapper.updateById(productDO) == 0) {
            throw new ConcurrentUpdateException("商品状态更新冲突: id=" + id.value());
        }
    }
}
