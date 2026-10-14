package com.clipforge.memory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ProjectMemorySnapshot（项目记忆快照）。
 *
 * <p>该对象表示当前 Project（项目）可用于创作链路的长期画像。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemorySnapshot {

    /** memoryId（记忆编号）用于追踪该项目记忆。 */
    private String memoryId;

    /** summary（摘要）描述项目长期定位。 */
    private String summary;

    /** targetPlatform（目标平台）表示项目常用平台。 */
    private String targetPlatform;

    /** targetAudience（目标受众）表示项目长期面向的人群。 */
    private String targetAudience;

    /** contentStyle（内容风格）表示项目稳定的内容调性。 */
    private String contentStyle;

    /** constraintsJson（约束 JSON）保存项目级额外限制。 */
    private String constraintsJson;
}
