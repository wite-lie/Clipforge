package com.clipforge.tool.execution;

/**
 * ToolAction（工具动作）。
 *
 * <p>该函数接口用于把现有工具方法包装进 ToolExecutor（工具执行器），让第一阶段不必改动所有工具的业务方法签名。
 *
 * @param <T> 工具输出类型
 */
@FunctionalInterface
public interface ToolAction<T> {

    /**
     * execute（执行）用于运行实际工具动作。
     *
     * @return 工具输出
     */
    T execute();
}
