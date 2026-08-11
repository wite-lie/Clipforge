package com.clipforge.model.rerank;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.model.governance.ModelCallLimiter;
import com.clipforge.model.governance.ModelCallType;
import com.clipforge.model.governance.NoOpModelCallLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * BailianRerankModelClient（百炼重排序模型客户端）。
 *
 * <p>该实现按照阿里云百炼 Rerank API（重排序接口）格式调用专用 Cross-Encoder（交叉编码器）类模型，不使用 ChatModel（聊天模型）。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.rerank", name = "mock-enabled", havingValue = "false")
public class BailianRerankModelClient implements RerankModelClient {

    /** DEFAULT_RERANK_PATH（默认重排序路径）是百炼官方文本重排序接口路径。 */
    private static final String DEFAULT_RERANK_PATH = "/api/v1/services/rerank/text-rerank/text-rerank";

    /** properties（重排序模型配置）保存供应商、地址、密钥、模型名称等配置。 */
    private final RerankModelProperties properties;

    /** objectMapper（对象映射器）用于构造和解析 JSON（对象表示格式）。 */
    private final ObjectMapper objectMapper;

    /** httpClient（HTTP 客户端）用于发起真实 Rerank API（重排序接口）调用。 */
    private final HttpClient httpClient;

    /** modelCallLimiter（模型调用限流器）用于控制真实 Rerank（重排序）调用并发。 */
    private final ModelCallLimiter modelCallLimiter;

