package com.clipforge.tool.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ToolInvocationRequest（工具调用请求）。
 *
 * <p>该对象承载一次工具调用所需的运行编号、工具名称、输入摘要和实际执行动作。
 *
 * @param <T> 工具输出类型
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolInvocationRequest<T> {

    /** runId（运行编号）用于把工具调用写入 Trace（执行链路追踪）。 */
    private String runId;

    /** toolName（工具名称）用于从 ToolRegistry（工具注册表）读取定义。 */
    private String toolName;

    /** inputSummary（输入摘要）用于 Trace 记录，不保存大对象原文。 */
    private String inputSummary;

    /** action（工具动作）表示真正要执行的工具调用。 */
    private ToolAction<T> action;
}
