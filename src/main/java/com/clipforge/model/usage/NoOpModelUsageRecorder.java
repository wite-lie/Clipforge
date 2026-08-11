package com.clipforge.model.usage;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * NoOpModelUsageRecorder（空模型用量记录器）。
 *
 * <p>该实现只在没有启用 db（数据库）Profile（配置环境）时生效，保证本地默认启动不依赖数据库。
 */
@Component
@Profile("!db")
public class NoOpModelUsageRecorder implements ModelUsageRecorder {

    /**
     * record（记录）在默认环境下不执行任何持久化操作。
     *
     * @param record 模型调用记录
     */
    @Override
    public void record(ModelCallRecord record) {
        // 默认环境不写数据库，保持最小运行链路轻量。
    }
}
