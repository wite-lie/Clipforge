package com.clipforge.api.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UserPreferenceMemoryResponse（用户偏好记忆响应）。
 *
 * <p>该对象用于接口返回用户长期偏好记忆，避免直接暴露数据库 Entity（实体）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceMemoryResponse {

    /** memoryId（记忆编号）表示对外使用的记忆唯一编号。 */
    private String memoryId;

    /** userId（用户编号）表示该偏好归属用户。 */
    private String userId;

    /** memoryType（记忆类型）表示风格、平台、受众、主题或负向偏好。 */
    private String memoryType;

    /** memoryKey（记忆键）表示偏好的具体维度。 */
    private String memoryKey;

    /** memoryValue（记忆值）表示可注入 Prompt（提示词）的偏好内容。 */
    private String memoryValue;

    /** confidence（置信度）表示该记忆可信程度。 */
    private BigDecimal confidence;

    /** sourceType（来源类型）表示该记忆来自用户明确表达、系统推断或人工维护。 */
    private String sourceType;

    /** sourceRunId（来源运行编号）表示沉淀该记忆的 Agent Run（智能体运行）。 */
    private String sourceRunId;

    /** status（状态）表示记忆是否启用。 */
    private String status;
}
