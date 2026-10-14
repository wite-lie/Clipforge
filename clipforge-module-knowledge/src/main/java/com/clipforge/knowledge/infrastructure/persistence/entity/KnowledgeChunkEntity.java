package com.clipforge.knowledge.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clipforge.knowledge.infrastructure.persistence.typehandler.JsonbStringTypeHandler;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeChunkEntity（知识切片实体）。
 *
 * <p>该实体映射 cf_knowledge_chunk（知识切片表），用于保存文档切分后的可检索文本单元。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "cf_knowledge_chunk", autoResultMap = true)
public class KnowledgeChunkEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** chunkId（知识切片编号）是对外使用的唯一编号。 */
    private String chunkId;

    /** knowledgeBaseId（知识库编号）表示切片归属的知识库。 */
    private String knowledgeBaseId;

    /** documentId（知识文档编号）表示切片归属的文档。 */
    private String documentId;

    /** chunkIndex（切片序号）用于保持原文顺序。 */
    private Integer chunkIndex;

    /** titlePath（标题路径）表示切片所在的 Markdown（标记文档）标题层级。 */
    private String titlePath;

    /** content（切片正文）表示可检索的文本内容。 */
    private String content;

    /** contentHash（内容哈希）用于后续变更检测和人工精修判断。 */
    private String contentHash;

    /** charCount（字符数）表示切片正文字符数。 */
    private Integer charCount;

    /** tokenCount（词元数）表示估算 Token（词元）数量。 */
    private Integer tokenCount;

    /** metadataJson（元数据 JSON）用于保存 JSONB（JSON 二进制类型）格式的切片扩展属性。 */
    @TableField(value = "metadata_json", typeHandler = JsonbStringTypeHandler.class)
    private String metadataJson;

    /** enabled（是否启用）1 表示启用，0 表示禁用。 */
    private Integer enabled;

    /** createTime（创建时间）表示记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
