package com.clipforge.rag.recovery;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * KnowledgeDocumentRecoveryProperties（知识文档恢复配置）。
 *
 * <p>该配置用于集中管理 RAG（检索增强生成）文档处理超时恢复和人工重试策略。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.rag.recovery")
public class KnowledgeDocumentRecoveryProperties {

    /** processingTimeoutMinutes（处理中超时分钟数）表示任务运行超过多久可以被判定为卡住。 */
    private long processingTimeoutMinutes = 30L;

    /** maxRetryTimes（最大重试次数）表示同一个文档允许人工或系统重试的最大次数。 */
    private int maxRetryTimes = 3;

    /** stuckScanLimit（卡住任务扫描上限）用于避免一次扫描更新过多任务。 */
    private int stuckScanLimit = 50;
}
