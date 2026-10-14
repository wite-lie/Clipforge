package com.clipforge.tool.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ToolInvocationResult（工具调用结果）。
 *
 * <p>该对象统一承载工具输出、状态、耗时和失败原因，方便 Trace（执行链路追踪）和后续前端复盘。
 *
 * @param <T> 工具输出类型
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolInvocationResult<T> {

    /** toolName（工具名称）表示被调用的工具。 */
    private String toolName;

    /** status（调用状态）表示成功、失败或降级。 */
    private ToolInvocationStatus status;

    /** output（工具输出）表示真实返回值。 */
    private T output;

    /** latencyMs（耗时毫秒）表示工具调用耗时。 */
    private long latencyMs;

    /** errorMessage（错误消息）表示失败原因。 */
    private String errorMessage;

    /**
     * succeeded（是否成功）用于调用方快速判断。
     *
     * @return 是否成功
     */
    public boolean succeeded() {
        return status == ToolInvocationStatus.SUCCESS;
    }
}
