package com.clipforge.quality.version;

/**
 * CreationPlanVersionType（创作方案版本类型）。
 *
 * <p>该枚举用于区分一次创作运行中的初始生成方案和修正后方案。
 */
public enum CreationPlanVersionType {

    /** INITIAL（初始生成）表示由创作生成器第一次生成并完成评估的方案。 */
    INITIAL,

    /** REVISED（修正版）表示根据评估建议执行 Reflection（反思修正）后的方案。 */
    REVISED
}