    /**
     * 构造函数用于创建百炼重排序客户端。
     *
     * @param properties 重排序模型配置
     * @param objectMapper 对象映射器
     */
    public BailianRerankModelClient(RerankModelProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, new NoOpModelCallLimiter());
    }

    /**
     * 构造函数用于注入模型调用限流器。
     *
     * @param properties 重排序模型配置
     * @param objectMapper 对象映射器
     * @param modelCallLimiter 模型调用限流器
     */
    @Autowired
    public BailianRerankModelClient(RerankModelProperties properties, ObjectMapper objectMapper, ModelCallLimiter modelCallLimiter) {
        // 保存配置。
        this.properties = properties;
        // 保存对象映射器。
        this.objectMapper = objectMapper;
        // 保存模型调用限流器，真实 HTTP（超文本传输协议）调用前会先获取许可。
        this.modelCallLimiter = modelCallLimiter;
        // 创建 JDK（Java 开发工具包）内置 HTTP 客户端。
        this.httpClient = HttpClient.newBuilder()
                // 设置连接超时。
                .connectTimeout(Duration.ofSeconds(30))
                // 构建客户端。
                .build();
    }

    /**
     * rerank（重排序）用于调用真实百炼 Rerank API（重排序接口）。
     *
     * @param request 重排序模型请求
     * @return 重排序模型响应
     */
    @Override
    public RerankModelResponse rerank(RerankModelRequest request) {
        try {
            // 校验配置。
            validateProperties();
            // 校验请求。
            validateRequest(request);
            // 构造请求体。
            Map<String, Object> body = buildRequestBody(request);
            // 转换为 JSON（对象表示格式）。
            // 按需打印请求。
            logRequest(request);
            // 构造 HTTP（超文本传输协议）请求。
            String endpoint = resolveEndpoint();
            // 构造 HTTP（超文本传输协议）请求。
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    // 设置接口地址。
                    .uri(URI.create(endpoint))
                    // 设置请求超时。
                    .timeout(Duration.ofSeconds(120))
                    // 设置鉴权头。
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    // 设置内容类型。
                    .header("Content-Type", "application/json")
                    // 设置 POST（提交）请求体。
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    // 构建请求。
                    .build();
            // 发起请求。
            HttpResponse<String> response = modelCallLimiter.execute(
                    ModelCallType.RERANK,
                    rerankBizId(request),
                    () -> httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()));
            // 按需打印响应。
            logResponse(response.body());
            // 校验 HTTP（超文本传输协议）状态。
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                // 抛出模型调用错误。
                throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "百炼 Rerank API 调用失败，HTTP 状态码："
                        + response.statusCode() + "，响应摘要：" + compact(response.body(), 500));
            }
            // 解析响应。
            return parseResponse(response.body(), request);
        } catch (BusinessException exception) {
            // 业务异常直接抛出。
            throw exception;
        } catch (Exception exception) {
            // 其他异常包装为模型调用错误。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "调用百炼 Rerank API 失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * buildRequestBody（构造请求体）用于按官方 curl 示例生成请求 JSON（对象表示格式）。
     *
     * @param request 重排序模型请求
     * @return 请求体映射
     */
    private Map<String, Object> buildRequestBody(RerankModelRequest request) {
        // 创建 input（输入）对象。
        Map<String, Object> input = new LinkedHashMap<>();
        // 设置查询文本。
        input.put("query", request.getQuery());
        // 设置候选文档文本列表。
        input.put("documents", request.getCandidates().stream().map(RerankCandidate::getText).toList());
        // 创建 parameters（参数）对象。
        Map<String, Object> parameters = new LinkedHashMap<>();
        // 设置是否返回文档。
        parameters.put("return_documents", request.getReturnDocuments() == null || request.getReturnDocuments());
        // 设置 top_n（保留数量）。
        parameters.put("top_n", request.getTopN());
        // 创建完整请求体。
        Map<String, Object> body = new LinkedHashMap<>();
        // 设置模型名称。
        body.put("model", properties.getModelName());
        // 设置 input（输入）。
        body.put("input", input);
        // 设置 parameters（参数）。
        body.put("parameters", parameters);
        // 返回请求体。
        return body;
    }

    /**
     * parseResponse（解析响应）用于把百炼响应转换为项目统一响应。
     *
     * @param responseBody 响应正文
     * @param request 原始请求
     * @return 重排序模型响应
     * @throws Exception JSON（对象表示格式）解析异常
     */
    private RerankModelResponse parseResponse(String responseBody, RerankModelRequest request) throws Exception {
        // 解析响应 JSON（对象表示格式）。
        JsonNode root = objectMapper.readTree(responseBody);
        // 读取结果数组，兼容 output.results 和 results 两种结构。
        JsonNode resultsNode = root.at("/output/results");
        // 如果 output.results 不存在，则尝试根级 results。
        if (!resultsNode.isArray()) {
            // 读取根级 results。
            resultsNode = root.path("results");
        }
        // 如果仍然不存在，则抛出解析错误。
        if (!resultsNode.isArray()) {
            // 抛出解析错误。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "百炼 Rerank API 响应缺少 output.results");
        }
        // 创建结果列表。
        List<RerankScoredItem> results = new ArrayList<>();
        // 遍历结果数组。
        for (JsonNode resultNode : resultsNode) {
            // 读取输入索引。
            int index = resultNode.path("index").asInt(-1);
            // 如果索引非法，则跳过。
            if (index < 0 || index >= request.getCandidates().size()) {
                // 继续下一条。
                continue;
            }
            // 读取候选对象。
            RerankCandidate candidate = request.getCandidates().get(index);
            // 读取相关性分数。
            double score = resultNode.path("relevance_score").asDouble(resultNode.path("score").asDouble(0.0));
            // 读取返回文档文本，兼容 document（文档）为字符串或对象两种形态。
            String documentText = readDocumentText(resultNode, candidate);
            // 添加结果。
            results.add(RerankScoredItem.builder()
                    // 设置索引。
                    .index(index)
                    // 设置候选编号。
                    .candidateId(candidate.getCandidateId())
                    // 设置相关性分数。
                    .relevanceScore(score)
                    // 设置文档文本。
                    .documentText(documentText)
                    // 构建对象。
                    .build());
        }
        // 按相关性分数倒序，保证供应商返回顺序异常时仍稳定。
        results = results.stream()
                // 分数越大越靠前。
                .sorted(Comparator.comparing(RerankScoredItem::getRelevanceScore, Comparator.reverseOrder()))
                // 收集为列表。
                .toList();
        // 读取 Token（词元）用量。
        Integer totalTokens = readTotalTokens(root, request);
        // 构建响应。
        return RerankModelResponse.builder()
                // 设置供应商。
                .provider(properties.getProvider())
                // 设置模型名称。
                .modelName(properties.getModelName())
                // 设置结果。
                .results(results)
                // 设置输入 Token（词元）。
                .promptTokens(totalTokens)
                // 设置总 Token（词元）。
                .totalTokens(totalTokens)
                // 构建响应。
                .build();
    }

    /**
     * readDocumentText（读取文档文本）用于兼容不同 Rerank API（重排序接口）的文档返回格式。
     *
     * @param resultNode 单条重排序结果节点
     * @param candidate 原始候选文档
     * @return 文档文本
     */
    private String readDocumentText(JsonNode resultNode, RerankCandidate candidate) {
        // 读取 document（文档）节点。
        JsonNode documentNode = resultNode.path("document");
        // 如果 document（文档）直接是字符串，则直接返回。
        if (documentNode.isTextual()) {
            // 返回字符串文档。
            return documentNode.asText();
        }
        // 如果 document（文档）是对象并包含 text（文本）字段，则返回 text（文本）。
        if (documentNode.path("text").isTextual()) {
            // 返回对象中的 text（文本）。
            return documentNode.path("text").asText();
        }
        // 如果供应商未返回文档文本，则回退到本地候选文本。
        return candidate.getText();
    }

    /**
     * readTotalTokens（读取总词元）用于兼容供应商响应和本地估算。
     *
     * @param root 响应根节点
     * @param request 原始请求
     * @return 总 Token（词元）
     */
    private Integer readTotalTokens(JsonNode root, RerankModelRequest request) {
        // 优先读取 usage.total_tokens。
        if (root.at("/usage/total_tokens").isNumber()) {
            // 返回供应商 Token（词元）用量。
            return root.at("/usage/total_tokens").asInt();
        }
        // 供应商未返回时使用本地估算。
        return estimateTokens(request);
    }

    /**
     * rerankBizId（重排序业务编号）用于限流日志定位，避免把完整 query（查询文本）写入日志。
     *
     * @param request 重排序请求
     * @return 业务编号
     */
    private String rerankBizId(RerankModelRequest request) {
        // 请求为空时返回 unknown（未知）。
        if (request == null || isBlank(request.getQuery())) {
            return "unknown";
        }
        // 使用查询文本哈希定位同一批检索，不暴露完整用户输入。
        return "rerank-query-" + Integer.toHexString(request.getQuery().hashCode());
    }

    /**
     * estimateTokens（估算词元）用于供应商未返回用量时给出成本参考。
     *
     * @param request 重排序请求
     * @return 估算词元数量
     */
    private int estimateTokens(RerankModelRequest request) {
        // 统计查询文本长度。
        int length = request.getQuery().length();
        // 累加候选文本长度。
        length += request.getCandidates().stream().mapToInt(candidate -> candidate.getText().length()).sum();
        // 中文场景按两个字符约等于一个 Token（词元）估算。
        return Math.max(1, (int) Math.ceil(length / 2.0));
    }

    /**
     * resolveEndpoint（解析接口地址）用于兼容完整接口地址和工作空间根地址两种配置。
     *
     * @return 最终接口地址
     */
    private String resolveEndpoint() {
        // 读取基础地址。
        String baseUrl = normalizeBaseUrl(trimTrailingSlash(properties.getBaseUrl()));
        // 如果已经是完整重排序接口，则直接返回。
        if (baseUrl.endsWith(DEFAULT_RERANK_PATH)) {
            // 返回完整地址。
            return baseUrl;
        }
        // 如果配置到上一级 text-rerank（文本重排序）路径，则补齐最后一段。
        if (baseUrl.endsWith("/api/v1/services/rerank/text-rerank")) {
            // 返回补齐后的地址。
            return baseUrl + "/text-rerank";
        }
        // 否则追加百炼默认重排序接口路径。
        return baseUrl + DEFAULT_RERANK_PATH;
    }

    /**
     * normalizeBaseUrl（归一化基础地址）用于把聊天模型兼容地址转换为重排序接口可用的 workspace（工作空间）根地址。
     *
     * @param value 原始基础地址
     * @return 归一化后的基础地址
     */
    private String normalizeBaseUrl(String value) {
        // 如果用户误填了兼容模式地址，则裁剪掉该后缀。
        if (value.endsWith("/compatible-mode/v1")) {
            // 返回工作空间根地址。
            return value.substring(0, value.length() - "/compatible-mode/v1".length());
        }
        // 如果用户直接填了 /v1，也尝试裁剪成 workspace 根地址。
        if (value.endsWith("/v1")) {
            // 返回工作空间根地址。
            return value.substring(0, value.length() - "/v1".length());
        }
        // 其他情况保持不变。
        return value;
    }

    /**
     * validateProperties（校验配置）用于检查真实重排序模型必要配置。
     */
    private void validateProperties() {
        // 校验基础地址。
        if (isBlank(properties.getBaseUrl())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.rerank.base-url 不能为空");
        }
        // 校验接口密钥。
        if (isBlank(properties.getApiKey())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.rerank.api-key 不能为空");
        }
        // 校验模型名称。
        if (isBlank(properties.getModelName())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.rerank.model-name 不能为空");
        }
    }

    /**
     * validateRequest（校验请求）用于保证重排序输入有效。
     *
     * @param request 重排序请求
     */
    private void validateRequest(RerankModelRequest request) {
        // 校验查询文本。
        if (request == null || isBlank(request.getQuery())) {
            // 抛出请求错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Rerank 请求 query（查询文本）不能为空");
        }
        // 校验候选列表。
        if (request.getCandidates() == null || request.getCandidates().isEmpty()) {
            // 抛出请求错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Rerank 请求 candidates（候选列表）不能为空");
        }
        // 校验候选文本。
        if (request.getCandidates().stream().anyMatch(candidate -> candidate == null || isBlank(candidate.getText()))) {
            // 抛出请求错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Rerank 请求 candidates（候选列表）不能包含空文本");
        }
    }

    /**
     * logRequest（打印请求）用于本地调试真实重排序调用。
     *
     * @param request 重排序请求
     */
    private void logRequest(RerankModelRequest request) {
        // 如果未启用请求日志，则跳过。
        if (!properties.isLogRequests()) {
            // 结束处理。
            return;
        }
        System.out.println("Rerank 模型请求摘要：model=" + properties.getModelName()
                + "，queryLength=" + (request == null || request.getQuery() == null ? 0 : request.getQuery().length())
                + "，candidateCount=" + (request == null || request.getCandidates() == null ? 0 : request.getCandidates().size())
                + "，topN=" + (request == null ? null : request.getTopN()));
    }

    /**
     * logResponse（打印响应）用于本地调试真实重排序调用。
     *
     * @param responseJson 响应 JSON（对象表示格式）
     */
    private void logResponse(String responseJson) {
        // 如果未启用响应日志，则跳过。
        if (!properties.isLogResponses()) {
            // 结束处理。
            return;
        }
        System.out.println("Rerank 模型响应摘要：responseLength=" + (responseJson == null ? 0 : responseJson.length())
                + "，responsePreview=" + compact(responseJson, 300));
    }

    private String compact(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...<省略>";
    }

    /**
     * trimTrailingSlash（去掉末尾斜杠）用于拼接接口地址。
     *
     * @param value 原始地址
     * @return 去掉末尾斜杠后的地址
     */
    private String trimTrailingSlash(String value) {
        // 如果地址以斜杠结尾，则去掉最后一个斜杠。
        if (value.endsWith("/")) {
            // 返回裁剪后的地址。
            return value.substring(0, value.length() - 1);
        }
        // 返回原始地址。
        return value;
    }

    /**
     * isBlank（是否空白）用于判断字符串是否为空。
     *
     * @param value 原始值
     * @return true 表示空白
     */
    private boolean isBlank(String value) {
        // 判断空引用或空白字符串。
        return value == null || value.isBlank();
    }
}
