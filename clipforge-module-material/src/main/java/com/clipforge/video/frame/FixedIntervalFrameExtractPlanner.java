package com.clipforge.video.frame;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * FixedIntervalFrameExtractPlanner（固定间隔视频帧抽取规划器）。
 *
 * <p>该规划器根据视频时长生成稳定、可解释的候选帧时间点，第一版不做画面差异和语义判断。
 */
@Component
@RequiredArgsConstructor
public class FixedIntervalFrameExtractPlanner implements VideoFrameExtractPlanner {

    /** videoFrameProperties（视频帧配置）提供抽帧数量和开头结尾避让时间。 */
    private final VideoFrameProperties videoFrameProperties;

    /**
     * plan（规划）用于生成固定间隔候选帧时间点。
     *
     * @param materialId 素材编号
     * @param durationSeconds 视频时长，单位秒
     * @return 抽帧计划
     */
    @Override
    public VideoFrameExtractPlan plan(String materialId, double durationSeconds) {
        // 创建候选帧集合。
        List<VideoFrameCandidate> candidates = new ArrayList<>();
        // 非法时长直接返回空计划。
        if (durationSeconds <= 0) {
            // 返回空计划。
            return buildPlan(materialId, durationSeconds, candidates);
        }
        // 读取最大原始候选帧数量，至少为 1。
        int maxFrameCount = Math.max(1, videoFrameProperties.getMaxRawFrameCount());
        // 读取原始抽帧间隔，至少为 1 秒。
        double rawIntervalSeconds = Math.max(1, videoFrameProperties.getRawIntervalSeconds());
        // 计算抽帧开始时间。
        double start = Math.max(0, videoFrameProperties.getStartMarginSeconds());
        // 计算抽帧结束时间。
        double end = durationSeconds - Math.max(0, videoFrameProperties.getEndMarginSeconds());
        // 如果视频太短导致可用区间无效，则使用视频中点。
        if (end <= start) {
            // 添加视频中点候选帧。
            candidates.add(VideoFrameCandidate.builder()
                    // 设置帧序号。
                    .frameIndex(0)
                    // 设置时间点。
                    .timestampSeconds(round(durationSeconds / 2))
                    // 构建候选帧。
                    .build());
            // 返回计划。
            return buildPlan(materialId, durationSeconds, candidates);
        }
        // 从开头避让后的第一个间隔点开始抽帧。
        double timestamp = start + rawIntervalSeconds;
        // 当前候选帧序号。
        int index = 0;
        // 按固定间隔生成更多原始候选帧时间点。
        while (timestamp < end && index < maxFrameCount) {
            // 添加候选帧。
            candidates.add(VideoFrameCandidate.builder()
                    // 设置帧序号。
                    .frameIndex(index)
                    // 设置四舍五入后的时间点。
                    .timestampSeconds(round(timestamp))
                    // 构建候选帧。
                    .build());
            // 移动到下一个时间点。
            timestamp += rawIntervalSeconds;
            // 增加候选帧序号。
            index++;
        }
        // 如果可用区间短于抽帧间隔，则保底抽取视频中间点。
        if (candidates.isEmpty()) {
            candidates.add(VideoFrameCandidate.builder()
                    // 设置帧序号。
                    .frameIndex(0)
                    // 设置可用区间中点。
                    .timestampSeconds(round((start + end) / 2))
                    // 构建候选帧。
                    .build());
        }
        // 返回抽帧计划。
        return buildPlan(materialId, durationSeconds, candidates);
    }

    /**
     * buildPlan（构建计划）用于组装抽帧计划对象。
     *
     * @param materialId 素材编号
     * @param durationSeconds 视频时长
     * @param candidates 候选帧集合
     * @return 抽帧计划
     */
    private VideoFrameExtractPlan buildPlan(String materialId, double durationSeconds, List<VideoFrameCandidate> candidates) {
        // 构建并返回抽帧计划。
        return VideoFrameExtractPlan.builder()
                // 设置素材编号。
                .materialId(materialId)
                // 设置视频时长。
                .durationSeconds(durationSeconds)
                // 设置候选帧集合。
                .candidates(candidates)
                // 构建计划。
                .build();
    }

    /**
     * round（四舍五入）用于把时间点保留三位小数。
     *
     * @param value 原始值
     * @return 四舍五入后的值
     */
    private double round(double value) {
        // 使用 BigDecimal（高精度数字）避免浮点展示过长。
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }
}
