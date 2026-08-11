package com.clipforge.model.routing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ModelRoutingProperties（模型路由配置）。
 *
 * <p>该配置负责控制模型供应商路由、故障转移、Mock（模拟）降级和健康状态跳过策略。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.model-routing")
public class ModelRoutingProperties {

    /** enabled（是否启用）为 true 时启用各模型客户端的路由包装器。 */
    private boolean enabled = false;

    /** failoverEnabled（是否启用故障转移）为 true 时主候选失败后继续尝试备用候选。 */
    private boolean failoverEnabled = true;

    /** mockFallbackEnabled（是否允许 Mock 降级）控制真实模型失败后是否可回退到模拟模型。 */
    private boolean mockFallbackEnabled = true;

    /** skipUnhealthyCandidate（是否跳过不健康候选）控制连续失败候选是否暂时跳过。 */
    private boolean skipUnhealthyCandidate = true;

    /** unhealthyFailureThreshold（不健康阈值）达到该连续失败次数后认为候选不健康。 */
    private int unhealthyFailureThreshold = 3;

    /** routes（路由明细）按模型能力配置供应商优先级。 */
    private Map<String, RouteProperties> routes = new LinkedHashMap<>();

    public RouteProperties routeOf(ModelCapability capability) {
        if (capability == null || routes == null) {
            return new RouteProperties();
        }
        return routes.getOrDefault(capability.configKey(), new RouteProperties());
    }

    public boolean mockFallbackEnabled(ModelCapability capability) {
        RouteProperties route = routeOf(capability);
        return route.getMockFallbackEnabled() == null ? mockFallbackEnabled : route.getMockFallbackEnabled();
    }

    public List<String> providerOrder(ModelCapability capability) {
        RouteProperties route = routeOf(capability);
        return route.getProviderOrder() == null ? List.of() : route.getProviderOrder();
    }

    @Getter
    @Setter
    public static class RouteProperties {

        /** providerOrder（供应商顺序）例如 langchain4j、bailian、mock。 */
        private List<String> providerOrder = new ArrayList<>();

        /** candidates（候选模型列表）用于配置同一能力下多个真实模型或 Mock（模拟）降级模型。 */
        private List<CandidateProperties> candidates = new ArrayList<>();

        /** mockFallbackEnabled（是否允许 Mock 降级）为空时继承全局配置。 */
        private Boolean mockFallbackEnabled;
    }

    @Getter
    @Setter
    public static class CandidateProperties {

        /** enabled（是否启用）用于临时下线某个候选模型。 */
        private boolean enabled = true;

        /** id（候选编号）用于路由日志和健康状态定位。 */
        private String id;

        /** provider（供应商）例如 bailian、siliconflow、ollama、openai-compatible、mock。 */
        private String provider;

        /** baseUrl（基础地址）是真实模型接口地址。 */
        private String baseUrl;

        /** apiKey（接口密钥）是真实模型访问凭证。 */
        private String apiKey;

        /** modelName（模型名称）是真实调用的模型 ID（模型编号）。 */
        private String modelName;

        /** temperature（温度）用于 Chat（聊天）和 Vision（视觉）模型。 */
        private Double temperature;

        /** maxTokens（最大输出词元数）用于 Chat（聊天）和 Vision（视觉）模型。 */
        private Integer maxTokens;

        /** dimension（向量维度）用于 Embedding（嵌入）模型。 */
        private Integer dimension;

        /** maxSegmentsPerBatch（单批最大文本段数）用于 Embedding（嵌入）模型。 */
        private Integer maxSegmentsPerBatch;

        /** topN（保留数量）用于 Rerank（重排序）模型。 */
        private Integer topN;

        /** maxCandidates（最大候选数）用于 Rerank（重排序）模型。 */
        private Integer maxCandidates;

        /** timeoutSeconds（请求超时秒数）用于 ASR（自动语音识别）模型。 */
        private Long timeoutSeconds;

        /** audioFormat（音频格式）用于 ASR（自动语音识别）模型。 */
        private String audioFormat;

        /** sampleRate（采样率）用于 ASR（自动语音识别）模型。 */
        private Integer sampleRate;

        /** logRequests（是否记录请求）用于本地调试，生产建议关闭。 */
        private Boolean logRequests;

        /** logResponses（是否记录响应）用于本地调试，生产建议关闭。 */
        private Boolean logResponses;
    }
}
