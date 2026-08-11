package com.clipforge.tool.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ToolDefinition（工具定义）。
 *
 * <p>该对象是统一工具体系的核心元数据，描述一个工具叫什么、做什么、输入输出是什么、是否有副作用以及能否暴露给 LLM（大语言模型）或 MCP（模型上下文协议）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {

    /** name（工具名称）是工具注册表中的唯一键。 */
    private String name;

    /** displayName（展示名称）用于文档和前端展示。 */
    private String displayName;

    /** description（描述）说明工具解决什么问题。 */
    private String description;

    /** effectType（副作用类型）用于判断工具调用风险。 */
    private ToolEffectType effectType;

    /** inputSchema（输入结构）描述工具输入。 */
    private ToolSchema inputSchema;

    /** outputSchema（输出结构）描述工具输出。 */
    private ToolSchema outputSchema;

    /** invocationPolicy（调用策略）描述超时、重试、LLM 调用和 MCP 调用策略。 */
    private ToolInvocationPolicy invocationPolicy;

    /** ownerModule（所属模块）标识工具主要归属，例如 RAG、Material、Video。 */
    private String ownerModule;

    /** version（版本）用于后续工具协议演进。 */
    private String version;

    /**
     * llmCallable（是否允许 LLM 调用）用于注册表快速过滤。
     *
     * @return 是否允许 LLM 调用
     */
    public boolean llmCallable() {
        return invocationPolicy != null && invocationPolicy.isLlmCallable();
    }
}
