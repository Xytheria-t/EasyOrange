package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * AI 详情面真实 MySQL 链路测试 —— 单测里读模型是 mock 出来的，兜不住「字段到底有没有从库里读出来」：
 * 修复前 {@code conditionDesc} / {@code location} 在这条路径上恒为 null，成色与地区两个比对维度静默缺席
 * （不报错、不影响对话，只是模型少一个决策依据，日志里也看不出来）。
 * <p>
 * 期望值从库里反推（读同一行 DO / 副表 / 用户表），不写死种子值，也不断言「非空」了事 —— 种子数据
 * 改了不会假红，映射断了必然真红。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("it")
class AssetDetailReadPathIT {

    @Autowired
    private AssetDetailPort assetDetailPort;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductDetailMapper productDetailMapper;

    @Test
    @DisplayName("详情面带回成色描述、脱敏地区、描述与卖家名（库里有的字段不再静默为空）")
    void findDetail_carriesDisplayFieldsFromDatabase() {
        var row = ChainWrappers.lambdaQueryChain(productMapper)
                .eq(ProductDO::getStatus, ProductStatus.ONLINE.getCode())
                .isNotNull(ProductDO::getConditionLevel)
                .isNotNull(ProductDO::getLocation)
                .isNotNull(ProductDO::getUserId)
                .orderByAsc(ProductDO::getCreateTime)
                .last("LIMIT 1")
                .one();
        assertThat(row).as("dev 库应有带成色 / 地区的在售种子商品").isNotNull();

        AssetDetail detail = assetDetailPort.findDetail(row.getId()).orElseThrow();

        assertThat(detail.title()).isEqualTo(row.getName());
        assertThat(detail.conditionDesc()).isEqualTo(row.getConditionLevel().getDesc());
        assertThat(detail.location()).as("地区按商品详情页同口径脱敏后交给模型").isEqualTo(MaskUtils.maskAddress(row.getLocation(), 6));
        assertThat(detail.description()).as("描述来自 eo_product_detail 副表").isEqualTo(databaseDescription(row.getId()));
        assertThat(detail.sellerName()).isEqualTo(databaseSellerName(row.getUserId()));
        assertThat(detail.status()).isEqualTo(row.getStatus().getCode());
    }

    @Test
    @DisplayName("查无此资产返回 empty：调用方据此合成模型可读的失败观察")
    void findDetail_unknownId() {
        assertThat(assetDetailPort.findDetail("it-missing-" + UUID.randomUUID()))
                .isEmpty();
    }

    /** 副表没写描述时两边同为 null，也是正确的收敛结果。 */
    private String databaseDescription(String productId) {
        return productDetailMapper.selectDetailsByProductIds(List.of(productId)).stream()
                .findFirst()
                .map(detail -> detail.getDescription())
                .orElse(null);
    }

    /** 与商品 VO 同口径：昵称优先，退用户名。 */
    private String databaseSellerName(String sellerId) {
        return productMapper.selectSellersByIds(Set.of(sellerId)).stream()
                .findFirst()
                .map(seller -> seller.nickName() != null ? seller.nickName() : seller.username())
                .orElse(null);
    }
}
