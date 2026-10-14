package com.clipforge.tool.registry;

import com.clipforge.tool.Tool;
import com.clipforge.tool.definition.ToolDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ToolRegistration（工具注册项）。
 *
 * <p>该对象把 ToolDefinition（工具定义）和可选 Tool（工具实例）绑定在一起。部分查询型工具第一阶段只有定义，执行时由 Workflow（工作流）显式包装动作。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolRegistration {

    /** definition（工具定义）描述工具元数据。 */
    private ToolDefinition definition;

    /** tool（工具实例）表示 Spring（Spring 框架）容器中的具体工具 Bean，可为空。 */
    private Tool tool;

    /**
     * name（工具名称）用于快速读取注册项名称。
     *
     * @return 工具名称
     */
    public String name() {
        return definition == null ? null : definition.getName();
    }
}
