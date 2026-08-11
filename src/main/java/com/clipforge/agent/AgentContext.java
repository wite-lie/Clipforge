package com.clipforge.agent;

import com.clipforge.memory.model.MemoryContext;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AgentContext（智能体上下文）。
 *
 * <p>该对象保存一次 Agent（智能体）运行过程中的输入、状态和中间产物。
 */
@Getter
@RequiredArgsConstructor
public class AgentContext {

    /** runId（运行编号）用于唯一标识一次智能体执行。 */
    private final String runId;

    /** goal（创作目标）表示用户希望生成什么内容方案。 */
    private final String goal;

    /** platform（目标平台）表示创作方案要适配的平台。 */
    private final String platform;

    /** stylePreference（风格偏好）表示用户希望采用的表达风格。 */
    private final String stylePreference;

    /** memoryContext（记忆上下文）保存本次运行读取到的用户偏好、项目记忆、历史摘要和会话摘要。 */
    private MemoryContext memoryContext;

    /** artifacts（中间产物）用于保存工作流每一步生成的数据。 */
    private final Map<String, Object> artifacts = new LinkedHashMap<>();

    /**
     * setMemoryContext（设置记忆上下文）用于把应用层读取到的 MemoryContext（记忆上下文）挂入当前 Agent Run（智能体运行）。
     *
     * @param memoryContext 记忆上下文
     */
    public void setMemoryContext(MemoryContext memoryContext) {
        // 保存强类型记忆上下文，便于后续 Planner（规划器）、Generator（生成器）和 Evaluator（评估器）读取。
        this.memoryContext = memoryContext;
        // 同时写入 artifacts（中间产物），兼容当前项目大量通过中间产物传递上下文的模式。
        artifacts.put("MEMORY_CONTEXT", memoryContext);
    }

    /**
     * putArtifact（写入中间产物）用于保存某一步生成的数据。
     *
     * @param key 产物键
     * @param value 产物值
     */
    public void putArtifact(String key, Object value) {
        // 如果写入的是 MemoryContext（记忆上下文），同步到强类型字段。
        if ("MEMORY_CONTEXT".equals(key) && value instanceof MemoryContext context) {
            // 保存强类型记忆上下文。
            this.memoryContext = context;
        }
        // 按键保存中间产物，后续步骤可以继续读取。
        artifacts.put(key, value);
    }

    /**
     * getArtifact（读取中间产物）用于按键获取某一步生成的数据。
     *
     * @param key 产物键
     * @return 产物值
     */
    public Object getArtifact(String key) {
        // 从中间产物 Map（映射）中读取指定数据。
        return artifacts.get(key);
    }
}
