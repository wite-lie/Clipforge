package com.clipforge.model.usage;

/**
 * ModelUsageRecorder（模型用量记录器）。
 *
 * <p>该接口负责记录模型调用明细，后续用于 Token（词元）成本统计、供应商分析和故障排查。
 */
public interface ModelUsageRecorder {

    /**
     * record（记录）用于保存一次模型调用明细。
     *
     * @param record 模型调用记录
     */
    void record(ModelCallRecord record);
}
