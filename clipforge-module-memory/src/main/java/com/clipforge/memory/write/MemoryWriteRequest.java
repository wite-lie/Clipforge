package com.clipforge.memory.write;

import com.clipforge.contract.creation.CreationPlanResult;
import com.clipforge.memory.extract.MemoryExtractionTriggerType;
import com.clipforge.memory.model.MemoryContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MemoryWriteRequest（记忆写入请求）。
 *
 * <p>该对象承载一次创作成功后的上下文数据，用于沉淀用户偏好、项目记忆和历史经验。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryWriteRequest {

    /** runId（运行编号）表示本次 Agent Run。 */
    private String runId;

    /** userId（用户编号）表示记忆归属用户。 */
    private String userId;

    /** projectId（项目编号）表示记忆归属项目。 */
    private String projectId;

    /** conversationId（会话编号）表示记忆来源会话。 */
    private String conversationId;

    /** platform（目标平台）表示本次创作面向的平台。 */
    private String platform;

    /** userInput（用户输入）表示本轮创作目标或对话内容。 */
    private String userInput;

    /** stylePreference（风格偏好）表示显式风格要求。 */
    private String stylePreference;

    /** finalResult（最终结果）表示本次创作的最终方案。 */
    private CreationPlanResult finalResult;

    /** existingMemoryContext（已有记忆上下文）表示写入前已经读取到的记忆。 */
    private MemoryContext existingMemoryContext;

    /** triggerType（触发类型）表示本次记忆写入属于哪种场景。 */
    @Builder.Default
    private MemoryExtractionTriggerType triggerType = MemoryExtractionTriggerType.CREATION_SUCCESS;

    /** triggerReason（触发原因）表示为什么会走到本次记忆写入。 */
    private String triggerReason;
}
