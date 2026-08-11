package com.clipforge.api.dto;

import com.clipforge.agent.CreationPlanResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * QueryPlanResponse（查询创作方案响应）。
 *
 * <p>该对象用于返回已经持久化到数据库中的创作方案结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryPlanResponse {

    /** runId（运行编号）用于唯一标识一次 Agent（智能体）执行。 */
    private String runId;

    /** taskId（任务编号）用于标识本次创作任务。 */
    private String taskId;

    /** resultId（结果编号）用于标识本次创作方案结果。 */
    private String resultId;

    /** result（创作方案结果）承载从数据库还原出来的结构化创作方案。 */
    private CreationPlanResult result;
}
