package com.clipforge.tool.execution;

/**
 * ToolExecutor（工具执行器）。
 *
 * <p>该接口统一包裹工具调用，负责 Trace（执行链路追踪）、耗时统计、失败包装和后续超时重试策略。
 */
public interface ToolExecutor {

    /**
     * execute（执行工具）用于执行一次工具动作。
     *
     * @param request 工具调用请求
     * @param <T> 工具输出类型
     * @return 工具调用结果
     */
    <T> ToolInvocationResult<T> execute(ToolInvocationRequest<T> request);
}
