package com.clipforge.memory.extract;

import com.clipforge.contract.creation.CreationPlanResult;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MemoryExtractionModelRequest（记忆抽取模型请求）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryExtractionModelRequest {

    /** runId（运行编号）用于链路追踪。 */
    private String runId;

    /** userId（用户编号）用于归属记忆。 */
    private String userId;

    /** projectId（项目编号）用于归属项目记忆。 */
    private String projectId;

    /** conversationId（会话编号）用于归属会话记忆。 */
    private String conversationId;

    /** platform（平台）用于帮助识别平台偏好。 */
    private String platform;

    /** userInput（用户输入）表示本轮对话或创作说明。 */
    private String userInput;

    /** stylePreference（风格偏好）表示显式风格输入。 */
    private String stylePreference;

    /** finalResult（最终结果）表示本轮创作结果，可用于提炼主题。 */
    private CreationPlanResult finalResult;

    /** ruleMemories（规则抽取结果）用于帮助模型避免重复抽取。 */
    private List<ExtractedUserPreferenceMemory> ruleMemories;

    /** triggerType（触发类型）用于说明本次为什么调用 LLM。 */
    private MemoryExtractionTriggerType triggerType;

    /** triggerReason（触发原因）用于说明调用上下文。 */
    private String triggerReason;
}
