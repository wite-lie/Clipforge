package com.clipforge.material.sampling;

import com.clipforge.contract.material.VideoFrameResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * RuleBasedMaterialUnderstandingSampler（规则素材理解抽样器）用确定性规则控制进入模型的素材数量。
 */
@Component
@RequiredArgsConstructor
public class RuleBasedMaterialUnderstandingSampler implements MaterialUnderstandingSampler {

    /** properties（抽样配置）用于读取数量上限和选择策略。 */
    private final MaterialUnderstandingSamplingProperties properties;

    /**
     * selectVideoFrames（选择视频帧）会先过滤无帧素材编号的记录，再按配置选择全部、前 N 个或均匀分布的 N 个。
     *
     * @param frames 已注册为图片素材的视频帧列表
     * @return 被选中进入 VLM（视觉语言模型）理解的视频帧列表
     */
    @Override
    public List<VideoFrameResponse> selectVideoFrames(List<VideoFrameResponse> frames) {
        // 空列表直接返回空结果，调用方会决定是否终止流程。
        if (frames == null || frames.isEmpty()) {
            return List.of();
        }
        // 只保留已经注册成图片素材的帧，避免把无效帧送进图片理解服务。
        List<VideoFrameResponse> validFrames = frames.stream()
                .filter(frame -> frame.getFrameMaterialId() != null && !frame.getFrameMaterialId().isBlank())
                .toList();
        // 抽样关闭时返回全部有效帧，用于本地调试或需要完整理解的场景。
        if (!properties.isEnabled()) {
            return validFrames;
        }
        // 读取单个视频最大理解帧数，至少保留 1 帧。
        int maxFrames = Math.max(1, properties.getMaxVideoFramesPerVideo());
        // 有效帧没有超过上限时无需抽样。
        if (validFrames.size() <= maxFrames) {
            return validFrames;
        }
        // 按时间点和帧序号排序，保证抽样结果稳定。
        List<VideoFrameResponse> orderedFrames = orderVideoFrames(validFrames);
        // 配置不要求均匀分布时，直接保留前 N 帧。
        if (!properties.isPreferEvenlyDistributedVideoFrames()) {
            return orderedFrames.stream().limit(maxFrames).toList();
        }
        // 均匀选择前中后位置的帧，避免只理解视频开头。
        return selectEvenly(orderedFrames, maxFrames);
    }

    /**
     * orderVideoFrames（排序视频帧）用于让抽样结果不受数据库返回顺序影响。
     *
     * @param frames 有效视频帧列表
     * @return 排序后的视频帧列表
     */
    private List<VideoFrameResponse> orderVideoFrames(List<VideoFrameResponse> frames) {
        // 复制列表，避免修改调用方传入的集合。
        List<VideoFrameResponse> orderedFrames = new ArrayList<>(frames);
        // 优先按时间点排序，其次按帧序号排序，最后按帧编号兜底排序。
        orderedFrames.sort(Comparator
                .comparing((VideoFrameResponse frame) -> frame.getTimestampSeconds(), Comparator.nullsLast(Double::compareTo))
                .thenComparing(frame -> frame.getFrameIndex(), Comparator.nullsLast(Integer::compareTo))
                .thenComparing(frame -> frame.getFrameId(), Comparator.nullsLast(String::compareTo)));
        // 返回排序结果。
        return orderedFrames;
    }

    /**
     * selectEvenly（均匀选择）用于从有序帧中取出覆盖前中后段的固定数量帧。
     *
     * @param frames 有序视频帧列表
     * @param maxFrames 最大选择数量
     * @return 均匀选择后的视频帧列表
     */
    private List<VideoFrameResponse> selectEvenly(List<VideoFrameResponse> frames, int maxFrames) {
        // 只允许选择 1 帧时，直接选择排序后的第一帧作为代表帧。
        if (maxFrames == 1) {
            return List.of(frames.get(0));
        }
        // 使用 LinkedHashMap（有序哈希表）去重并保持选择顺序。
        Map<Integer, VideoFrameResponse> selectedByIndex = new LinkedHashMap<>();
        // 计算最后一个下标，便于把区间平均分成 maxFrames - 1 段。
        int lastIndex = frames.size() - 1;
        // 逐个计算目标位置，确保第一帧和最后一帧尽量被覆盖。
        for (int selectedIndex = 0; selectedIndex < maxFrames; selectedIndex++) {
            // 四舍五入得到当前目标位置。
            int frameIndex = (int) Math.round((double) selectedIndex * lastIndex / (maxFrames - 1));
            // 放入结果集合，重复位置会被覆盖但不会重复输出。
            selectedByIndex.put(frameIndex, frames.get(frameIndex));
        }
        // 极端情况下如果四舍五入导致数量不足，则从头补齐未选择的帧。
        for (int frameIndex = 0; selectedByIndex.size() < maxFrames && frameIndex < frames.size(); frameIndex++) {
            selectedByIndex.putIfAbsent(frameIndex, frames.get(frameIndex));
        }
        // 返回最终抽样结果。
        return new ArrayList<>(selectedByIndex.values());
    }
}
