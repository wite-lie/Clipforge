package com.clipforge.model.embedding;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MockEmbeddingModelClient（模拟嵌入模型客户端）。
 *
 * <p>该实现不调用真实模型平台，而是根据文本哈希生成稳定伪向量，便于普通单元测试不消耗模型额度。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "clipforge.embedding", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockEmbeddingModelClient implements EmbeddingModelClient {

    /** properties（配置属性）用于读取模拟向量维度。 */
    private final EmbeddingModelProperties properties;

    /**
     * embed（生成模拟嵌入向量）用于生成稳定、可重复的伪向量。
     *
     * @param request 嵌入模型请求
     * @return 嵌入模型响应
     */
    @Override
    public EmbeddingModelResponse embed(EmbeddingModelRequest request) {
        // 校验请求文本，避免生成无意义向量。
        validateRequest(request);
        // 读取配置维度，缺省时使用 1024。
        int dimension = properties.getDimension() == null ? 1024 : properties.getDimension();
        // 为每一段文本生成一个稳定伪向量。
        List<List<Float>> vectors = request.getTexts().stream()
                // 调用单段文本向量生成方法。
                .map(text -> createVector(text, dimension))
                // 收集为向量列表。
                .toList();
        // 估算输入 Token（词元）数量。
        int estimatedTokens = estimateTokens(request.getTexts());
        // 构造模拟响应。
        return EmbeddingModelResponse.builder()
                // 标记供应商为 mock。
                .provider("mock")
                // 标记模型名称。
                .modelName("mock-embedding-" + dimension)
                // 设置向量维度。
                .dimension(dimension)
                // 设置向量列表。
                .embeddings(vectors)
                // 设置估算输入词元。
                .promptTokens(estimatedTokens)
                // 设置估算总词元。
                .totalTokens(estimatedTokens)
                // 构建响应。
                .build();
    }

    /**
     * createVector（创建向量）用于把文本哈希展开为固定维度浮点向量。
     *
     * @param text 文本
     * @param dimension 向量维度
     * @return 模拟向量
     */
    private List<Float> createVector(String text, int dimension) {
        // 计算文本哈希字节，作为伪随机种子。
        byte[] digest = sha256(text);
        // 创建可变向量数组。
        java.util.ArrayList<Float> vector = new java.util.ArrayList<>(dimension);
        // 用哈希字节循环填充目标维度。
        for (int index = 0; index < dimension; index++) {
            // 读取一个哈希字节并转为无符号整数。
            int value = Byte.toUnsignedInt(digest[index % digest.length]);
            // 映射到 -1 到 1 之间，模拟真实向量取值范围。
            vector.add((value / 127.5f) - 1.0f);
        }
        // 返回稳定伪向量。
        return vector;
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
     * estimateTokens（估算词元）用于在 Mock（模拟）模式下给出成本参考。
     *
     * @param texts 文本列表
     * @return 估算词元数量
     */
    private int estimateTokens(List<String> texts) {
        // 中文场景先按两个字符约等于一个 Token（词元）粗略估算。
        return texts.stream()
                // 取每段文本长度。
                .mapToInt(text -> Math.max(1, (int) Math.ceil(text.length() / 2.0)))
                // 汇总。
                .sum();
    }

    /**
     * sha256（计算 SHA-256）用于生成稳定哈希种子。
     *
     * @param text 文本
     * @return 哈希字节
     */
    private byte[] sha256(String text) {
        try {
            // 创建 SHA-256（安全哈希算法）摘要器。
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            // 返回 UTF-8（统一字符编码）文本哈希。
            return messageDigest.digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            // 理论上 JVM（Java 虚拟机）都支持 SHA-256，这里兜底包装。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SHA-256 哈希算法不可用", exception);
        }
    }
}
