package com.clipforge.ai.infrastructure.output;

/**
 * ModelOutputRepairStatus（模型输出修复状态）。
 *
 * <p>该枚举用于记录模型输出治理流程最终是否需要修复、是否修复成功以及是否降级。
 */
public enum ModelOutputRepairStatus {

    /** NOT_NEEDED（无需修复）表示原始输出可直接解析使用。 */
    NOT_NEEDED,

    /** REPAIR_NEEDED（需要修复）表示输出存在可修复问题。 */
    REPAIR_NEEDED,

    /** REPAIRED（修复成功）表示二次修复后可用。 */
    REPAIRED,

    /** REPAIR_FAILED（修复失败）表示二次修复后仍不可用。 */
    REPAIR_FAILED,

    /** DEGRADED（已降级）表示系统不再继续使用该输出。 */
    DEGRADED,

    /** UNREPAIRABLE（不可修复）表示当前输出不适合自动修复。 */
    UNREPAIRABLE;
}
