package com.clipforge.tool.execution;

/**
 * ToolInvocationStatus（工具调用状态）。
 */
public enum ToolInvocationStatus {

    /** SUCCESS（成功）表示工具正常返回结果。 */
    SUCCESS,

    /** FAILED（失败）表示工具调用抛出异常。 */
    FAILED,

    /** FALLBACK（降级）表示工具失败后返回了降级结果。 */
    FALLBACK
}
