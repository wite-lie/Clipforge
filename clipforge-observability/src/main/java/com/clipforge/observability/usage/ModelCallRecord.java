package com.clipforge.observability.usage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ModelCallRecord（模型调用记录）。
 *
 * <p>该对象记录一次 LLM（大语言模型）或 VLM（视觉语言模型）调用的供应商、模型、Token（词元）用量、耗时和成功状态。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCallRecord {

    /** runId（运行编号）表示本次模型调用所属的 Agent（智能体）运行。 */
    private String runId;

    /** projectId（项目编号）表示后续 Project（项目）维度，当前阶段可以为空。 */
    private String projectId;

    /** conversationId（会话编号）表示后续 Conversation（会话）维度，当前阶段可以为空。 */
    private String conversationId;

    /** provider（供应商）表示模型服务来源。 */
    private String provider;

    /** modelName（模型名称）表示实际调用的模型。 */
    private String modelName;

    /** purpose（调用用途）用于区分创作生成、输出修复、评估等调用场景。 */
    private String purpose;

    /** promptTokens（输入词元数）表示 Prompt（提示词）消耗。 */
    private Integer promptTokens;

    /** completionTokens（输出词元数）表示模型生成消耗。 */
    private Integer completionTokens;

    /** totalTokens（总词元数）表示输入和输出合计消耗。 */
    private Integer totalTokens;

    /** latencyMs（调用耗时毫秒）表示模型调用耗时。 */
    private Long latencyMs;

    /** success（是否成功）表示本次模型调用是否成功返回。 */
    private Boolean success;

    /** errorCode（错误码）表示失败时的错误类型。 */
    private String errorCode;

    /** errorMessage（错误消息）表示失败时的简要原因。 */
    private String errorMessage;
}
