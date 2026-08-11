package com.clipforge.model.rerank;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RerankModelProperties（重排序模型配置属性）。
 *
 * <p>该配置类绑定 `clipforge.rerank`（项目重排序模型配置），用于提前区分 Rerank（重排序）模型和 Embedding（嵌入）模型。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.rerank")
public class RerankModelProperties {

    /** enabled（是否启用）表示后续是否启用 Rerank（重排序）能力。 */
    private boolean enabled = false;

    /** mockEnabled（是否启用模拟重排序模型）为 true 时后续使用 Mock（模拟）实现。 */
    private boolean mockEnabled = true;

    /** provider（供应商）表示真实重排序模型供应商类型。 */
    private String provider = "openai-compatible";

    /** baseUrl（基础地址）表示真实 Rerank API（重排序接口）地址。 */
    private String baseUrl;

    /** apiKey（接口密钥）表示 Rerank（重排序）模型平台 API Key（接口密钥）。 */
    private String apiKey;

    /** modelName（模型名称）表示真实调用的 Rerank（重排序）模型 ID（模型编号）。 */
    private String modelName;

    /** topN（保留数量）表示重排序后最终保留多少条候选知识，当前按用户要求默认保留 5 条。 */
    private Integer topN = 5;

    /** maxCandidates（最大候选数）表示送入 Rerank（重排序）模型的最大候选切片数。 */
    private Integer maxCandidates = 20;

    /** logRequests（是否记录请求）用于本地调试重排序请求，生产环境建议关闭。 */
    private boolean logRequests = false;

    /** logResponses（是否记录响应）用于本地调试重排序响应，生产环境建议关闭。 */
    private boolean logResponses = false;
}
