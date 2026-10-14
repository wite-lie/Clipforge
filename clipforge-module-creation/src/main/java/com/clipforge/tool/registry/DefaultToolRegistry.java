package com.clipforge.tool.registry;

import com.clipforge.tool.Tool;
import com.clipforge.tool.definition.ToolDefinition;
import com.clipforge.tool.definition.ToolDefinitionProvider;
import com.clipforge.tool.definition.ToolEffectType;
import com.clipforge.tool.definition.ToolInvocationPolicy;
import com.clipforge.tool.definition.ToolSchema;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * DefaultToolRegistry（默认工具注册表）。
 *
 * <p>该实现从 Spring（Spring 框架）容器收集 Tool（工具）实例和 ToolDefinitionProvider（工具定义提供器），形成统一工具清单。
 */
@Component
public class DefaultToolRegistry implements ToolRegistry {

    /** registrations（注册项）按工具名称保存工具定义和可选实例。 */
    private final Map<String, ToolRegistration> registrations;

    public DefaultToolRegistry(List<Tool> tools, List<ToolDefinitionProvider> definitionProviders) {
        Map<String, ToolRegistration> collected = new LinkedHashMap<>();
        registerToolBeans(collected, tools);
        registerProvidedDefinitions(collected, definitionProviders);
        this.registrations = Map.copyOf(collected);
    }

    @Override
    public Optional<ToolRegistration> findRegistration(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(registrations.get(toolName));
    }

    @Override
    public Optional<ToolDefinition> findDefinition(String toolName) {
        return findRegistration(toolName).map(ToolRegistration::getDefinition);
    }

    @Override
    public List<ToolDefinition> listDefinitions() {
        return registrations.values()
                .stream()
                .map(ToolRegistration::getDefinition)
                .sorted(Comparator.comparing(ToolDefinition::getName))
                .toList();
    }

    @Override
    public List<ToolDefinition> listLlmCallableDefinitions() {
        return listDefinitions().stream()
                .filter(ToolDefinition::llmCallable)
                .toList();
    }

    private void registerToolBeans(Map<String, ToolRegistration> collected, List<Tool> tools) {
        if (tools == null || tools.isEmpty()) {
            return;
        }
        for (Tool tool : tools) {
            if (tool == null || !hasText(tool.name())) {
                continue;
            }
            ToolDefinition definition = defaultDefinition(tool);
            collected.put(tool.name(), ToolRegistration.builder()
                    .definition(definition)
                    .tool(tool)
                    .build());
        }
    }

    private void registerProvidedDefinitions(Map<String, ToolRegistration> collected, List<ToolDefinitionProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return;
        }
        for (ToolDefinitionProvider provider : providers) {
            if (provider == null || provider.definitions() == null) {
                continue;
            }
            for (ToolDefinition definition : provider.definitions()) {
                if (definition == null || !hasText(definition.getName())) {
                    continue;
                }
                ToolRegistration existing = collected.get(definition.getName());
                collected.put(definition.getName(), ToolRegistration.builder()
                        .definition(definition)
                        .tool(existing == null ? null : existing.getTool())
                        .build());
            }
        }
    }

    private ToolDefinition defaultDefinition(Tool tool) {
        return ToolDefinition.builder()
                .name(tool.name())
                .displayName(tool.name())
                .description(tool.description())
                .effectType(ToolEffectType.INTERNAL)
                .inputSchema(ToolSchema.of("unknown", "未声明输入结构"))
                .outputSchema(ToolSchema.of("unknown", "未声明输出结构"))
                .invocationPolicy(ToolInvocationPolicy.internalOnly())
                .ownerModule("unknown")
                .version("v1")
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
