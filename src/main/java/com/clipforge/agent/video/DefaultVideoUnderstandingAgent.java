package com.clipforge.agent.video;

import com.clipforge.trace.TraceRecorder;
import com.clipforge.video.understanding.VideoUnderstandingContext;
import com.clipforge.video.understanding.VideoUnderstandingContextBuilder;
import com.clipforge.video.understanding.VideoUnderstandingResult;
import com.clipforge.video.understanding.VideoUnderstandingSummarizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DefaultVideoUnderstandingAgent（默认视频理解智能体）。
 *
 * <p>该实现串联 VideoUnderstandingContextBuilder（视频理解上下文构造器）和 VideoUnderstandingSummarizer（视频理解摘要器）。
 */
@Component
@RequiredArgsConstructor
public class DefaultVideoUnderstandingAgent implements VideoUnderstandingAgent {

    /** contextBuilder（上下文构造器）负责收集元数据、候选帧理解和音频转写线索。 */
    private final VideoUnderstandingContextBuilder contextBuilder;

    /** summarizer（摘要器）负责把事实上下文生成视频级结构化理解结果。 */
    private final VideoUnderstandingSummarizer summarizer;

    /** traceRecorder（执行链路记录器）负责记录视频理解局部 Agent（智能体）的执行事件。 */
    private final TraceRecorder traceRecorder;

    /**
     * understand（理解视频）用于执行视频理解局部 Agent（智能体）。
     *
     * @param runId 运行编号
     * @param materialId 视频素材编号
     * @param instruction 理解指令
     * @return 视频理解结果
     */
    @Override
    public VideoUnderstandingResult understand(String runId, String materialId, String instruction) {
        // 记录视频理解开始事件。
        traceRecorder.record(runId, "VIDEO_UNDERSTANDING_AGENT_STARTED", "视频理解智能体开始执行", "materialId：" + materialId);
        // 构建视频理解上下文。
        VideoUnderstandingContext context = contextBuilder.build(runId, materialId, instruction);
        // 记录上下文构建完成事件。
        traceRecorder.record(runId, "VIDEO_UNDERSTANDING_CONTEXT_BUILT", "视频理解上下文构建完成", "候选帧数量：" + context.getFrames().size());
        // 生成视频理解结果。
        VideoUnderstandingResult result = summarizer.summarize(context);
        // 记录视频理解完成事件。
        traceRecorder.record(runId, "VIDEO_UNDERSTANDING_AGENT_FINISHED", "视频理解智能体执行完成", "status：" + result.getStatus());
        // 返回视频理解结果。
        return result;
    }
}
