package com.clipforge.tool;

/**
 * Tool（工具）。
 *
 * <p>该接口表示 Agent（智能体）可以调用的受控外部能力，例如知识检索、素材分析或后续 MCP（模型上下文协议）工具。
 */
public interface Tool {

    /**
     * name（工具名称）用于让 Workflow（工作流）或后续 LLM（大语言模型）识别工具。
     *
     * @return 工具名称
     */
    String name();

    /**
     * description（工具描述）用于说明工具能解决什么问题。
     *
     * @return 工具描述
     */
    String description();
}

