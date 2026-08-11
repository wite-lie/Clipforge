package com.clipforge.tool.builtin;

import com.clipforge.model.vision.VisionModelRequest;
import com.clipforge.model.vision.VisionModelResponse;
import com.clipforge.tool.definition.ToolDefinition;
import com.clipforge.tool.definition.ToolDefinitionProvider;
import com.clipforge.tool.definition.ToolEffectType;
import com.clipforge.tool.definition.ToolInvocationPolicy;
import com.clipforge.tool.definition.ToolSchema;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ImageUnderstandingToolDefinitionProvider（图片理解工具定义提供器）。
 */
@Component
public class ImageUnderstandingToolDefinitionProvider implements ToolDefinitionProvider {

    @Override
    public List<ToolDefinition> definitions() {
        return List.of(ToolDefinition.builder()
                .name("image_understanding")
                .displayName("图片理解")
                .description("调用 VLM（视觉语言模型）理解图片素材，输出主体、场景、风格、关键元素和风险提示")
                .effectType(ToolEffectType.COSTLY)
                .inputSchema(ToolSchema.of(VisionModelRequest.class.getName(), "图片素材元数据和理解指令"))
                .outputSchema(ToolSchema.of(VisionModelResponse.class.getName(), "结构化图片理解结果和模型用量"))
                .invocationPolicy(ToolInvocationPolicy.internalOnly())
                .ownerModule("material")
                .version("v1")
                .build());
    }
}
