package com.clipforge.prompt.budget;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * TokenBudgetProfile（词元预算档案）。
 *
 * <p>该对象表示某一种模型调用用途的预算配置，例如创作方案生成、评估、修正或输出修复。
 */
@Getter
@Setter
public class TokenBudgetProfile {

    /** contextWindow（上下文窗口）表示模型本次调用可容纳的总 Token（词元）上限。 */
    private Integer contextWindow;

    /** reservedOutputTokens（预留输出词元）表示为模型回答预留的 Token（词元）数量。 */
    private Integer reservedOutputTokens;

    /** safetyTokens（安全余量词元）表示为估算误差、系统额外开销和供应商差异预留的 Token（词元）数量。 */
    private Integer safetyTokens;

    /** sectionLimits（片段限制）表示每类上下文来源的局部预算。 */
    private Map<String, Integer> sectionLimits = new LinkedHashMap<>();
}
