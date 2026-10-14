package com.clipforge.tool.registry;

import com.clipforge.tool.definition.ToolDefinition;
import java.util.List;
import java.util.Optional;

/**
 * ToolRegistry（工具注册表）。
 *
 * <p>该接口统一管理系统内工具定义，为 Workflow（工作流）、LangChain4j（LangChain 的 Java 封装）工具适配器和 MCP（模型上下文协议）适配器提供同一份工具清单。
 */
public interface ToolRegistry {

    /**
     * findRegistration（查找工具注册项）用于按名称查找工具实例和定义。
     *
     * @param toolName 工具名称
     * @return 工具注册项
     */
    Optional<ToolRegistration> findRegistration(String toolName);

    /**
     * findDefinition（查找工具定义）用于按名称查找工具元数据。
     *
     * @param toolName 工具名称
     * @return 工具定义
     */
    Optional<ToolDefinition> findDefinition(String toolName);

    /**
     * listDefinitions（列出工具定义）用于返回全部已注册工具。
     *
     * @return 工具定义列表
     */
    List<ToolDefinition> listDefinitions();

    /**
     * listLlmCallableDefinitions（列出可供 LLM 调用的工具定义）用于 Function Calling（函数调用）适配。
     *
     * @return 可供 LLM 调用的工具定义列表
     */
    List<ToolDefinition> listLlmCallableDefinitions();
}
