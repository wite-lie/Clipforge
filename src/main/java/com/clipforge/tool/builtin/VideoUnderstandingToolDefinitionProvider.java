package com.clipforge.tool.builtin;

import com.clipforge.tool.definition.ToolDefinition;
import com.clipforge.tool.definition.ToolDefinitionProvider;
import com.clipforge.tool.definition.ToolEffectType;
import com.clipforge.tool.definition.ToolInvocationPolicy;
import com.clipforge.tool.definition.ToolSchema;
import com.clipforge.video.understanding.VideoUnderstandingCreationContext;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * VideoUnderstandingToolDefinitionProvider（视频理解读取工具定义提供器）。
 */
@Component
public class VideoUnderstandingToolDefinitionProvider implements ToolDefinitionProvider {

    @Override
    public List<ToolDefinition> definitions() {
        return List.of(ToolDefinition.builder()
                .name("video_understanding_read")
                .displayName("视频理解读取")
                .description("读取已经完成处理的视频理解摘要，供创作工作流注入上下文")
                .effectType(ToolEffectType.READ_ONLY)
                .inputSchema(ToolSchema.of("List<String>", "视频素材编号列表"))
                .outputSchema(ToolSchema.of(VideoUnderstandingCreationContext.class.getName(), "可注入创作方案的视频理解上下文"))
                .invocationPolicy(ToolInvocationPolicy.readOnlyLlmCallable())
                .ownerModule("video")
                .version("v1")
                .build());
    }
}
