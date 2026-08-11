package com.clipforge.tool.execution;

/**
 * ToolInvocationException（工具调用异常）。
 *
 * <p>该异常用于在统一工具执行器中包装工具名称和原始异常，方便上层定位是哪一个工具失败。
 */
public class ToolInvocationException extends RuntimeException {

    /** toolName（工具名称）表示失败工具。 */
    private final String toolName;

    public ToolInvocationException(String toolName, String message, Throwable cause) {
        super(message, cause);
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }
}
