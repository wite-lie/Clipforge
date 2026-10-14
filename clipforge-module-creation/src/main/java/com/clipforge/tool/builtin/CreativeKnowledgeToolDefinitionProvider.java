package com.clipforge.tool.builtin;

import com.clipforge.creation.model.AgentContext;
import com.clipforge.creation.knowledge.CreativeKnowledge;
import com.clipforge.tool.definition.ToolDefinition;
import com.clipforge.tool.definition.ToolDefinitionProvider;
import com.clipforge.tool.definition.ToolEffectType;
import com.clipforge.tool.definition.ToolInvocationPolicy;
import com.clipforge.tool.definition.ToolSchema;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * CreativeKnowledgeToolDefinitionProvider（创作知识工具定义提供器）。
 */
@Component
public class CreativeKnowledgeToolDefinitionProvider implements ToolDefinitionProvider {

    @Override
    public List<ToolDefinition> definitions() {
        return List.of(ToolDefinition.builder()
                .name("creative_knowledge_retrieve")
                .displayName("创作知识检索")
                .description("根据目标平台检索平台规则、脚本结构和评估提示")
                .effectType(ToolEffectType.READ_ONLY)
                .inputSchema(ToolSchema.of(AgentContext.class.getName(), "包含创作目标、平台、风格偏好和运行编号的智能体上下文"))
                .outputSchema(ToolSchema.of(CreativeKnowledge.class.getName(), "平台规则、脚本知识和评估提示"))
                .invocationPolicy(ToolInvocationPolicy.readOnlyLlmCallable())
                .ownerModule("creation")
                .version("v1")
                .build());
    }
}
