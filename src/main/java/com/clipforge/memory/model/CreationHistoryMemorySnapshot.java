package com.clipforge.memory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CreationHistoryMemorySnapshot（创作历史记忆快照）。
 *
 * <p>该对象表示历史高质量创作方案沉淀出的可复用经验。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationHistoryMemorySnapshot {

    /** memoryId（记忆编号）用于追踪该历史记忆。 */
    private String memoryId;

    /** runId（运行编号）表示该历史记忆来自哪一次 Agent Run（智能体运行）。 */
    private String runId;

    /** theme（创作主题）表示历史方案主题。 */
    private String theme;

    /** platform（目标平台）表示历史方案面向的平台。 */
    private String platform;

    /** summary（摘要）保存历史方案可复用经验。 */
    private String summary;

    /** score（评分）表示历史方案最终评分。 */
    private Integer score;

    /** tagsJson（标签 JSON）保存主题、风格或经验标签。 */
    private String tagsJson;
}
