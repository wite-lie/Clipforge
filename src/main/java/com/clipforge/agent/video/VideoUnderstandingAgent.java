package com.clipforge.agent.video;

import com.clipforge.video.understanding.VideoUnderstandingResult;

/**
 * VideoUnderstandingAgent（视频理解智能体）。
 *
 * <p>该接口定义视频理解局部 Agent（智能体）的入口，用于把视频素材转成可注入创作工作流的结构化摘要。
 */
public interface VideoUnderstandingAgent {

    /**
     * understand（理解视频）用于执行视频理解链路。
     *
     * @param runId 运行编号
     * @param materialId 视频素材编号
     * @param instruction 理解指令
     * @return 视频理解结果
     */
    VideoUnderstandingResult understand(String runId, String materialId, String instruction);
}
