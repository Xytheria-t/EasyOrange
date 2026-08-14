package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.exception.ConcurrentUpdateException;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ViewCountEntry;
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
    private final IdGenerator idGenerator;

    public ProductRepositoryImpl(
            ProductMapper productMapper,
            ProductDetailMapper productDetailMapper,
            ProductImageMapper productImageMapper,
            ProductDataMapper dataMapper,
            IdGenerator idGenerator) {
        super(productMapper);
        this.productDetailMapper = productDetailMapper;
        this.productImageMapper = productImageMapper;
        this.dataMapper = dataMapper;
        this.idGenerator = idGenerator;
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
        return findByIds(List.of(id)).stream().findFirst();
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

    @Override
    public void batchAddViewCounts(List<ViewCountEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        mapper.batchAddViewCounts(entries);
    }

    private Product insert(Product product) {
        var productDO = dataMapper.toDataObject(product);
        productDO.setId(idGenerator.generateId());
        mapper.insert(productDO);
        var saved = product.assignId(productDO.getId());
        var detailDO = dataMapper.toDetailDO(saved.getId(), saved.getDescription());
        if (detailDO != null) {
            productDetailMapper.insert(detailDO);
        }
        insertImages(saved.getId(), saved.getImages());
        return saved;
    }

    private Product update(Product product) {
        if (mapper.updateById(dataMapper.toDataObject(product)) == 0) {
            throw new ConcurrentUpdateException("商品更新冲突: id=" + product.getId().value());
        }
        upsertDetail(product);
        replaceImages(product);
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

    private void insertImages(ProductId productId, ImageSet imageSet) {
        var imageDOs = dataMapper.toImageDOs(productId, imageSet);
        if (imageDOs.isEmpty()) return;
        imageDOs.forEach(img -> img.setId(idGenerator.generateId()));
        productImageMapper.batchInsert(imageDOs);
    }

    // 图片整组替换：物理删全量再重插。行 id 无外部引用，isMain/sortOrder 完全由列表顺序推导，
    // 全量重写同时修正了旧实现"仅按 URL 求差、顺序变更不生效"的缺陷。
    private void replaceImages(Product product) {
        productImageMapper.deleteByProductId(product.getId().value());
        insertImages(product.getId(), product.getImages());
    }
}
