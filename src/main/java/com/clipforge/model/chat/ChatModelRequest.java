package com.clipforge.model.chat;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ChatModelRequest（聊天模型请求）。
 *
 * <p>该对象封装一次 LLM（大语言模型）调用所需的消息、模型参数和业务追踪字段。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatModelRequest {

    /** runId（运行编号）用于把模型调用和 Agent（智能体）执行链路关联起来。 */
    private String runId;

    /** messages（消息列表）表示发送给 Chat Model（聊天模型）的 system/user/assistant（系统/用户/助手）消息。 */
    private List<ChatMessage> messages;

    /** temperature（温度）用于控制模型输出随机性，数值越低越稳定。 */
    private Double temperature;

    /** maxTokens（最大输出 Token 数）用于限制模型生成长度。 */
    private Integer maxTokens;

    /** purpose（调用用途）用于 Trace（执行链路追踪）和日志区分不同模型调用场景。 */
    private String purpose;
}
