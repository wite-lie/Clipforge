package com.clipforge.agent.intent;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ConversationIntentDecision（会话意图决策）。
 *
 * <p>该对象是 ConversationIntentAgent（会话意图智能体）的结构化输出，后续会交给
 * WorkflowRouter（工作流路由器）选择真正执行的应用服务或 Workflow（工作流）。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationIntentDecision {

    /** intentType（意图类型）表示当前用户输入的主要意图。 */
    private ConversationIntentType intentType;

    /** confidence（置信度）表示规则或模型对意图判断的把握程度，范围 0 到 1。 */
    private double confidence;

    /** recognitionMethod（识别方式）表示本次判断来自规则、LLM（大语言模型）还是兜底。 */
    private ConversationIntentRecognitionMethod recognitionMethod;

    /** routeTarget（路由目标）表示后续应进入的路由处理器名称。 */
    private String routeTarget;

    /** requiredContext（必需上下文）表示执行该路由最好具备哪些关键字段。 */
    private List<String> requiredContext;

    /** missingContext（缺失上下文）表示当前请求中缺少哪些关键字段，需要向用户追问。 */
    private List<String> missingContext;

    /** reason（原因）说明为什么做出当前意图判断，方便 Trace（执行链路追踪）和调试观察。 */
    private String reason;

    /** rawModelOutput（原始模型输出）保存 LLM（大语言模型）分类器的原始 JSON（对象表示）文本。 */
    private String rawModelOutput;
}
