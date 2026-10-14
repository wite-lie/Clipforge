package com.clipforge.memory.infrastructure.persistence.enums;

/**
 * UserPreferenceMemoryType（用户偏好记忆类型）。
 *
 * <p>该枚举用于区分用户级 Memory（记忆）的业务含义。
 */
public enum UserPreferenceMemoryType {

    /** STYLE（风格）表示语气、表达方式、内容调性等偏好。 */
    STYLE,

    /** PLATFORM（平台）表示用户常用或默认的目标平台。 */
    PLATFORM,

    /** AUDIENCE（受众）表示用户常面向的目标人群。 */
    AUDIENCE,

    /** TOPIC（主题）表示用户长期关注的创作主题。 */
    TOPIC,

    /** NEGATIVE（负向偏好）表示用户明确不希望出现的表达或内容。 */
    NEGATIVE;

    /**
     * value（数据库值）用于返回当前枚举写入数据库的字符串。
     *
     * @return 数据库类型值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}
