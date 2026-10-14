package com.clipforge.ai.api.routing;

/**
 * ModelCallSupplier（模型调用供应器）封装真正的模型请求。
 *
 * <p>它允许抛出受检异常，方便 HTTP（超文本传输协议）和 LangChain4j（LangChain 的 Java 封装）调用直接接入限流器。
 *
 * @param <T> 模型调用返回类型
 */
@FunctionalInterface
public interface ModelCallSupplier<T> {

    /**
     * get（获取结果）执行真正的模型调用。
     *
     * @return 模型调用结果
     * @throws Exception 模型供应商、网络或解析异常
     */
    T get() throws Exception;
}
