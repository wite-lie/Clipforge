package com.clipforge.creation.application.api;

import com.clipforge.contract.knowledge.QueryPlanResponse;

/**
 * CreationPlanQueryUseCase（创作方案查询用例）。
 *
 * <p>该接口隔离 Controller（控制器）和具体持久化查询实现，保证接口层不直接依赖 legacy（旧实现）应用服务。
 */
public interface CreationPlanQueryUseCase {

    /**
     * queryPlanByRunId（按运行编号查询创作方案）用于读取已经持久化的创作方案。
     *
     * @param runId Agent（智能体）运行编号
     * @return 查询创作方案响应
     */
    QueryPlanResponse queryPlanByRunId(String runId);
}
