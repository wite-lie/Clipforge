package com.clipforge.prompt.compress;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PromptCompressionProperties（提示词压缩配置）。
 *
 * <p>该配置绑定 `clipforge.prompt.compression`，用于控制本次调用前动态上下文的规则裁剪上限。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.prompt.compression")
public class PromptCompressionProperties {

    /** enabled（是否启用）表示是否允许 Prompt（提示词）压缩策略生效。 */
    private boolean enabled = true;

    /** appendTruncationMarker（是否追加裁剪标记）表示截断后是否明确告诉模型内容已省略。 */
    private boolean appendTruncationMarker = true;

    /** sectionCharLimits（片段字符限制）表示每个诊断片段允许保留的最大字符数。 */
    private Map<String, Integer> sectionCharLimits = defaultSectionCharLimits();

    /**
     * defaultSectionCharLimits（默认片段字符上限）用于在配置文件缺省时仍然具备基础裁剪能力。
     *
     * @return 默认片段字符上限
     */
    private Map<String, Integer> defaultSectionCharLimits() {
        Map<String, Integer> limits = new LinkedHashMap<>();
        limits.put("rag-context", 6000);
        limits.put("image-summary", 5000);
        limits.put("video-summary", 5000);
        limits.put("memory-context", 2500);
        limits.put("conversation-history", 3000);
        return limits;
    }
}
