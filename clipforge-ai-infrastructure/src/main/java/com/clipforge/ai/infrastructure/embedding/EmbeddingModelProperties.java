package com.clipforge.ai.infrastructure.embedding;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * EmbeddingModelProperties（嵌入模型配置属性）。
 *
 * <p>该配置类绑定 `clipforge.embedding`（项目嵌入模型配置），专门用于知识切片向量化，不和 Rerank（重排序）模型混用。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.embedding")
public class EmbeddingModelProperties {

    /** mockEnabled（是否启用模拟嵌入模型）为 true 时使用 MockEmbeddingModelClient（模拟嵌入模型客户端）。 */
    private boolean mockEnabled = true;

    /** provider（供应商）表示真实嵌入模型供应商类型，当前优先支持 openai-compatible（OpenAI 兼容接口）。 */
    private String provider = "openai-compatible";

    /** baseUrl（基础地址）表示真实嵌入模型 OpenAI-Compatible API（OpenAI 兼容接口）地址。 */
    private String baseUrl;

    /** apiKey（接口密钥）表示嵌入模型平台 API Key（接口密钥），应通过环境变量或本地配置注入。 */
    private String apiKey;

    /** modelName（模型名称）表示真实调用的 Embedding（嵌入）模型 ID（模型编号）。 */
    private String modelName;

    /** dimension（向量维度）必须和后续 pgvector（PostgreSQL 向量扩展）表字段维度保持一致。 */
    private Integer dimension = 1024;

    /** maxSegmentsPerBatch（单批最大文本段数）用于控制一次请求最多向量化多少个切片。 */
    private Integer maxSegmentsPerBatch = 16;

    /** logRequests（是否记录请求）用于本地调试模型请求，生产环境建议关闭。 */
    private boolean logRequests = false;

    /** logResponses（是否记录响应）用于本地调试模型响应，生产环境建议关闭。 */
    private boolean logResponses = false;
}
