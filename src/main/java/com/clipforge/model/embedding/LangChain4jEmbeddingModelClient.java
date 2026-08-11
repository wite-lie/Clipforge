package com.clipforge.model.embedding;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.model.governance.ModelCallLimiter;
import com.clipforge.model.governance.ModelCallType;
import com.clipforge.model.governance.NoOpModelCallLimiter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * LangChain4jEmbeddingModelClient（LangChain4j 嵌入模型客户端）。
 *
 * <p>该实现通过 LangChain4j（LangChain 的 Java 封装）的 OpenAiEmbeddingModel（OpenAI 嵌入模型）调用 OpenAI-Compatible API（OpenAI 兼容接口）。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.embedding", name = "mock-enabled", havingValue = "false")
public class LangChain4jEmbeddingModelClient implements EmbeddingModelClient {

    /** properties（配置属性）保存嵌入模型供应商、地址、密钥、模型名称和维度。 */
    private final EmbeddingModelProperties properties;

    /** embeddingModel（嵌入模型）是真正执行 LangChain4j（LangChain 的 Java 封装）调用的模型对象。 */
    private final EmbeddingModel embeddingModel;

    /** modelCallLimiter（模型调用限流器）用于控制真实 Embedding（嵌入）调用并发。 */
    private final ModelCallLimiter modelCallLimiter;

    /**
     * 构造函数用于根据配置创建 LangChain4j（LangChain 的 Java 封装）嵌入模型。
     *
     * @param properties 嵌入模型配置属性
     */
    public LangChain4jEmbeddingModelClient(EmbeddingModelProperties properties) {
        this(properties, new NoOpModelCallLimiter());
    }

    /**
     * 构造函数用于注入模型调用限流器。
     *
     * @param properties 嵌入模型配置
     * @param modelCallLimiter 模型调用限流器
     */
    @Autowired
    public LangChain4jEmbeddingModelClient(EmbeddingModelProperties properties, ModelCallLimiter modelCallLimiter) {
        // 保存配置属性。
        this.properties = properties;
        // 保存模型调用限流器，真实向量化调用前会先获取许可。
        this.modelCallLimiter = modelCallLimiter;
        // 校验真实嵌入模型配置。
        validateProperties(properties);
        // 创建 OpenAI-Compatible API（OpenAI 兼容接口）嵌入模型。
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                // 设置模型接口基础地址。
                .baseUrl(properties.getBaseUrl())
                // 设置接口密钥。
                .apiKey(properties.getApiKey())
                // 设置模型名称。
                .modelName(properties.getModelName())
                // 设置向量维度，部分模型支持在请求侧指定输出维度。
                .dimensions(properties.getDimension())
                // 设置单批最大文本段数。
                .maxSegmentsPerBatch(properties.getMaxSegmentsPerBatch())
                // 设置是否记录请求。
                .logRequests(properties.isLogRequests())
                // 设置是否记录响应。
                .logResponses(properties.isLogResponses())
                // 构建嵌入模型。
                .build();
    }

