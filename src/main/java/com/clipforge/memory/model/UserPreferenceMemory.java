package com.clipforge.memory.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * UserPreferenceMemory（用户偏好记忆）。
 *
 * <p>该对象是 MemoryContext（记忆上下文）中的用户偏好视图，屏蔽数据库实体细节。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceMemory {

    /** memoryId（记忆编号）用于追踪该偏好来源。 */
    private String memoryId;

    /** memoryType（记忆类型）表示风格、平台、受众、主题或负向偏好。 */
    private String memoryType;

    /** memoryKey（记忆键）表示偏好的具体维度。 */
    private String memoryKey;

    /** memoryValue（记忆值）表示后续可注入 Prompt（提示词）的偏好内容。 */
    private String memoryValue;

    /** confidence（置信度）表示该偏好可信程度。 */
    private BigDecimal confidence;

    /** sourceType（来源类型）表示该偏好来自用户明确表达、系统推断或人工维护。 */
    private String sourceType;
}
