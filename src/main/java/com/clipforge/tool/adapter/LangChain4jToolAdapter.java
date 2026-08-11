package com.clipforge.tool.adapter;

import com.clipforge.tool.definition.ToolDefinition;
import com.clipforge.tool.registry.ToolRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LangChain4jToolAdapter（LangChain4j 工具适配器）。
 *
 * <p>该类是 Function Calling（函数调用）接入预留点。当前阶段只负责筛选可安全暴露给 LLM（大语言模型）的工具定义，不直接把所有业务工具交给模型自由调用。
 */
@Component
@RequiredArgsConstructor
public class LangChain4jToolAdapter {

    /** toolRegistry（工具注册表）提供统一工具清单。 */
    private final ToolRegistry toolRegistry;

    /**
     * listCallableToolDefinitions（列出可调用工具定义）用于后续转换为 LangChain4j @Tool（工具注解）或动态工具规格。
     *
     * @return 可供 LLM 调用的工具定义列表
     */
    public List<ToolDefinition> listCallableToolDefinitions() {
        return toolRegistry.listLlmCallableDefinitions();
    }
}
