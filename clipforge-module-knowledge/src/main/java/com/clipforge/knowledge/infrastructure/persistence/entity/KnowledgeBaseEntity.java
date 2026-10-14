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
 * KnowledgeBaseEntity（知识库实体）。
 *
 * <p>该实体映射 cf_knowledge_base（知识库表），用于保存 RAG（检索增强生成）知识库容器和默认处理配置。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "cf_knowledge_base", autoResultMap = true)
public class KnowledgeBaseEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** knowledgeBaseId（知识库编号）是对外使用的唯一编号。 */
    private String knowledgeBaseId;

    /** userId（用户编号）表示知识库所属用户，系统级知识库可以为空。 */
    private String userId;

    /** name（知识库名称）表示知识库的人类可读名称。 */
    private String name;

    /** description（知识库描述）用于说明知识库内容和适用场景。 */
    private String description;

    /** category（知识库分类）用于区分平台规则、垂直领域资料和案例库。 */
    private String category;

    /** defaultPlatform（默认平台）用于记录默认适配平台。 */
    private String defaultPlatform;

    /** embeddingProvider（嵌入供应商）用于记录默认 Embedding（嵌入）模型供应商。 */
    private String embeddingProvider;

    /** embeddingModel（嵌入模型）用于记录默认 Embedding（嵌入）模型名称。 */
    private String embeddingModel;

    /** embeddingDimension（嵌入维度）用于记录向量维度。 */
    private Integer embeddingDimension;

    /** chunkStrategy（切片策略）用于记录默认文档切片方式。 */
    private String chunkStrategy;

    /** chunkConfigJson（切片配置 JSON）用于保存 JSONB（JSON 二进制类型）格式的切片配置。 */
    @TableField(value = "chunk_config_json", typeHandler = JsonbStringTypeHandler.class)
    private String chunkConfigJson;

    /** retrievalConfigJson（检索配置 JSON）用于保存 JSONB（JSON 二进制类型）格式的检索配置。 */
    @TableField(value = "retrieval_config_json", typeHandler = JsonbStringTypeHandler.class)
    private String retrievalConfigJson;

    /** status（状态）表示 ACTIVE（启用中）或 ARCHIVED（已归档）。 */
    private String status;

    /** enabled（是否启用）1 表示启用，0 表示禁用。 */
    private Integer enabled;

    /** createTime（创建时间）表示记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
