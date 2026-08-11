package com.clipforge.model.routing;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * ModelRouteRequest（模型路由请求）。
 *
 * <p>该对象把一次模型调用所需的能力、业务编号、Trace（执行链路追踪）编号和候选模型列表交给路由执行器。
 *
 * @param <T> 模型调用返回类型
 */
@Getter
@Builder
public class ModelRouteRequest<T> {

    /** capability（模型能力）用于匹配路由规则和健康状态。 */
    private final ModelCapability capability;

    /** purpose（调用用途）用于日志和后续按用途细分策略。 */
    private final String purpose;

    /** bizId（业务编号）通常是 runId、materialId、documentId 或 query hash。 */
    private final String bizId;

    /** traceRunId（执行链路编号）存在时会写入 Trace（执行链路追踪）。 */
    private final String traceRunId;

    /** candidates（候选列表）按默认优先级传入，路由器会再根据配置调整顺序。 */
    private final List<ModelRouteCandidate<T>> candidates;
}
