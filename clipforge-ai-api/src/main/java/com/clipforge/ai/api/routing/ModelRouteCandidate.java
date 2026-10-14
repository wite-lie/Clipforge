package com.clipforge.ai.api.routing;

import com.clipforge.ai.api.routing.ModelCallSupplier;
import lombok.Builder;
import lombok.Getter;

/**
 * ModelRouteCandidate（模型路由候选）。
 *
 * <p>该对象描述一次模型调用可以尝试的单个候选实现，例如百炼真实模型、OpenAI-Compatible（OpenAI 兼容）
 * 模型或 Mock（模拟）模型。
 *
 * @param <T> 模型调用返回类型
 */
@Getter
@Builder
public class ModelRouteCandidate<T> {

    /** candidateName（候选名称）用于日志、Trace（执行链路追踪）和健康状态定位。 */
    private final String candidateName;

    /** provider（供应商）用于区分 bailian、langchain4j、mock 等模型来源。 */
    private final String provider;

    /** modelName（模型名称）用于成本复盘和故障转移路径展示。 */
    private final String modelName;

    /** mockCandidate（是否为模拟候选）用于控制是否允许降级到 Mock（模拟）模型。 */
    private final boolean mockCandidate;

    /** supplier（调用供应器）封装真正的模型调用动作。 */
    private final ModelCallSupplier<T> supplier;
}
