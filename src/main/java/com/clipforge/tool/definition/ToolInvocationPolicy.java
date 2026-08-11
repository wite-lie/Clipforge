package com.clipforge.tool.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ToolInvocationPolicy（工具调用策略）。
 *
 * <p>该对象描述工具调用的工程约束，包括是否允许 LLM（大语言模型）调用、超时时间、重试次数和降级说明。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolInvocationPolicy {

    /** llmCallable（是否允许 LLM 调用）表示该工具是否可以被 LangChain4j Function Calling（函数调用）适配器暴露。 */
    private boolean llmCallable;

    /** mcpCallable（是否允许 MCP 调用）表示该工具是否可以被 MCP（模型上下文协议）适配器暴露。 */
    private boolean mcpCallable;

    /** timeoutMillis（超时时间毫秒）表示工具一次同步调用的建议超时。 */
    private long timeoutMillis;

    /** maxRetries（最大重试次数）表示工具失败后的同步重试次数。 */
    private int maxRetries;

    /** fallbackDescription（降级说明）描述工具失败后调用方应该怎么处理。 */
    private String fallbackDescription;

    /**
     * readOnlyLlmCallable（只读可模型调用策略）用于查询型工具。
     *
     * @return 工具调用策略
     */
    public static ToolInvocationPolicy readOnlyLlmCallable() {
        return ToolInvocationPolicy.builder()
                .llmCallable(true)
                .mcpCallable(true)
                .timeoutMillis(5000)
                .maxRetries(0)
                .fallbackDescription("工具失败时返回空结果或让模型保守回答")
                .build();
    }

    /**
     * internalOnly（内部工具策略）用于 Workflow（工作流）内部工具。
     *
     * @return 工具调用策略
     */
    public static ToolInvocationPolicy internalOnly() {
        return ToolInvocationPolicy.builder()
                .llmCallable(false)
                .mcpCallable(false)
                .timeoutMillis(10000)
                .maxRetries(0)
                .fallbackDescription("工具失败时由工作流决定是否降级或终止")
                .build();
    }
}
