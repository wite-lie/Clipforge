package com.clipforge.ai.api.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ChatModelResponse（聊天模型响应）。
 *
 * <p>该对象封装 Chat Model（聊天模型）返回的文本内容、模型标识和 Token（词元）用量。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatModelResponse {

    /** provider（供应商）表示本次响应来自哪个模型供应商，例如 mock（模拟）、openai-compatible（OpenAI 兼容接口）。 */
    private String provider;

    /** modelName（模型名称）表示本次调用使用的具体模型。 */
    private String modelName;

    /** content（内容）表示模型生成的文本。 */
    private String content;

    /** promptTokens（输入 Token 数）表示提示词消耗的 Token（词元）数量，未知时可以为空。 */
    private Integer promptTokens;

    /** completionTokens（输出 Token 数）表示模型回答消耗的 Token（词元）数量，未知时可以为空。 */
    private Integer completionTokens;

    /** totalTokens（总 Token 数）表示输入和输出合计 Token（词元）数量，未知时可以为空。 */
    private Integer totalTokens;

    /** finishReason（结束原因）表示模型停止生成的原因，例如 stop（正常结束）、length（长度限制）或 unknown（未知）。 */
    private String finishReason;

    /** responseLength（响应字符数）表示模型文本输出的字符长度。 */
    private Integer responseLength;

    /** truncatedSuspected（是否疑似截断）表示本地规则是否认为输出可能未完整返回。 */
    private Boolean truncatedSuspected;

    /** rawErrorMessage（原始错误消息）表示供应商或输出治理过程中附带的错误说明。 */
    private String rawErrorMessage;
}
