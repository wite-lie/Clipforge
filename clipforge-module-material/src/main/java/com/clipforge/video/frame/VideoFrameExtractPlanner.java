package com.clipforge.video.frame;

/**
 * VideoFrameExtractPlanner（视频帧抽取规划器）。
 *
 * <p>该接口负责根据视频时长生成候选帧时间点。
 */
public interface VideoFrameExtractPlanner {

    /**
     * plan（规划）用于生成固定数量的候选帧时间点。
     *
     * @param materialId 素材编号
     * @param durationSeconds 视频时长，单位秒
     * @return 抽帧计划
     */
    VideoFrameExtractPlan plan(String materialId, double durationSeconds);
}
