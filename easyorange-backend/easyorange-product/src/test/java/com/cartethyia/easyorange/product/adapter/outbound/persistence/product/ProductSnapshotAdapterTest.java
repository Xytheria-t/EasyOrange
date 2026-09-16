package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.product.domain.aggregate.ProductTestFixture;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ImageUrl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSnapshotAdapter 测试")
class ProductSnapshotAdapterTest {

    @Mock
    private ProductRepository productRepository;

    private ProductSnapshotAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProductSnapshotAdapter(productRepository);
    }

    @Test
    @DisplayName("findSnapshots 一次查库，实时状态与展示信息一并带出")
    void findSnapshots_returnsLiveStateAndDisplayFields() {
        var productId = ProductId.of("1");
        when(productRepository.findByIds(List.of(productId))).thenReturn(List.of(ProductTestFixture.onlineProduct()));

        var snapshots = adapter.findSnapshots(List.of(productId));

        assertThat(snapshots).hasSize(1);
        var snapshot = snapshots.getFirst();
        assertThat(snapshot.productId().value()).isEqualTo("1");
        assertThat(snapshot.sellerId().value()).isEqualTo("1");
        assertThat(snapshot.price().value()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(snapshot.status()).isEqualTo(ProductStatus.ONLINE);
        assertThat(snapshot.stock().value()).isEqualTo(10);
        assertThat(snapshot.title()).isEqualTo("测试商品");
        assertThat(snapshot.image()).isEqualTo("http://img/1.jpg");
        assertThat(snapshot.description()).isEqualTo("描述");
        assertThat(snapshot.conditionLevel()).isEqualTo("全新");
        // 一次 findByIds 拿到全部快照，逐 id 循环查库会让下单链路退化成 N 次读
        verify(productRepository).findByIds(anyList());
    }

    @Test
    @DisplayName("主图不在首位时取主图，无图时 image 为 null 交由调用方回退")
    void findSnapshots_imageResolution_prefersMain() {
        var productId = ProductId.of("1");
        var product = ProductTestFixture.defaultProduct().toBuilder()
                .images(ImageSet.ofImages(List.of(
                        new ImageSet.ProductImage(new ImageUrl("http://img/second.jpg"), 1, false),
                        new ImageSet.ProductImage(new ImageUrl("http://img/main.jpg"), 2, true))))
                .build();
        when(productRepository.findByIds(List.of(productId))).thenReturn(List.of(product));

        assertThat(adapter.findSnapshots(List.of(productId)).getFirst().image()).isEqualTo("http://img/main.jpg");

        var noImage = ProductTestFixture.defaultProduct().toBuilder()
                .images(ImageSet.empty())
                .build();
        when(productRepository.findByIds(List.of(productId))).thenReturn(List.of(noImage));

        assertThat(adapter.findSnapshots(List.of(productId)).getFirst().image()).isNull();
    }

    @Test
    @DisplayName("空 id 列表直接返回空，不打库")
    void findSnapshots_emptyIds_skipsQuery() {
        assertThat(adapter.findSnapshots(List.of())).isEmpty();

        verify(productRepository, never()).findByIds(anyList());
    }
}
