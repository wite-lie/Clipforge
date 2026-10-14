package com.clipforge.material.sampling;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MaterialUnderstandingSamplingProperties（素材理解抽样配置）用于控制图片和视频帧进入 VLM（视觉语言模型）前的数量上限。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.material.understanding-sampling")
public class MaterialUnderstandingSamplingProperties {

    /** enabled（是否启用）为 true 时，会在素材理解前执行抽样策略。 */
    private boolean enabled = true;

    /** maxImagesPerUploadBatch（单次上传批次最大自动理解图片数）用于后续批量图片上传时控制成本。 */
    private int maxImagesPerUploadBatch = 12;

    /** maxVideoFramesPerVideo（单个视频最大理解帧数）用于避免视频帧过多导致 VLM（视觉语言模型）调用过慢。 */
    private int maxVideoFramesPerVideo = 6;

    /** preferEvenlyDistributedVideoFrames（是否优先均匀选择视频帧）用于让被理解的帧尽量覆盖视频前中后段。 */
    private boolean preferEvenlyDistributedVideoFrames = true;

    /** skipUnsupportedMaterials（是否跳过不支持的素材）用于后续扩展批量素材理解时控制异常分支。 */
    private boolean skipUnsupportedMaterials = true;
}
