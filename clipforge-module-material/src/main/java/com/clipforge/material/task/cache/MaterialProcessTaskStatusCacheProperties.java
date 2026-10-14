package com.clipforge.material.task.cache;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MaterialProcessTaskStatusCacheProperties（素材处理任务状态缓存配置）。
 *
 * <p>该配置用于控制 Redis（缓存）中素材处理任务状态的 Key（键）前缀和 TTL（生存时间）。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.task.material-process-status-cache")
public class MaterialProcessTaskStatusCacheProperties {

    /** enabled（是否启用）表示是否使用 Redis（缓存）保存素材处理任务状态。 */
    private boolean enabled = true;

    /** keyPrefix（键前缀）用于隔离素材处理任务状态缓存。 */
    private String keyPrefix = "clipforge:material-process-task";

    /** pendingTtl（等待状态生存时间）用于保存等待处理任务状态。 */
    private Duration pendingTtl = Duration.ofHours(6);

    /** processingTtl（处理中状态生存时间）用于防止运行中状态永久残留。 */
    private Duration processingTtl = Duration.ofHours(6);

    /** successTtl（成功状态生存时间）用于短期保留成功处理结果。 */
    private Duration successTtl = Duration.ofHours(24);

    /** failedTtl（失败状态生存时间）用于较长时间保留失败原因。 */
    private Duration failedTtl = Duration.ofHours(72);
}
