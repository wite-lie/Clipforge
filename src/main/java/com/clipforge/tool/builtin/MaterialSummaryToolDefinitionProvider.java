package com.clipforge.tool.builtin;

import com.clipforge.material.MaterialSummary;
import com.clipforge.model.vision.ImageUnderstandingResult;
import com.clipforge.tool.definition.ToolDefinition;
import com.clipforge.tool.definition.ToolDefinitionProvider;
import com.clipforge.tool.definition.ToolEffectType;
import com.clipforge.tool.definition.ToolInvocationPolicy;
import com.clipforge.tool.definition.ToolSchema;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * MaterialSummaryToolDefinitionProvider（素材摘要工具定义提供器）。
 */
@Component
public class MaterialSummaryToolDefinitionProvider implements ToolDefinitionProvider {

    @Override
    public List<ToolDefinition> definitions() {
        return List.of(ToolDefinition.builder()
                .name("material_summary")
                .displayName("素材摘要")
                .description("把多张图片理解结果合并为创作规划可直接使用的素材摘要")
                .effectType(ToolEffectType.INTERNAL)
                .inputSchema(ToolSchema.of(List.class.getName() + "<" + ImageUnderstandingResult.class.getSimpleName() + ">", "图片理解结果列表"))
                .outputSchema(ToolSchema.of(MaterialSummary.class.getName(), "聚合后的素材摘要"))
                .invocationPolicy(ToolInvocationPolicy.internalOnly())
                .ownerModule("material")
                .version("v1")
                .build());
    }
}
