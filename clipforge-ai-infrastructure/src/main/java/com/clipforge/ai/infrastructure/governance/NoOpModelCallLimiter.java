package com.clipforge.ai.infrastructure.governance;
import com.clipforge.ai.api.routing.ModelCallSupplier;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;

/**
 * NoOpModelCallLimiter（空操作模型调用限流器）用于兼容手动构造客户端或关闭治理能力的场景。
 */
public class NoOpModelCallLimiter implements ModelCallLimiter {

    /**
     * execute（执行）不做任何限流，直接调用供应器。
     *
     * @param callType 模型调用类型
     * @param bizId 业务编号
     * @param supplier 真正的模型调用
     * @return 模型调用结果
     * @param <T> 返回类型
     */
    @Override
    public <T> T execute(ModelCallType callType, String bizId, ModelCallSupplier<T> supplier) {
        try {
            // 直接执行真实调用，不做并发控制。
            return supplier.get();
        } catch (BusinessException exception) {
            // 业务异常直接抛出，避免重复包装。
            throw exception;
        } catch (RuntimeException exception) {
            // 运行时异常保持原样抛出，便于测试和上层保留原始异常类型。
            throw exception;
        } catch (Exception exception) {
            // 受检异常统一包装成业务异常，避免污染上层方法签名。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "模型调用失败：" + exception.getMessage(), exception);
        }
    }
}
