package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.common.exception.ConcurrentUpdateException;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Primary
@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductDataMapper dataMapper;

    public ProductRepositoryImpl(ProductMapper productMapper,
                                  ProductDetailMapper productDetailMapper,
                                  ProductImageMapper productImageMapper,
                                  ProductDataMapper dataMapper) {
        this.productMapper = productMapper;
        this.productDetailMapper = productDetailMapper;
        this.productImageMapper = productImageMapper;
        this.dataMapper = dataMapper;
    }

    // ========== 写 ==========

    @Override
    public Product create(Product product) {
        ProductDO productDO = dataMapper.toDataObject(product);
        productMapper.insert(productDO);
        Product created = product.assignId(productDO.getId());

        ProductDetailDO detailDO = dataMapper.toDetailDO(created.getId(), created.getDescription());
        if (detailDO != null) {
            productDetailMapper.insert(detailDO);
        }

        List<ProductImageDO> imageDOs = dataMapper.toImageDOs(created.getId(), created.getImages());
        if (!imageDOs.isEmpty()) {
            productImageMapper.batchInsert(imageDOs);
        }

        return created;
    }

    @Override
    public void update(Product product) {
        if (productMapper.updateById(dataMapper.toDataObject(product)) == 0) {
            throw new ConcurrentUpdateException("商品更新冲突: id=" + product.getId().value());
        }
        updateProductDetail(product);
        updateImagesDifferentially(product);
    }

    @Override
    public void delete(ProductId id) {
        productMapper.deleteById(id.value());
        productDetailMapper.deleteById(id.value());
        productImageMapper.delete(Wrappers.<ProductImageDO>lambdaQuery()
                .eq(ProductImageDO::getProductId, id.value()));
    }

    // ========== 读 ==========

    @Override
    public Optional<Product> findById(ProductId id) {
        ProductDO productDO = productMapper.selectById(id.value());
        if (productDO == null) {
            return Optional.empty();
        }
        ProductDetailDO detailDO = productDetailMapper.selectById(productDO.getId());
        List<ProductImageDO> imageDOs = productMapper.selectImagesByProductIds(List.of(productDO.getId()));
        return Optional.of(dataMapper.toDomain(productDO, detailDO, imageDOs));
    }

    @Override
    public List<Product> findByIds(List<ProductId> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ProductDO> productDOs = productMapper.selectList(
                Wrappers.<ProductDO>lambdaQuery()
                        .in(ProductDO::getId, ids.stream().map(ProductId::value).toList()));
        if (productDOs.isEmpty()) {
            return List.of();
        }
        return batchConvertProducts(productDOs);
    }

    // ========== 私有辅助 ==========

    private List<Product> batchConvertProducts(List<ProductDO> productDOs) {
        List<String> productIds = productDOs.stream().map(ProductDO::getId).toList();

        Map<String, ProductDetailDO> detailMap = productDetailMapper
                .selectDetailsByProductIds(productIds).stream()
                .collect(Collectors.toMap(ProductDetailDO::getProductId, d -> d, (a, _) -> a));

        Map<String, List<ProductImageDO>> imagesByProduct = productMapper
                .selectImagesByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(ProductImageDO::getProductId));

        return productDOs.stream()
                .map(productDO -> dataMapper.toDomain(
                        productDO,
                        detailMap.get(productDO.getId()),
                        imagesByProduct.getOrDefault(productDO.getId(), List.of())
                ))
                .toList();
    }

    private void updateProductDetail(Product product) {
        ProductDetailDO detailDO = dataMapper.toDetailDO(product.getId(), product.getDescription());
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
        String productId = product.getId().value();

        List<ProductImageDO> existingImages = productMapper.selectImagesByProductIds(List.of(productId));
        List<ProductImageDO> newImages = dataMapper.toImageDOs(product.getId(), product.getImages());

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
                    .toList();
            productImageMapper.batchInsert(imagesToAdd);
        }
    }
}
