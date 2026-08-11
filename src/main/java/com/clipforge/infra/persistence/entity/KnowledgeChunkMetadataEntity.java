package com.clipforge.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeChunkMetadataEntity（知识切片元数据实体）。
 *
 * <p>该实体映射 cf_knowledge_chunk_metadata（知识切片元数据表），用于保存切片级可查询键值元数据。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_knowledge_chunk_metadata")
public class KnowledgeChunkMetadataEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** metadataId（元数据编号）是对外使用的唯一编号。 */
    private String metadataId;

    /** knowledgeBaseId（知识库编号）表示元数据归属的知识库。 */
    private String knowledgeBaseId;

    /** documentId（知识文档编号）表示元数据归属的文档。 */
    private String documentId;

    /** chunkId（知识切片编号）表示元数据归属的切片。 */
    private String chunkId;

    /** metadataKey（元数据键）表示可查询字段名。 */
    private String metadataKey;

    /** metadataValue（元数据值）表示字段值。 */
    private String metadataValue;

    /** valueType（值类型）表示 STRING（字符串）、NUMBER（数字）、BOOLEAN（布尔）或 JSON（对象表示格式）。 */
    private String valueType;

    /** sourceType（来源类型）表示 SYSTEM（系统）、USER（用户）、PARSER（解析器）或 LLM（大语言模型）。 */
    private String sourceType;

    /** confidence（置信度）用于模型或解析器抽取场景，当前切片阶段为空。 */
    private BigDecimal confidence;

    /** enabled（是否启用）1 表示启用，0 表示禁用。 */
    private Integer enabled;

    /** createTime（创建时间）表示记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
