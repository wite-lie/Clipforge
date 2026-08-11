package com.clipforge.rag.process;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * KnowledgeDocumentProcessStatusCacheProperties（知识文档处理状态缓存配置）。
 *
 * <p>该配置用于控制 Redis（缓存）中 RAG（检索增强生成）文档处理状态的 Key（键）前缀和 TTL（生存时间）。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.rag.process-status-cache")
public class KnowledgeDocumentProcessStatusCacheProperties {

    /** keyPrefix（键前缀）用于隔离知识文档处理状态缓存。 */
    private String keyPrefix = "clipforge:rag:document-process";

    /** runningTtl（运行中状态生存时间）用于防止运行中状态永久残留。 */
    private Duration runningTtl = Duration.ofHours(6);

    /** successTtl（成功状态生存时间）用于短期保留成功处理结果。 */
    private Duration successTtl = Duration.ofHours(24);

    /** failedTtl（失败状态生存时间）用于较长时间保留失败原因。 */
    private Duration failedTtl = Duration.ofHours(72);
}
