package com.clipforge.ai.api.routing;

import lombok.Builder;
import lombok.Getter;

/**
 * ModelRouteAttempt（模型路由尝试记录）。
 *
 * <p>该对象记录一次候选模型尝试是否成功、耗时和失败原因，用于故障转移复盘。
 */
@Getter
@Builder
public class ModelRouteAttempt {

    private final String candidateName;
    private final String provider;
    private final String modelName;
    private final boolean success;
    private final long latencyMs;
    private final String failureReason;
}
