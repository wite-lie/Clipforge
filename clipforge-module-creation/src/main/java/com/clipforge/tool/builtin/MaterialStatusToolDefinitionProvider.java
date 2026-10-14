package com.clipforge.tool.builtin;

import com.clipforge.tool.definition.ToolDefinition;
import com.clipforge.tool.definition.ToolDefinitionProvider;
import com.clipforge.tool.definition.ToolEffectType;
import com.clipforge.tool.definition.ToolInvocationPolicy;
import com.clipforge.tool.definition.ToolSchema;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * MaterialStatusToolDefinitionProvider（素材状态工具定义提供器）。
 */
@Component
public class MaterialStatusToolDefinitionProvider implements ToolDefinitionProvider {

    @Override
    public List<ToolDefinition> definitions() {
        return List.of(ToolDefinition.builder()
                .name("material_status_query")
                .displayName("素材处理状态查询")
                .description("查询图片、视频等素材的上传和后台处理状态")
                .effectType(ToolEffectType.READ_ONLY)
                .inputSchema(ToolSchema.of("materialId/processTaskId", "素材编号或处理任务编号"))
                .outputSchema(ToolSchema.of("TaskStatusResponse", "任务状态、处理阶段、进度、失败原因和摘要预览"))
                .invocationPolicy(ToolInvocationPolicy.readOnlyLlmCallable())
                .ownerModule("material")
                .version("v1")
                .build());
    }
}
