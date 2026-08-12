package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.exception.ConcurrentUpdateException;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.ProductTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductRepositoryImpl 测试")
class ProductRepositoryImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductDetailMapper productDetailMapper;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private IdGenerator idGenerator;

    private ProductRepositoryImpl productRepository;

    @BeforeEach
    void setUp() {
        productRepository = new ProductRepositoryImpl(
                productMapper, productDetailMapper, productImageMapper, new ProductDataMapper(), idGenerator);
    }

    @Test
    @DisplayName("insert 为商品与图片生成 id 并级联写入")
    void insert_newProduct_generatesIdsAndCascades() {
        when(idGenerator.generateId()).thenReturn("prod-id", "img-id-1");

        var created = Product.create(ProductTestFixture.defaultCreateSpec()).aggregate();

        var saved = productRepository.save(created);

        assertThat(saved.getId().value()).isEqualTo("prod-id");

        verify(productMapper)
                .insert(argThat((ProductDO entity) -> entity.getId().equals("prod-id")));

        verify(productDetailMapper)
                .insert(argThat((ProductDetailDO d) -> d.getProductId().equals("prod-id")));

        verify(productImageMapper)
                .batchInsert(argThat(images -> images.size() == 1
                        && images.getFirst().getId().equals("img-id-1")
                        && images.getFirst().getProductId().equals("prod-id")));
    }

    @Test
    @DisplayName("update 整组替换图片：先物理删全量再重插")
    void update_existingProduct_replacesImages() {
        when(productMapper.updateById(any(ProductDO.class))).thenReturn(1);
        when(productDetailMapper.selectById("1")).thenReturn(null);
        when(idGenerator.generateId()).thenReturn("img-id-2");

        productRepository.save(ProductTestFixture.defaultProduct());

        verify(productMapper).updateById(any(ProductDO.class));
        verify(productImageMapper).deleteByProductId("1");
        verify(productImageMapper)
                .batchInsert(argThat(images ->
                        images.size() == 1 && images.getFirst().getId().equals("img-id-2")));
    }

    @Test
    @DisplayName("update 乐观锁冲突时抛 ConcurrentUpdateException")
    void update_conflict_throws() {
        when(productMapper.updateById(any(ProductDO.class))).thenReturn(0);

        var product = ProductTestFixture.defaultProduct();
        assertThatThrownBy(() -> productRepository.save(product)).isInstanceOf(ConcurrentUpdateException.class);
    }
}
