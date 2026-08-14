package com.cartethyia.easyorange.ai.adapter.outbound.persistence.knowledge;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.ai.enums.KnowledgeDocStatus;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eo_knowledge_doc")
public class KnowledgeDocDO extends BaseDO {

    private String title;
    private String content;
    private String source;
    /** 枚举经 {@code @EnumValue} 注解落 VARCHAR(code)。 */
    private KnowledgeDocStatus status;

    private Integer chunkCount;
}
