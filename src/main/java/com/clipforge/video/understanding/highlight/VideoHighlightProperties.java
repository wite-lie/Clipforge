package com.clipforge.video.understanding.highlight;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * VideoHighlightProperties（视频高光推荐配置）。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.video.highlight")
public class VideoHighlightProperties {

    /** enabled（是否启用）表示是否生成结构化高光候选。 */
    private boolean enabled = true;

    /** topK（保留数量）表示最多返回多少个高光候选。 */
    private Integer topK = 5;

    /** preWindowSeconds（前置窗口秒数）表示以候选帧为中心向前扩多少秒。 */
    private Double preWindowSeconds = 1.5D;

    /** postWindowSeconds（后置窗口秒数）表示以候选帧为中心向后扩多少秒。 */
    private Double postWindowSeconds = 3.0D;

    /** subtitleOverlapWindowSeconds（字幕重叠窗口秒数）表示帧时间附近多少秒内的字幕可作为证据。 */
    private Double subtitleOverlapWindowSeconds = 2.0D;

    /** minConfidence（最低置信度）表示低于该值的片段不会进入最终结果。 */
    private Double minConfidence = 0.35D;

    /** openingHookSeconds（开头钩子秒数）表示前多少秒内的片段可获得开头钩子加权。 */
    private Double openingHookSeconds = 5.0D;
}
