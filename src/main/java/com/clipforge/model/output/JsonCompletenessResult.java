package com.clipforge.model.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JsonCompletenessResult（JSON 完整性检查结果）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonCompletenessResult {

    /** complete（是否完整）表示 JSON 外形是否闭合。 */
    private boolean complete;

    /** issueType（问题类型）表示发现的第一个明显结构问题。 */
    private ModelOutputIssueType issueType;

    /** reason（原因）用于 Trace（执行链路追踪）和测试打印。 */
    private String reason;

    /** objectBalance（对象括号余额）大于 0 表示对象未闭合。 */
    private int objectBalance;

    /** arrayBalance（数组括号余额）大于 0 表示数组未闭合。 */
    private int arrayBalance;
}
