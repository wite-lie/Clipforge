package com.clipforge.ai.infrastructure.chat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ChatModelProperties（聊天模型配置属性）。
 *
 * <p>该配置类绑定 `clipforge.model`（项目模型配置）下的真实模型参数，用于创建 LangChain4jChatModelClient（LangChain4j 聊天模型客户端）。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.model")
public class ChatModelProperties {

    /** mockEnabled（是否启用模拟模型）为 true 时使用 MockChatModelClient（模拟聊天模型客户端）。 */
    private boolean mockEnabled = true;

    /** provider（供应商）表示真实模型供应商类型，当前支持 openai-compatible（OpenAI 兼容接口）。 */
    private String provider = "openai-compatible";

    /** baseUrl（基础地址）表示 OpenAI-Compatible API（OpenAI 兼容接口）的基础地址。 */
    private String baseUrl;

    /** apiKey（接口密钥）表示模型平台 API Key（接口密钥），应通过环境变量注入。 */
    private String apiKey;

    /** modelName（模型名称）表示真实调用的模型 ID（模型编号）。 */
    private String modelName;

    /** temperature（温度）控制模型输出随机性。 */
    private Double temperature = 0.2;

    /** maxTokens（最大输出词元数）控制模型最多生成多少 Token（词元）。 */
    private Integer maxTokens = 2048;

    /** logRequests（是否记录请求）用于调试模型请求，生产环境建议关闭。 */
    private boolean logRequests = false;

    /** logResponses（是否记录响应）用于调试模型响应，生产环境建议关闭。 */
    private boolean logResponses = false;
}
