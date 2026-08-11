package com.clipforge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationHistoryMemoryResponse（创作历史记忆响应）。
 *
 * <p>该对象用于接口返回历史高分创作方案摘要。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationHistoryMemoryResponse {

    /** memoryId（记忆编号）表示对外使用的记忆唯一编号。 */
    private String memoryId;

    /** userId（用户编号）表示历史记忆归属用户。 */
    private String userId;

    /** projectId（项目编号）表示历史记忆归属项目。 */
    private String projectId;

    /** conversationId（会话编号）表示历史记忆来源会话。 */
    private String conversationId;

    /** runId（运行编号）表示历史记忆来源的 Agent Run（智能体运行）。 */
    private String runId;

    /** theme（创作主题）表示历史方案主题。 */
    private String theme;

    /** platform（目标平台）表示历史方案面向平台。 */
    private String platform;

    /** summary（摘要）表示历史方案可复用经验。 */
    private String summary;

    /** score（评分）表示历史方案最终评分。 */
    private Integer score;

    /** tagsJson（标签 JSON）表示主题、风格或经验标签。 */
    private String tagsJson;

    /** status（状态）表示历史记忆是否启用。 */
    private String status;
}