    /**
     * embed（生成嵌入向量）用于发起一次真实 Embedding（嵌入）模型调用。
     *
     * @param request 嵌入模型请求
     * @return 嵌入模型响应
     */
    @Override
    public EmbeddingModelResponse embed(EmbeddingModelRequest request) {
        try {
            // 校验请求文本。
            validateRequest(request);
            // 把项目文本转换为 LangChain4j TextSegment（文本片段）。
            List<TextSegment> segments = request.getTexts().stream()
                    // 创建文本片段。
                    .map(TextSegment::from)
                    // 收集为列表。
                    .toList();
            // 调用真实嵌入模型。
            Response<List<dev.langchain4j.data.embedding.Embedding>> response = modelCallLimiter.execute(
                    ModelCallType.EMBEDDING,
                    firstNotBlank(request.getRunId(), request.getProjectId(), request.getConversationId(), request.getPurpose()),
                    () -> embeddingModel.embedAll(segments));
            // 提取模型返回的向量。
            List<List<Float>> vectors = response.content().stream()
                    // 转换为普通 Float（浮点数）列表，便于后续 JSON（对象表示格式）打印和入库适配。
                    .map(dev.langchain4j.data.embedding.Embedding::vectorAsList)
                    // 收集为列表。
                    .toList();
            // 读取 Token（词元）用量。
            TokenUsage tokenUsage = response.tokenUsage();
            // 读取向量维度，优先使用返回向量的真实维度。
            int dimension = vectors.isEmpty() ? embeddingModel.dimension() : vectors.get(0).size();
            // 构造项目响应。
            return EmbeddingModelResponse.builder()
                    // 设置供应商。
                    .provider(properties.getProvider())
                    // 设置模型名称。
                    .modelName(defaultText(embeddingModel.modelName(), properties.getModelName()))
                    // 设置向量维度。
                    .dimension(dimension)
                    // 设置向量列表。
                    .embeddings(vectors)
                    // 设置输入 Token（词元），供应商不返回时使用估算值。
                    .promptTokens(tokenUsage == null ? estimateTokens(request.getTexts()) : tokenUsage.inputTokenCount())
                    // 设置总 Token（词元），供应商不返回时使用估算值。
                    .totalTokens(tokenUsage == null ? estimateTokens(request.getTexts()) : tokenUsage.totalTokenCount())
                    // 构建响应。
                    .build();
        } catch (BusinessException exception) {
            // 业务异常直接抛出。
            throw exception;
        } catch (Exception exception) {
            // 其他异常包装为模型调用错误。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "LangChain4j 调用真实嵌入模型失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * validateProperties（校验配置）用于检查真实嵌入模型必要配置。
     *
     * @param properties 嵌入模型配置属性
     */
    private void validateProperties(EmbeddingModelProperties properties) {
        // 校验基础地址。
        if (isBlank(properties.getBaseUrl())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.embedding.base-url 不能为空");
        }
        // 校验接口密钥。
        if (isBlank(properties.getApiKey())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.embedding.api-key 不能为空");
        }
        // 校验模型名称。
        if (isBlank(properties.getModelName())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.embedding.model-name 不能为空");
        }
        // 校验向量维度。
        if (properties.getDimension() == null || properties.getDimension() <= 0) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.embedding.dimension 必须大于 0");
        }
    }

    /**
     * validateRequest（校验请求）用于保证文本列表存在。
     *
     * @param request 嵌入模型请求
     */
    private void validateRequest(EmbeddingModelRequest request) {
        // 请求不能为空。
        if (request == null || request.getTexts() == null || request.getTexts().isEmpty()) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "嵌入模型请求 texts（文本列表）不能为空");
        }
        // 文本列表中不能包含空文本。
        if (request.getTexts().stream().anyMatch(text -> text == null || text.isBlank())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "嵌入模型请求 texts（文本列表）不能包含空文本");
        }
    }

    /**
     * estimateTokens（估算词元）用于供应商未返回 Token（词元）用量时给出成本参考。
     *
     * @param texts 文本列表
     * @return 估算词元数量
     */
    private int estimateTokens(List<String> texts) {
        // 中文场景先按两个字符约等于一个 Token（词元）粗略估算。
        return texts.stream()
                // 估算单段文本词元。
                .mapToInt(text -> Math.max(1, (int) Math.ceil(text.length() / 2.0)))
                // 汇总。
                .sum();
    }

    /**
     * defaultText（默认文本）用于处理空字符串。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 最终文本
     */
    private String defaultText(String value, String defaultValue) {
        // 如果原始值为空，返回默认值。
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * firstNotBlank（第一个非空文本）用于选择模型限流日志里的业务编号。
     *
     * @param values 候选文本
     * @return 第一个非空文本
     */
    private String firstNotBlank(String... values) {
        // 遍历所有候选值。
        for (String value : values) {
            // 命中非空文本时直接返回。
            if (!isBlank(value)) {
                return value;
            }
        }
        // 全部为空时返回 unknown（未知），避免日志字段为空。
        return "unknown";
    }

    /**
     * isBlank（是否空白）用于判断字符串是否为空引用或空白。
     *
     * @param value 原始值
     * @return 是否空白
     */
    private boolean isBlank(String value) {
        // 判断字符串是否为空引用或空白字符串。
        return value == null || value.isBlank();
    }
}
