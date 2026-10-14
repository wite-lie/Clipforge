package com.clipforge.creation.application.api;

import com.clipforge.contract.creation.CreatePlanRequest;
import com.clipforge.contract.knowledge.CreatePlanResponse;

/**
 * CreationPlanUseCase（创作方案生成用例）。
 *
 * <p>该接口是创作模块对 Web（网络接口）层暴露的稳定入口，具体 Agent（智能体）编排仍由实现侧决定。
 */
public interface CreationPlanUseCase {

    /**
     * createPlan（创建创作方案）用于根据用户目标生成结构化创作方案。
     *
     * @param request 创建方案请求
     * @return 创建方案响应
     */
    CreatePlanResponse createPlan(CreatePlanRequest request);
}
