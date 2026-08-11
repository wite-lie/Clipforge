package com.clipforge.tool.definition;

import java.util.List;

/**
 * ToolDefinitionProvider（工具定义提供器）。
 *
 * <p>该接口用于由各业务模块声明自己的工具元数据，让 ToolRegistry（工具注册表）不需要硬编码所有工具。
 */
public interface ToolDefinitionProvider {

    /**
     * definitions（工具定义列表）返回当前模块提供的工具定义。
     *
     * @return 工具定义列表
     */
    List<ToolDefinition> definitions();
}
