package com.clipforge.creation.model;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AgentPlan（智能体计划）。
 *
 * <p>该对象描述一次 Agent（智能体）准备执行的步骤列表。
 */
@Getter
@RequiredArgsConstructor
public class AgentPlan {

    /** planId（计划编号）用于唯一标识一份执行计划。 */
    private final String planId;

    /** goal（计划目标）描述这份计划要完成的任务。 */
    private final String goal;

    /** steps（步骤列表）保存执行计划中的有序步骤。 */
    private final List<AgentStep> steps;
}
