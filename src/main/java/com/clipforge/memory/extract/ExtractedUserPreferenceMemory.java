package com.clipforge.memory.extract;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ExtractedUserPreferenceMemory（抽取出的用户偏好记忆）。
 *
 * <p>该对象是规则抽取器或后续 LLM（大语言模型）抽取器的中间结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedUserPreferenceMemory {

    /** memoryType（记忆类型）表示风格、平台、受众、主题或负向偏好。 */
    private String memoryType;

    /** memoryKey（记忆键）表示偏好的具体维度。 */
    private String memoryKey;

    /** memoryValue（记忆值）表示可长期复用的偏好内容。 */
    private String memoryValue;

    /** confidence（置信度）表示抽取结果可信程度。 */
    private BigDecimal confidence;

    /** sourceType（来源类型）表示用户明确表达或系统推断。 */
    private String sourceType;
}
