package com.clipforge.rag.sync;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * KnowledgeDocumentUrlSyncProperties（知识文档 URL 同步配置）。
 *
 * <p>该配置集中管理 URL（统一资源定位符）定时同步的扫描周期、租约时长、抓取超时和安全限制。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.rag.url-sync")
public class KnowledgeDocumentUrlSyncProperties {

    /** enabled（是否启用）表示是否开启 URL 定时同步。 */
    private boolean enabled = false;

    /** dispatchToMq（是否投递 MQ）表示定时扫描抢到租约后是否通过 RocketMQ（消息队列）执行同步。 */
    private boolean dispatchToMq = false;

    /** topic（主题）表示 URL 同步任务投递到哪个 RocketMQ（消息队列）主题。 */
    private String topic = "clipforge-rag-url-sync-topic";

    /** consumerGroup（消费者组）表示消费 URL 同步消息的一组应用实例。 */
    private String consumerGroup = "clipforge-rag-url-sync-consumer-group";

    /** scanCron（扫描 Cron）表示调度器多久扫描一次到期任务。 */
    private String scanCron = "0 */5 * * * ?";

    /** scanBatchSize（扫描批次）表示每次最多处理多少条到期任务。 */
    private int scanBatchSize = 20;

    /** leaseMinutes（租约分钟数）表示单次任务最多占用多久。 */
    private int leaseMinutes = 10;

    /** connectTimeoutMillis（连接超时）表示建立远程连接的最大等待时间。 */
    private int connectTimeoutMillis = 3000;

    /** requestTimeoutMillis（请求超时）表示整次请求的最大等待时间。 */
    private int requestTimeoutMillis = 10000;

    /** maxRedirects（最大重定向数）表示最多跟随多少次重定向。 */
    private int maxRedirects = 3;

    /** maxContentBytes（最大内容字节数）表示允许抓取的最大响应体大小。 */
    private long maxContentBytes = 20L * 1024 * 1024;

    /** blockPrivateNetwork（是否拦截内网）表示是否拦截本地网段和私网地址。 */
    private boolean blockPrivateNetwork = true;

    /** allowedHosts（允许主机）表示可选的 URL 主机白名单。 */
    private List<String> allowedHosts = new ArrayList<>();

    /** storageRootPath（存储根路径）表示远程抓取内容落盘目录。 */
    private String storageRootPath = "data/uploads/knowledge/url-sync";
}
