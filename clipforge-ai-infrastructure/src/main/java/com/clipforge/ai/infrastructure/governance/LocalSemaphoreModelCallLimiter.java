package com.clipforge.ai.infrastructure.governance;
import com.clipforge.ai.api.routing.ModelCallSupplier;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LocalSemaphoreModelCallLimiter（本机信号量模型调用限流器）用于第一版模型并发保护。
 *
 * <p>当前只保护单个 JVM（Java 虚拟机）进程内的并发，不做分布式限流。后续多实例部署时，可在该接口后增加 Redis（缓存）或网关级限流实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalSemaphoreModelCallLimiter implements ModelCallLimiter {

    /** properties（配置）保存不同模型调用类型的并发上限和等待超时时间。 */
    private final ModelGovernanceProperties properties;

    /** semaphores（信号量集合）按模型调用类型缓存本机并发许可。 */
    private final Map<ModelCallType, Semaphore> semaphores = new ConcurrentHashMap<>();

    /**
     * execute（执行）先获取信号量许可，再执行真实模型调用。
     *
     * @param callType 模型调用类型
     * @param bizId 业务编号
     * @param supplier 真正的模型调用
     * @return 模型调用结果
     * @param <T> 返回类型
     */
    @Override
    public <T> T execute(ModelCallType callType, String bizId, ModelCallSupplier<T> supplier) {
        // 未启用模型治理时直接放行，便于本地调试或压测对比。
        if (!properties.isEnabled()) {
            return executeWithoutLimit(supplier);
        }
        // 调用类型为空时归入聊天生成池，避免调用方漏传类型导致空指针异常。
        ModelCallType safeCallType = callType == null ? ModelCallType.CHAT_GENERATION : callType;
        // 读取当前调用类型的信号量。
        Semaphore semaphore = semaphoreOf(safeCallType);
        // 读取等待超时时间。
        long timeoutMillis = properties.timeoutMillisOf(safeCallType);
        // 标记是否已经拿到许可，确保 finally 中只释放自己拿到的许可。
        boolean acquired = false;
        try {
            // 并发满时最多等待配置的毫秒数。
            acquired = semaphore.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
            // 等待超时则抛出业务异常，让上层明确知道是限流保护触发。
            if (!acquired) {
                throw new BusinessException(ErrorCode.MODEL_CALL_ERROR,
                        "模型调用并发已满，等待超时，callType：" + safeCallType + "，bizId：" + defaultText(bizId));
            }
            // 记录调试日志，默认由日志级别控制，不污染模型测试输出。
            log.debug("模型调用获得并发许可，callType：{}，bizId：{}，availablePermits：{}",
                    safeCallType, defaultText(bizId), semaphore.availablePermits());
            // 执行真正模型调用。
            return supplier.get();
        } catch (BusinessException exception) {
            // 业务异常直接抛出，保留原始错误码。
            throw exception;
        } catch (RuntimeException exception) {
            // 运行时异常保持原样抛出，避免隐藏调用方真正抛出的异常类型。
            throw exception;
        } catch (InterruptedException exception) {
            // 恢复线程中断标记，避免吞掉上层取消信号。
            Thread.currentThread().interrupt();
            // 包装成业务异常，便于统一异常处理。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR,
                    "模型调用等待并发许可时被中断，callType：" + safeCallType + "，bizId：" + defaultText(bizId), exception);
        } catch (Exception exception) {
            // 受检异常统一包装成业务异常，避免把 throws Exception 传染给所有调用方和测试。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR,
                    "模型调用失败，callType：" + safeCallType + "，bizId：" + defaultText(bizId) + "，原因：" + exception.getMessage(), exception);
        } finally {
            // 只有已经获取许可时才释放，避免错误增加许可数。
            if (acquired) {
                semaphore.release();
                log.debug("模型调用释放并发许可，callType：{}，bizId：{}，availablePermits：{}",
                        safeCallType, defaultText(bizId), semaphore.availablePermits());
            }
        }
    }

    /**
     * executeWithoutLimit（无限流执行）用于关闭模型治理时直接运行真实调用。
     *
     * @param supplier 真正的模型调用
     * @return 模型调用结果
     * @param <T> 返回类型
     */
    private <T> T executeWithoutLimit(ModelCallSupplier<T> supplier) {
        try {
            // 直接执行真实调用。
            return supplier.get();
        } catch (BusinessException exception) {
            // 业务异常直接抛出。
            throw exception;
        } catch (RuntimeException exception) {
            // 运行时异常保持原样抛出。
            throw exception;
        } catch (Exception exception) {
            // 其他异常包装为模型调用错误。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "模型调用失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * semaphoreOf（读取信号量）按模型调用类型懒加载信号量。
     *
     * @param callType 模型调用类型
     * @return 信号量
     */
    private Semaphore semaphoreOf(ModelCallType callType) {
        return semaphores.computeIfAbsent(callType, type -> new Semaphore(properties.maxConcurrentOf(type)));
    }

    /**
     * defaultText（默认文本）用于日志里显示空业务编号。
     *
     * @param value 原始文本
     * @return 非空文本
     */
    private String defaultText(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
