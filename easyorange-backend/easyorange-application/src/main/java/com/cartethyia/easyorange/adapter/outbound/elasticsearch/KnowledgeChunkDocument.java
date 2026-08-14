package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * 知识库分块文档（RAG 索引侧）— 与 {@code knowledge-mapping.json} 的 dense_vector(1024)
 * 对齐；embedding 为空（摄入时 embed 失败）的块照常写入，检索时按纯文本兜底。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "knowledge_docs")
public class KnowledgeChunkDocument {

    /** 文档 ID + 块序号（docId:chunkIndex），重投幂等覆盖。 */
    @Id
    private String id;

    private String docId;
    private Integer chunkIndex;
    private String title;
    private String content;
    private String source;

    @Field(type = FieldType.Dense_Vector)
    private List<Float> embedding;
}
