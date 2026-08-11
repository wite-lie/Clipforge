package com.clipforge.memory.read;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MemoryReadRequest（记忆读取请求）。
 *
 * <p>该对象表示读取 MemoryContext（记忆上下文）所需的最小查询条件。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryReadRequest {

    /** runId（运行编号）用于后续 Trace（执行链路追踪）关联，可以为空。 */
    private String runId;

    /** userId（用户编号）用于读取用户偏好和兜底历史记忆。 */
    private String userId;

    /** projectId（项目编号）用于读取项目记忆和项目历史经验。 */
    private String projectId;

    /** conversationId（会话编号）用于读取会话滚动摘要。 */
    private String conversationId;

    /** platform（目标平台）用于筛选同平台历史创作经验。 */
    private String platform;

    /** userPreferenceLimit（用户偏好上限）用于避免注入过多偏好。 */
    @Builder.Default
    private int userPreferenceLimit = 8;

    /** historyMemoryLimit（历史记忆上限）用于避免注入过多历史摘要。 */
    @Builder.Default
    private int historyMemoryLimit = 5;

    /** highScoreThreshold（高分阈值）用于筛选历史高质量方案。 */
    @Builder.Default
    private int highScoreThreshold = 85;
}
