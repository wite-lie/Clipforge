package com.clipforge.video.metadata;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * FFprobeJsonVideoMetadataParser（FFprobe JSON 视频元数据解析器）。
 *
 * <p>该解析器使用 Jackson（JSON 解析库）读取 FFprobe 输出，避免手写字符串截取。
 */
@Component
@RequiredArgsConstructor
public class FFprobeJsonVideoMetadataParser implements VideoMetadataParser {

    /** objectMapper（对象映射器）用于解析 JSON（对象表示格式）。 */
    private final ObjectMapper objectMapper;

    /**
     * parse（解析）用于把 FFprobe JSON 转换为 VideoMetadata（视频元数据）。
     *
     * @param rawJson FFprobe 原始 JSON
     * @return 视频元数据
     */
    @Override
    public VideoMetadata parse(String rawJson) {
        // 原始 JSON 不能为空。
        if (rawJson == null || rawJson.isBlank()) {
            // 抛出解析失败异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FFprobe 原始 JSON 不能为空");
        }
        try {
            // 解析根节点。
            JsonNode root = objectMapper.readTree(rawJson);
            // 读取 format（格式）节点。
            JsonNode formatNode = root.path("format");
            // 遍历 streams（媒体流）节点并寻找第一条视频流和第一条音频流。
            JsonNode streamsNode = root.path("streams");
            // 初始化视频流。
            VideoStreamInfo videoStream = null;
            // 初始化音频流。
            AudioStreamInfo audioStream = null;
            // 初始化音轨数量。
            int audioStreamCount = 0;
            // 如果 streams 是数组，则逐条读取。
            if (streamsNode.isArray()) {
                // 遍历媒体流。
                for (JsonNode streamNode : streamsNode) {
                    // 读取流类型。
                    String codecType = text(streamNode, "codec_type");
                    // 处理视频流。
                    if ("video".equals(codecType) && videoStream == null) {
                        // 构建第一条视频流信息。
                        videoStream = VideoStreamInfo.builder()
                                // 读取视频编码。
                                .codecName(text(streamNode, "codec_name"))
                                // 读取宽度。
                                .width(integer(streamNode, "width"))
                                // 读取高度。
                                .height(integer(streamNode, "height"))
                                // 优先读取 avg_frame_rate（平均帧率），没有时使用 r_frame_rate（基础帧率）。
                                .frameRate(frameRate(streamNode))
                                // 构建视频流对象。
                                .build();
                    }
                    // 处理音频流。
                    if ("audio".equals(codecType)) {
                        // 累加音轨数量。
                        audioStreamCount++;
                        // 只保存第一条音频流详情。
                        if (audioStream == null) {
                            // 构建第一条音频流信息。
                            audioStream = AudioStreamInfo.builder()
                                    // 读取音频编码。
                                    .codecName(text(streamNode, "codec_name"))
                                    // 构建音频流对象。
                                    .build();
                        }
                    }
                }
            }
            // 如果没有视频流，则认为格式不适合作为视频素材。
            if (videoStream == null) {
                // 抛出参数异常。
                throw new BusinessException(ErrorCode.BAD_REQUEST, "FFprobe 结果中没有视频流");
            }
            // 构建视频元数据对象。
            return VideoMetadata.builder()
                    // 读取视频时长。
                    .durationSeconds(doubleValue(formatNode, "duration"))
                    // 读取整体码率。
                    .bitRate(longValue(formatNode, "bit_rate"))
                    // 读取容器格式。
                    .formatName(text(formatNode, "format_name"))
                    // 设置视频流信息。
                    .videoStream(videoStream)
                    // 设置音频流信息。
                    .audioStream(audioStream)
                    // 设置音轨数量。
                    .audioStreamCount(audioStreamCount)
                    // 保存原始 JSON。
                    .rawProbeJson(rawJson)
                    // 构建结果。
                    .build();
        } catch (BusinessException exception) {
            // 业务异常直接向外抛出。
            throw exception;
        } catch (Exception exception) {
            // JSON 解析失败时抛出业务异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FFprobe JSON 解析失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * frameRate（帧率）用于读取并解析 FFprobe 帧率字段。
     *
     * @param streamNode 媒体流节点
     * @return 帧率
     */
    private Double frameRate(JsonNode streamNode) {
        // 优先读取平均帧率。
        String avgFrameRate = text(streamNode, "avg_frame_rate");
        // 如果平均帧率为空或为 0/0，则读取基础帧率。
        if (avgFrameRate == null || "0/0".equals(avgFrameRate)) {
            // 使用基础帧率。
            avgFrameRate = text(streamNode, "r_frame_rate");
        }
        // 解析分数字符串。
        return parseFraction(avgFrameRate);
    }

    /**
     * parseFraction（解析分数）用于把 30000/1001 这类 FFprobe 帧率转换为小数。
     *
     * @param value 原始值
     * @return 小数值
     */
    private Double parseFraction(String value) {
        // 空值返回 null。
        if (value == null || value.isBlank()) {
            // 返回空引用。
            return null;
        }
        // 如果包含斜杠，则按分数处理。
        if (value.contains("/")) {
            // 拆分分子和分母。
            String[] parts = value.split("/");
            // 分数格式必须是两段。
            if (parts.length != 2) {
                // 返回空引用。
                return null;
            }
            // 解析分子。
            double numerator = Double.parseDouble(parts[0]);
            // 解析分母。
            double denominator = Double.parseDouble(parts[1]);
            // 分母为 0 时返回 null。
            if (denominator == 0) {
                // 返回空引用。
                return null;
            }
            // 返回小数帧率。
            return numerator / denominator;
        }
        // 普通数字直接解析。
        return Double.parseDouble(value);
    }

    /**
     * text（文本值）用于安全读取字符串字段。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @return 字符串值
     */
    private String text(JsonNode node, String fieldName) {
        // 节点缺失时返回 null。
        if (node == null || node.path(fieldName).isMissingNode() || node.path(fieldName).isNull()) {
            // 返回空引用。
            return null;
        }
        // 返回文本值。
        return node.path(fieldName).asText();
    }

    /**
     * integer（整数值）用于安全读取整数字段。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @return 整数值
     */
    private Integer integer(JsonNode node, String fieldName) {
        // 读取文本值。
        String value = text(node, fieldName);
        // 空值返回 null。
        if (value == null || value.isBlank()) {
            // 返回空引用。
            return null;
        }
        // 解析整数。
        return Integer.parseInt(value);
    }

    /**
     * doubleValue（小数值）用于安全读取小数字段。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @return 小数值
     */
    private Double doubleValue(JsonNode node, String fieldName) {
        // 读取文本值。
        String value = text(node, fieldName);
        // 空值返回 null。
        if (value == null || value.isBlank()) {
            // 返回空引用。
            return null;
        }
        // 解析小数。
        return Double.parseDouble(value);
    }

    /**
     * longValue（长整数值）用于安全读取长整数字段。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @return 长整数值
     */
    private Long longValue(JsonNode node, String fieldName) {
        // 读取文本值。
        String value = text(node, fieldName);
        // 空值返回 null。
        if (value == null || value.isBlank()) {
            // 返回空引用。
            return null;
        }
        // 解析长整数。
        return Long.parseLong(value);
    }
}
