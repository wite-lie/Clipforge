package com.clipforge.agent.intent;

/**
 * ConversationIntentType（会话意图类型）。
 *
 * <p>该枚举用于表达用户在当前会话轮次中真正想做的事情，供 WorkflowRouter（工作流路由器）
 * 决定是否进入创作 Workflow（工作流），还是转向状态查询、成本查询、质量复盘或澄清回复。
 */
public enum ConversationIntentType {

    /** GENERATE_PLAN（生成方案）表示用户希望生成新的短视频创作方案。 */
    GENERATE_PLAN,

    /** REVISE_PLAN（修正方案）表示用户希望基于已有方案或已有上下文继续优化。 */
    REVISE_PLAN,

    /** EXPLAIN_EVALUATION（解释评估）表示用户希望理解评分、修正原因或质量复盘。 */
    EXPLAIN_EVALUATION,

    /** QUERY_USAGE_COST（查询用量成本）表示用户希望查看 Token（词元）消耗、调用次数或估算成本。 */
    QUERY_USAGE_COST,

    /** QUERY_MATERIAL_STATUS（查询素材状态）表示用户希望知道图片、视频或处理任务是否已经解析完成。 */
    QUERY_MATERIAL_STATUS,

    /** PROCESS_MATERIAL（处理素材）表示用户刚上传或补充了素材，当前重点是处理素材而不是立即生成方案。 */
    PROCESS_MATERIAL,

    /** KNOWLEDGE_QA（知识问答）表示用户希望基于知识库或项目资料回答问题。 */
    KNOWLEDGE_QA,

    /** CHITCHAT（闲聊）表示用户只是在寒暄或进行非任务型交流。 */
    CHITCHAT,

    /** CLARIFICATION（澄清）表示用户输入信息不足，需要系统追问关键条件。 */
    CLARIFICATION,

    /** QUERY_HISTORY（查询历史）表示用户希望查询历史方案或会话内容，保留旧接口兼容。 */
    QUERY_HISTORY,

    /** UNKNOWN（未知）表示当前规则和模型都无法稳定判断用户意图。 */
    UNKNOWN
}
