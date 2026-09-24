package com.cartethyia.easyorange.adapter.outbound.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository.ProductDetailInfo;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AI 详情工具面适配器测试 —— 读模型只带 eo_product 单表字段，适配器负责单件补齐描述 / 卖家名，
 * 并按买家可见口径脱敏地区；这三件都在观察文本里会进供应商请求，回归时不能悄悄退回去。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("资产详情适配器（AI 详情工具面） -> 测试")
class AssetDetailAdapterTest {

    @Mock
    private ProductQueryRepository productQueryRepository;

    private AssetDetailPort adapter() {
        return new AssetDetailAdapter(productQueryRepository);
    }

    @Test
    @DisplayName("描述与卖家名单件补齐，地区按商品详情页同口径脱敏")
    void findDetail_fillsDescriptionAndSeller_masksLocation() {
        when(productQueryRepository.findProductById("p1")).thenReturn(readModel());
        when(productQueryRepository.findDetailsByProductIds(List.of("p1")))
                .thenReturn(List.of(new ProductDetailInfo("p1", "个人自用，无磕碰")));
        when(productQueryRepository.findSellersByIds(Set.of("s1")))
                .thenReturn(List.of(new SellerReadModel("s1", "liming", "李明", null)));

        var detail = adapter().findDetail("p1").orElseThrow();

        assertThat(detail.description()).isEqualTo("个人自用，无磕碰");
        assertThat(detail.sellerName()).isEqualTo("李明");
        assertThat(detail.conditionDesc()).isEqualTo("几乎全新");
        assertThat(detail.location()).isEqualTo("浙江省杭州市***");
        assertThat(detail.status()).isEqualTo("ONLINE");
    }

    @Test
    @DisplayName("昵称缺省时退用户名；副表无描述、卖家查不到时留 null 由观察文本回退")
    void findDetail_fallsBackToUsername_andLeavesMissingFieldsNull() {
        when(productQueryRepository.findProductById("p2"))
                .thenReturn(readModel().toBuilder().id("p2").build());
        when(productQueryRepository.findDetailsByProductIds(List.of("p2"))).thenReturn(List.of());
        when(productQueryRepository.findSellersByIds(Set.of("s1")))
                .thenReturn(List.of(new SellerReadModel("s1", "liming", null, null)));

        var detail = adapter().findDetail("p2").orElseThrow();

        assertThat(detail.description()).isNull();
        assertThat(detail.sellerName()).isEqualTo("liming");
    }

    @Test
    @DisplayName("无地区 / 无卖家 ID 时留 null，也不去打用户表")
    void findDetail_absentLocationAndSellerId() {
        when(productQueryRepository.findProductById("p3"))
                .thenReturn(readModel().toBuilder()
                        .id("p3")
                        .location(null)
                        .sellerId(null)
                        .build());
        when(productQueryRepository.findDetailsByProductIds(List.of("p3"))).thenReturn(List.of());

        var detail = adapter().findDetail("p3").orElseThrow();

        assertThat(detail.location()).isNull();
        assertThat(detail.sellerName()).isNull();
        verify(productQueryRepository, never()).findSellersByIds(any());
    }

    @Test
    @DisplayName("查无此资产返回 empty，由调用方转成模型可读的失败观察")
    void findDetail_notFound() {
        when(productQueryRepository.findProductById("missing")).thenReturn(null);

        assertThat(adapter().findDetail("missing")).isEmpty();
    }

    @Test
    @DisplayName("批量查详情：N 件候选固定 3 次查询（商品/描述/卖家各一次），不退化成逐件 N+1")
    void findDetails_batchesIntoThreeQueries() {
        when(productQueryRepository.findProductsByIds(List.of("p1", "p2")))
                .thenReturn(List.of(
                        readModel(),
                        readModel().toBuilder().id("p2").sellerId("s2").build()));
        when(productQueryRepository.findDetailsByProductIds(List.of("p1", "p2")))
                .thenReturn(List.of(new ProductDetailInfo("p1", "个人自用，无磕碰"), new ProductDetailInfo("p2", "几乎全新")));
        when(productQueryRepository.findSellersByIds(Set.of("s1", "s2")))
                .thenReturn(List.of(
                        new SellerReadModel("s1", "liming", "李明", null),
                        new SellerReadModel("s2", "zhangsan", null, null)));

        var details = adapter().findDetails(List.of("p1", "p2"));

        assertThat(details).hasSize(2);
        assertThat(details)
                .extracting(AssetDetail::productId, AssetDetail::description, AssetDetail::sellerName)
                .containsExactly(tuple("p1", "个人自用，无磕碰", "李明"), tuple("p2", "几乎全新", "zhangsan"));
        // 关键回归点：每类查询各一次，与候选数无关
        verify(productQueryRepository).findProductsByIds(List.of("p1", "p2"));
        verify(productQueryRepository).findDetailsByProductIds(List.of("p1", "p2"));
        verify(productQueryRepository).findSellersByIds(Set.of("s1", "s2"));
    }

    @Test
    @DisplayName("批量查详情：空入参 / 全部查无 不打任何下游查询")
    void findDetails_emptyOrMissing() {
        assertThat(adapter().findDetails(List.of())).isEmpty();
        when(productQueryRepository.findProductsByIds(List.of("missing"))).thenReturn(List.of());
        assertThat(adapter().findDetails(List.of("missing"))).isEmpty();
        verify(productQueryRepository, never()).findSellersByIds(any());
    }

    private static ProductReadModel readModel() {
        return ProductReadModel.builder()
                .id("p1")
                .sellerId("s1")
                .title("九成新显卡")
                .price(new BigDecimal("3400.00"))
                .status("ONLINE")
                .condition("2")
                .conditionDesc("几乎全新")
                .location("浙江省杭州市西湖区文三路 100 号")
                .build();
    }
}
