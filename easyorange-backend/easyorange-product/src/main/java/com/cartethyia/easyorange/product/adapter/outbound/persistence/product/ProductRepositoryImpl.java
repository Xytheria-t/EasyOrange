package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.exception.ConcurrentUpdateException;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class ProductRepositoryImpl extends BaseRepository<ProductMapper, ProductDO> implements ProductRepository {

    private final ProductDetailMapper productDetailMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductDataMapper dataMapper;

    public ProductRepositoryImpl(
            ProductMapper productMapper,
            ProductDetailMapper productDetailMapper,
            ProductImageMapper productImageMapper,
            ProductDataMapper dataMapper) {
        super(productMapper);
        this.productDetailMapper = productDetailMapper;
        this.productImageMapper = productImageMapper;
        this.dataMapper = dataMapper;
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            return insert(product);
        }
        return update(product);
    }

    @Override
    public void delete(ProductId id) {
        mapper.deleteById(id.value());
        productDetailMapper.deleteById(id.value());
        productImageMapper.delete(Wrappers.<ProductImageDO>lambdaQuery().eq(ProductImageDO::getProductId, id.value()));
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        var productDO = mapper.selectById(id.value());
        if (productDO == null) {
            return Optional.empty();
        }
        var detailDO = productDetailMapper.selectById(productDO.getId());
        var imageDOs = mapper.selectImagesByProductIds(List.of(productDO.getId()));
        return Optional.of(dataMapper.toDomain(productDO, detailDO, imageDOs));
    }

    @Override
    public List<Product> findByIds(List<ProductId> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        var productDOs = lambdaQuery()
                .in(ProductDO::getId, ids.stream().map(ProductId::value).toList())
                .list();
        return productDOs.isEmpty() ? List.of() : toDomainList(productDOs);
    }

    private Product insert(Product product) {
        var productDO = dataMapper.toDataObject(product);
        mapper.insert(productDO);
        var saved = product.assignId(productDO.getId());
        var detailDO = dataMapper.toDetailDO(saved.getId(), saved.getDescription());
        if (detailDO != null) {
            productDetailMapper.insert(detailDO);
        }
        var imageDOs = dataMapper.toImageDOs(saved.getId(), saved.getImages());
        if (!imageDOs.isEmpty()) {
            productImageMapper.batchInsert(imageDOs);
        }
        return saved;
    }

    private Product update(Product product) {
        if (mapper.updateById(dataMapper.toDataObject(product)) == 0) {
            throw new ConcurrentUpdateException("商品更新冲突: id=" + product.getId().value());
        }
        upsertDetail(product);
        syncImages(product);
        return product;
    }

    private List<Product> toDomainList(List<ProductDO> productDOs) {
        var productIds = productDOs.stream().map(ProductDO::getId).toList();
        var detailMap = productDetailMapper.selectDetailsByProductIds(productIds).stream()
                .collect(Collectors.toMap(ProductDetailDO::getProductId, d -> d, (a, _) -> a));
        var imagesByProduct = mapper.selectImagesByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(ProductImageDO::getProductId));
        return productDOs.stream()
                .map(productDO -> dataMapper.toDomain(
                        productDO,
                        detailMap.get(productDO.getId()),
                        imagesByProduct.getOrDefault(productDO.getId(), List.of())))
                .toList();
    }

    private void upsertDetail(Product product) {
        var detailDO = dataMapper.toDetailDO(product.getId(), product.getDescription());
        if (detailDO == null) return;
        var existingDetail = productDetailMapper.selectById(product.getId().value());
        if (existingDetail != null) {
            existingDetail.setDescription(detailDO.getDescription());
            productDetailMapper.updateById(existingDetail);
        } else {
            productDetailMapper.insert(detailDO);
        }
    }

    private void syncImages(Product product) {
        var productId = product.getId().value();
        var existingImages = mapper.selectImagesByProductIds(List.of(productId));
        var newImages = dataMapper.toImageDOs(product.getId(), product.getImages());
        var existingUrls =
                existingImages.stream().map(ProductImageDO::getImageUrl).collect(Collectors.toSet());
        var newUrls = newImages.stream().map(ProductImageDO::getImageUrl).collect(Collectors.toSet());
        var urlsToDelete = new HashSet<>(existingUrls);
        urlsToDelete.removeAll(newUrls);
        if (!urlsToDelete.isEmpty()) {
            productImageMapper.deleteByProductIdAndUrls(productId, new ArrayList<>(urlsToDelete));
        }
        var urlsToAdd = new HashSet<>(newUrls);
        urlsToAdd.removeAll(existingUrls);
        if (!urlsToAdd.isEmpty()) {
            var imagesToAdd = newImages.stream()
                    .filter(img -> urlsToAdd.contains(img.getImageUrl()))
                    .toList();
            productImageMapper.batchInsert(imagesToAdd);
        }
    }
}
