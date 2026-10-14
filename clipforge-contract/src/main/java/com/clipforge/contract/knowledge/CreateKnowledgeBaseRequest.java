package com.clipforge.contract.knowledge;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreateKnowledgeBaseRequest（创建知识库请求）。
 *
 * <p>该对象承载创建 KnowledgeBase（知识库）时需要的基础配置。Phase 7.2（阶段 7.2）只保存配置，不调用 Embedding（嵌入）模型。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateKnowledgeBaseRequest {

    /** userId（用户编号）表示知识库所属用户；平台规则等系统知识库可以为空。 */
    private String userId;

    /** name（知识库名称）表示知识库的人类可读名称。 */
    @NotBlank(message = "知识库名称不能为空")
    private String name;

    /** description（知识库描述）用于说明知识库收录内容和适用场景。 */
    private String description;

    /** category（知识库分类）用于区分 PLATFORM_RULE（平台规则）、ANIMAL_SCIENCE（动物科普）、CASE_STUDY（案例库）等。 */
    private String category;

    /** defaultPlatform（默认平台）用于标记知识库默认适配的小红书、抖音、B 站或 TikTok 等平台。 */
    private String defaultPlatform;

    /** embeddingProvider（嵌入供应商）用于记录后续向量化默认使用的模型平台。 */
    private String embeddingProvider;

    /** embeddingModel（嵌入模型）用于记录后续向量化默认使用的模型名称。 */
    private String embeddingModel;

    /** embeddingDimension（嵌入维度）用于记录向量维度，必须和 pgvector（PostgreSQL 向量扩展）表结构匹配。 */
    @Min(value = 1, message = "向量维度必须大于 0")
    private Integer embeddingDimension;

    /** chunkStrategy（切片策略）用于记录后续文档处理默认采用的切片方式。 */
    private String chunkStrategy;

    /** chunkConfigJson（切片配置 JSON）用于保存切片大小、重叠长度等结构化配置。 */
    private String chunkConfigJson;

    /** retrievalConfigJson（检索配置 JSON）用于保存 topK、混合检索权重、重排序开关等结构化配置。 */
    private String retrievalConfigJson;
}
