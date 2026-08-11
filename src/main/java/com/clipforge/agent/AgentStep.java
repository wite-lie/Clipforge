package com.clipforge.agent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AgentStep（智能体步骤）。
 *
 * <p>该对象描述 AgentPlan（智能体计划）中的一个具体执行步骤。
 */
@Getter
@RequiredArgsConstructor
public class AgentStep {

    /** name（步骤名称）用于标识步骤。 */
    private final String name;

    /** description（步骤描述）用于解释该步骤要完成的事情。 */
    private final String description;
}
