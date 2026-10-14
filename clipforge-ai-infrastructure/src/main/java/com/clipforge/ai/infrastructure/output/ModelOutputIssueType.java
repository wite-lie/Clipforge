package com.clipforge.ai.infrastructure.output;

/**
 * ModelOutputIssueType（模型输出问题类型）。
 *
 * <p>该枚举用于描述模型已经返回文本之后，结构化解析前发现的输出问题。
 */
public enum ModelOutputIssueType {

    /** NONE（无问题）表示暂未发现明显输出问题。 */
    NONE,

    /** EMPTY_OUTPUT（空输出）表示模型返回内容为空。 */
    EMPTY_OUTPUT,

    /** TRUNCATED_BY_FINISH_REASON（被结束原因判定截断）表示供应商明确返回长度限制。 */
    TRUNCATED_BY_FINISH_REASON,

    /** UNCLOSED_JSON_OBJECT（JSON 对象未闭合）表示大括号数量不匹配。 */
    UNCLOSED_JSON_OBJECT,

    /** UNCLOSED_JSON_ARRAY（JSON 数组未闭合）表示中括号数量不匹配。 */
    UNCLOSED_JSON_ARRAY,

    /** UNCLOSED_JSON_STRING（JSON 字符串未闭合）表示引号没有正确闭合。 */
    UNCLOSED_JSON_STRING,

    /** SUSPICIOUS_TAIL（可疑尾部）表示输出末尾停在逗号、冒号等不完整位置。 */
    SUSPICIOUS_TAIL,

    /** INVALID_JSON（非法 JSON）表示输出不满足 JSON 外形。 */
    INVALID_JSON,

    /** MISSING_REQUIRED_FIELD（缺少必需字段）表示 JSON 可能完整但业务字段不完整。 */
    MISSING_REQUIRED_FIELD;
}
