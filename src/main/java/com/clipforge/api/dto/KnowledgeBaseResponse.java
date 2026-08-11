package com.clipforge.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeBaseResponse（知识库响应）。
 *
 * <p>该对象用于接口返回 KnowledgeBase（知识库）的基础信息和默认处理配置。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseResponse {

    /** knowledgeBaseId（知识库编号）是对外使用的知识库唯一编号。 */
    private String knowledgeBaseId;

    /** userId（用户编号）表示知识库所属用户。 */
    private String userId;

    /** name（知识库名称）表示知识库的人类可读名称。 */
    private String name;

    /** description（知识库描述）用于说明知识库用途。 */
    private String description;

    /** category（知识库分类）用于区分平台规则、垂直领域资料、案例库等。 */
    private String category;

    /** defaultPlatform（默认平台）表示该知识库默认面向的内容平台。 */
    private String defaultPlatform;

    /** embeddingProvider（嵌入供应商）表示后续向量化默认使用的供应商。 */
    private String embeddingProvider;

    /** embeddingModel（嵌入模型）表示后续向量化默认使用的模型名称。 */
    private String embeddingModel;

    /** embeddingDimension（嵌入维度）表示向量维度。 */
    private Integer embeddingDimension;

    /** chunkStrategy（切片策略）表示默认文档切片策略。 */
    private String chunkStrategy;

    /** chunkConfigJson（切片配置 JSON）表示默认切片配置。 */
    private String chunkConfigJson;

    /** retrievalConfigJson（检索配置 JSON）表示默认检索配置。 */
    private String retrievalConfigJson;

    /** status（状态）表示 ACTIVE（启用中）或 ARCHIVED（已归档）。 */
    private String status;

    /** enabled（是否启用）1 表示启用，0 表示禁用。 */
    private Integer enabled;

    /** createTime（创建时间）表示知识库创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示知识库最近更新时间。 */
    private LocalDateTime updateTime;
}
