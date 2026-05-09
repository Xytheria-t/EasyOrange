package com.cartethyia.easyorange.product.adapter.outbound.persistence.repository;

import com.cartethyia.easyorange.product.domain.aggregate.Product;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
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
        List<ProductImageDO> imageDOs = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImageDO>()
                        .eq(ProductImageDO::getProductId, productDO.getId())
                        .orderByAsc(ProductImageDO::getSortOrder)
        );
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
        List<ProductDO> productDOs = productMapper.selectList(
                new LambdaQueryWrapper<ProductDO>()
                        .eq(ProductDO::getUserId, sellerId.value())
                        .orderByDesc(ProductDO::getCreateTime)
        );
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

        Map<Long, List<ProductImageDO>> imagesByProduct = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImageDO>()
                        .in(ProductImageDO::getProductId, productIds)
                        .orderByAsc(ProductImageDO::getSortOrder)
        ).stream().collect(Collectors.groupingBy(ProductImageDO::getProductId));

        return productDOs.stream()
                .map(productDO -> converter.toDomain(
                        productDO,
                        detailMap.get(productDO.getId()),
                        imagesByProduct.getOrDefault(productDO.getId(), Collections.emptyList())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void save(Product product) {
        ProductDO productDO = converter.toDataObject(product);
        productMapper.insert(productDO);
        product.assignId(productDO.getId());

        ProductDetailDO detailDO = converter.toDetailDO(product.getId(), product.getDescription());
        if (detailDO != null) {
            productDetailMapper.insert(detailDO);
        }

        List<ProductImageDO> imageDOs = converter.toImageDOs(product.getId(), product.getImages());
        if (!imageDOs.isEmpty()) {
            productImageMapper.batchInsert(imageDOs);
        }
    }

    @Override
    public void update(Product product) {
        ProductDO productDO = converter.toDataObject(product);
        productMapper.updateById(productDO);

        ProductDetailDO existingDetail = productDetailMapper.selectById(product.getId().value());
        ProductDetailDO detailDO = converter.toDetailDO(product.getId(), product.getDescription());
        if (detailDO != null) {
            if (existingDetail != null) {
                existingDetail.setDescription(detailDO.getDescription());
                productDetailMapper.updateById(existingDetail);
            } else {
                productDetailMapper.insert(detailDO);
            }
        }

        updateImagesDifferentially(product);
    }

    private void updateImagesDifferentially(Product product) {
        Long productId = product.getId().value();

        List<ProductImageDO> existingImages = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImageDO>()
                        .eq(ProductImageDO::getProductId, productId)
        );

        List<ProductImageDO> newImages = converter.toImageDOs(product.getId(), product.getImages());

        Set<String> existingUrls = existingImages.stream()
                .map(ProductImageDO::getImageUrl)
                .collect(Collectors.toSet());

        Set<String> newUrls = newImages.stream()
                .map(ProductImageDO::getImageUrl)
                .collect(Collectors.toSet());

        Set<String> urlsToDelete = new HashSet<>(existingUrls);
        urlsToDelete.removeAll(newUrls);

        Set<String> urlsToAdd = new HashSet<>(newUrls);
        urlsToAdd.removeAll(existingUrls);

        if (!urlsToDelete.isEmpty()) {
            productImageMapper.deleteByProductIdAndUrls(productId, new ArrayList<>(urlsToDelete));
        }

        if (!urlsToAdd.isEmpty()) {
            List<ProductImageDO> imagesToInsert = newImages.stream()
                    .filter(img -> urlsToAdd.contains(img.getImageUrl()))
                    .collect(Collectors.toList());
            productImageMapper.batchInsert(imagesToInsert);
        }
    }

    @Override
    public void delete(ProductId id) {
        productMapper.deleteById(id.value());
        productDetailMapper.deleteById(id.value());
        productImageMapper.delete(
                new LambdaQueryWrapper<ProductImageDO>()
                        .eq(ProductImageDO::getProductId, id.value())
        );
    }

    @Override
    public boolean existsById(ProductId id) {
        return productMapper.selectById(id.value()) != null;
    }

    @Override
    public void updateStatus(ProductId id, ProductStatus status) {
        ProductDO productDO = productMapper.selectById(id.value());
        if (productDO != null) {
            productDO.setStatus(status.getCode());
            productMapper.updateById(productDO);
        }
    }
}
