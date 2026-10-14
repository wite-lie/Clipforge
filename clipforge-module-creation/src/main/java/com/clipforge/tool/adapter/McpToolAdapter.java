package com.clipforge.tool.adapter;

import com.clipforge.tool.definition.ToolDefinition;
import com.clipforge.tool.definition.ToolEffectType;
import com.clipforge.tool.registry.ToolRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * McpToolAdapter（MCP 工具适配器）。
 *
 * <p>该类是 MCP（模型上下文协议）阶段的预留点，用于复用 ToolRegistry（工具注册表）中的工具定义，而不是为 MCP 重新维护一套工具清单。
 */
@Component
@RequiredArgsConstructor
public class McpToolAdapter {

    /** toolRegistry（工具注册表）提供统一工具清单。 */
    private final ToolRegistry toolRegistry;

    /**
     * listMcpCandidateDefinitions（列出 MCP 候选工具）用于后续 MCP Server（模型上下文协议服务端）发布工具。
     *
     * @return MCP 候选工具定义列表
     */
    public List<ToolDefinition> listMcpCandidateDefinitions() {
        return toolRegistry.listDefinitions()
                .stream()
                .filter(definition -> definition.getEffectType() == ToolEffectType.READ_ONLY)
                .filter(definition -> definition.getInvocationPolicy() != null && definition.getInvocationPolicy().isMcpCallable())
                .toList();
    }
}
