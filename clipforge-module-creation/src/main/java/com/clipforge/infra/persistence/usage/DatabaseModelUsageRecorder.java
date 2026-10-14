package com.clipforge.infra.persistence.usage;

import com.clipforge.memory.infrastructure.persistence.repository.ModelCallRecordRepository;
import com.clipforge.observability.usage.ModelCallRecord;
import com.clipforge.observability.usage.ModelUsageRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * DatabaseModelUsageRecorder（数据库模型用量记录器）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，负责把模型调用明细保存到 PostgreSQL（关系数据库）。
 */
@Component
@Profile("db")
@RequiredArgsConstructor
public class DatabaseModelUsageRecorder implements ModelUsageRecorder {

    /** modelCallRecordRepository（模型调用记录仓储）负责保存模型调用明细。 */
    private final ModelCallRecordRepository modelCallRecordRepository;

    /**
     * record（记录）用于保存模型调用明细。
     *
     * @param record 模型调用记录
     */
    @Override
    public void record(ModelCallRecord record) {
        // 调用仓储保存模型调用记录。
        modelCallRecordRepository.save(record);
    }
}
