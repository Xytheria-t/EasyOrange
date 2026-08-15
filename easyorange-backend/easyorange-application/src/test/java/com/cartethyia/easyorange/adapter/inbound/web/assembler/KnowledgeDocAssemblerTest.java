package com.cartethyia.easyorange.adapter.inbound.web.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.enums.KnowledgeDocStatus;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeDocEntity;
import com.cartethyia.easyorange.common.result.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KnowledgeDocAssembler 测试")
class KnowledgeDocAssemblerTest {

    @Test
    @DisplayName("实体 → VO：剔除正文大字段，保留列表所需字段")
    void toVO_dropsContent() {
        var entity = new KnowledgeDocEntity(
                "id-1", "上架规则", "长正文……", "运营", KnowledgeDocStatus.INDEXED, 3, LocalDateTime.of(2026, 8, 14, 10, 0));

        var vo = KnowledgeDocAssembler.toVO(entity);

        assertThat(vo.id()).isEqualTo("id-1");
        assertThat(vo.title()).isEqualTo("上架规则");
        assertThat(vo.source()).isEqualTo("运营");
        assertThat(vo.status()).isEqualTo(KnowledgeDocStatus.INDEXED);
        assertThat(vo.chunkCount()).isEqualTo(3);
        assertThat(vo.createTime()).isEqualTo(entity.createTime());
    }

    @Test
    @DisplayName("分页整体映射：records 转换、分页元信息原样透传")
    void toVOPage_mapsRecordsAndKeepsPagination() {
        var entity =
                new KnowledgeDocEntity("id-1", "上架规则", "正文", "运营", KnowledgeDocStatus.PENDING, 0, LocalDateTime.now());
        var page = new PageResult<>(List.of(entity), 1L, 2, 10, 1);

        var voPage = KnowledgeDocAssembler.toVOPage(page);

        assertThat(voPage.records()).hasSize(1);
        assertThat(voPage.records().get(0).id()).isEqualTo("id-1");
        assertThat(voPage.total()).isEqualTo(1L);
        assertThat(voPage.current()).isEqualTo(2);
        assertThat(voPage.size()).isEqualTo(10);
        assertThat(voPage.pages()).isEqualTo(1);
    }
}
