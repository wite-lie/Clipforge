package com.clipforge.tool;

import com.clipforge.agent.AgentContext;
import com.clipforge.rag.StaticCreativeKnowledgeRetriever;
import com.clipforge.trace.TraceRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * CreativeKnowledgeRetrieveTool（创作知识检索工具）。
 *
 * <p>该工具是真正意义上的 Tool（工具），负责为 Agent（智能体）获取模型自身不稳定记忆的外部创作知识。
 */
@Component
@RequiredArgsConstructor
public class CreativeKnowledgeRetrieveTool implements Tool {

    /** traceRecorder（执行链路记录器）用于记录知识检索过程。 */
    private final TraceRecorder traceRecorder;

    /** staticCreativeKnowledgeRetriever（静态创作知识检索器）用于从 Markdown（标记文档）知识库读取创作知识。 */
    private final StaticCreativeKnowledgeRetriever staticCreativeKnowledgeRetriever;

    /**
     * name（工具名称）用于标识创作知识检索工具。
     *
     * @return 工具名称
     */
    @Override
    public String name() {
        // 返回工具名称。
        return "creative_knowledge_retrieve";
    }

    /**
     * description（工具描述）用于说明该工具负责检索创作知识。
     *
     * @return 工具描述
     */
    @Override
    public String description() {
        // 返回工具能力描述。
        return "根据目标平台检索平台规则、脚本结构和评估提示";
    }

    /**
     * retrieve（检索）用于根据 AgentContext（智能体上下文）获取创作知识。
     *
     * @param context 智能体上下文
     * @return 创作知识
     */
    public CreativeKnowledge retrieve(AgentContext context) {
        // 记录知识检索开始事件。
        traceRecorder.record(context.getRunId(), "KNOWLEDGE_TOOL_STARTED", "开始检索创作知识", "平台：" + context.getPlatform());
        // 调用静态创作知识检索器，从 data/knowledge（静态知识库目录）读取平台知识。
        CreativeKnowledge knowledge = staticCreativeKnowledgeRetriever.retrieveByPlatform(context.getPlatform());
        // 把创作知识写入智能体上下文，供 Planner（规划器）、Generator（生成器）和 Evaluator（评估器）使用。
        context.putArtifact("CREATIVE_KNOWLEDGE", knowledge);
        // 记录知识检索完成事件。
        traceRecorder.record(context.getRunId(), "KNOWLEDGE_TOOL_FINISHED", "创作知识检索完成", "规则数：" + knowledge.getPlatformRules().size());
        // 返回创作知识。
        return knowledge;
    }
}
