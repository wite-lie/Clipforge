package com.clipforge.application.schedule;

import com.clipforge.application.KnowledgeDocumentScheduleApplicationService;
import com.clipforge.knowledge.sync.KnowledgeDocumentUrlSyncProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * KnowledgeDocumentScheduleJob（知识文档定时任务）。
 *
 * <p>该任务负责按照 Cron（定时表达式）周期扫描并执行到期的 URL（统一资源定位符）同步任务。
 */
@Component
@Profile("db")
@RequiredArgsConstructor
public class KnowledgeDocumentScheduleJob {

    /** scheduleApplicationService（定时同步应用服务）负责真正执行调度逻辑。 */
    private final KnowledgeDocumentScheduleApplicationService scheduleApplicationService;

    /** properties（同步配置）提供扫描开关和 Cron（定时表达式）。 */
    private final KnowledgeDocumentUrlSyncProperties properties;

    /**
     * execute（执行）用于定时扫描到期任务。
     */
    @Scheduled(cron = "${clipforge.rag.url-sync.scan-cron:0 */5 * * * ?}")
    public void execute() {
        if (!properties.isEnabled()) {
            return;
        }
        scheduleApplicationService.executeDueSchedules();
    }
}
