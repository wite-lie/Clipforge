package com.clipforge.material.understanding;

/**
 * MaterialUnderstandingStatus（素材理解状态）。
 *
 * <p>该枚举用于统一 cf_material_understanding（素材理解结果表）的 status（状态）字段取值。
 */
public enum MaterialUnderstandingStatus {

    /** PROCESSING（处理中）表示素材理解任务正在执行。 */
    PROCESSING,

    /** SUCCEEDED（成功）表示素材理解结果已经可复用。 */
    SUCCEEDED,

    /** FAILED（失败）表示素材理解失败。 */
    FAILED;

    /**
     * value（字符串值）用于写入数据库或返回前端。
     *
     * @return 状态字符串
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}
