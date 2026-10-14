package com.clipforge.video.understanding;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * VideoFrameUnderstandingParallelProperties（视频帧理解并行配置）用于控制单个视频内部多帧 VLM（视觉语言模型）理解并发。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.video.frame-understanding")
public class VideoFrameUnderstandingParallelProperties {

    /** enabled（是否启用）为 true 时，单个视频的多个关键帧会并行进入图片理解处理服务。 */
    private boolean enabled = true;

    /** maxConcurrent（最大并发数）表示单个视频内部最多同时处理多少个关键帧。 */
    private int maxConcurrent = 3;

    /** timeoutSeconds（等待超时秒数）表示单个视频等待所有关键帧理解完成的最长时间。 */
    private long timeoutSeconds = 180L;
}
