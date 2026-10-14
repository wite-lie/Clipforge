package com.clipforge.memory.model;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MemoryContext（记忆上下文）。
 *
 * <p>该对象是创作 Workflow（工作流）后续读取 Memory（记忆）的统一入口。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryContext {

    /** userId（用户编号）表示当前记忆上下文归属的用户。 */
    private String userId;

    /** projectId（项目编号）表示当前记忆上下文归属的项目。 */
    private String projectId;

    /** conversationId（会话编号）表示当前记忆上下文归属的会话。 */
    private String conversationId;

    /** platform（目标平台）表示本次创作请求的平台。 */
    private String platform;

    /** userPreferences（用户偏好）表示用户级长期偏好。 */
    @Builder.Default
    private List<UserPreferenceMemory> userPreferences = Collections.emptyList();

    /** projectMemory（项目记忆）表示项目级长期画像。 */
    private ProjectMemorySnapshot projectMemory;

    /** historyMemories（历史记忆）表示历史高质量方案摘要。 */
    @Builder.Default
    private List<CreationHistoryMemorySnapshot> historyMemories = Collections.emptyList();

    /** conversationSummary（会话摘要）表示短期记忆的滚动摘要。 */
    private ConversationSummarySnapshot conversationSummary;

    /** stats（读取统计）表示本次记忆读取命中情况。 */
    private MemoryReadStats stats;

    /**
     * hasAnyMemory（是否有任何记忆）用于判断是否需要在后续 Prompt（提示词）中注入记忆段落。
     *
     * @return 有任意记忆时返回 true
     */
    public boolean hasAnyMemory() {
        // 只要用户偏好、项目记忆、历史记忆或会话摘要任意存在，就认为命中记忆。
        return !userPreferences.isEmpty()
                || projectMemory != null
                || !historyMemories.isEmpty()
                || conversationSummary != null;
    }
}
