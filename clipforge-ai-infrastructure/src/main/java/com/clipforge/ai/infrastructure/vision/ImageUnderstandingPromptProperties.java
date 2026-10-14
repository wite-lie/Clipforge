package com.clipforge.ai.infrastructure.vision;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ImageUnderstandingPromptProperties（图片理解提示词配置属性）。
 *
 * <p>该配置类绑定 `clipforge.prompt.image-understanding`（图片理解提示词配置），用于从文件加载 VLM（视觉语言模型）提示词模板。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.prompt.image-understanding")
public class ImageUnderstandingPromptProperties {

    /** systemPath（系统提示词路径）定义图片理解模型的角色、边界和输出格式。 */
    private String systemPath = "classpath:prompts/图片理解-系统提示词.md";

    /** userPath（用户提示词路径）定义图片理解模型的动态任务指令和素材清单格式。 */
    private String userPath = "classpath:prompts/图片理解-用户提示词.md";

    /** repairSystemPath（修复系统提示词路径）定义图片理解 JSON（对象表示格式）修复角色和边界。 */
    private String repairSystemPath = "classpath:prompts/图片理解修复-系统提示词.md";

    /** repairUserPath（修复用户提示词路径）定义图片理解 JSON（对象表示格式）修复时的错误原因和原始输出。 */
    private String repairUserPath = "classpath:prompts/图片理解修复-用户提示词.md";
}
