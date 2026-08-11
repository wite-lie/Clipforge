package com.clipforge.video.frame;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * VideoFrameQualityAnalyzer（视频帧质量分析器）。
 *
 * <p>该组件使用 Java 标准图片能力分析候选帧，不调用模型，也不依赖 OpenCV（开源计算机视觉库）。
 */
@Component
@RequiredArgsConstructor
public class VideoFrameQualityAnalyzer {

    /** videoFrameProperties（视频帧配置）提供亮度和清晰度过滤阈值。 */
    private final VideoFrameProperties videoFrameProperties;

    /**
     * analyze（分析）用于读取图片并计算质量指标。
     *
     * @param filePath 图片路径
     * @return 质量分析结果
     */
    public VideoFrameQuality analyze(String filePath) {
        // 文件路径为空时直接返回无效。
        if (filePath == null || filePath.isBlank()) {
            return invalid("候选帧文件路径为空");
        }
        // 转换为路径对象。
        Path path = Path.of(filePath);
        // 文件不存在时直接返回无效。
        if (!Files.exists(path)) {
            return invalid("候选帧文件不存在");
        }
        try {
            // 空文件直接视为无效。
            if (Files.size(path) <= 0) {
                return invalid("候选帧文件为空");
            }
            // 使用 ImageIO（Java 图片输入输出工具）读取图片。
            BufferedImage image = ImageIO.read(path.toFile());
            // 图片无法解析时视为无效。
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return invalid("候选帧图片无法解析");
            }
            // 计算平均亮度。
            double brightness = calculateBrightness(image);
            // 计算简化清晰度分数。
            double sharpness = calculateSharpness(image);
            // 判断质量问题。
            boolean blackFrame = brightness < 8 && sharpness < videoFrameProperties.getMinSharpnessScore();
            boolean tooDark = brightness < videoFrameProperties.getMinBrightness();
            boolean tooBright = brightness > videoFrameProperties.getMaxBrightness();
            boolean tooBlurred = sharpness < videoFrameProperties.getMinSharpnessScore();
            // 构建过滤原因。
            String rejectReason = rejectReason(blackFrame, tooDark, tooBright, tooBlurred);
            // 计算质量分。
            double qualityScore = Math.max(0, Math.min(100, sharpness + brightness / 4));
            // 返回质量结果。
            return VideoFrameQuality.builder()
                    .valid(rejectReason == null)
                    .brightness(round(brightness))
                    .sharpnessScore(round(sharpness))
                    .qualityScore(round(qualityScore))
                    .blackFrame(blackFrame)
                    .tooDark(tooDark)
                    .tooBright(tooBright)
                    .tooBlurred(tooBlurred)
                    .rejectReason(rejectReason)
                    .build();
        } catch (Exception exception) {
            // 图片读取或文件访问异常时返回无效结果。
            return invalid("候选帧质量分析失败：" + exception.getMessage());
        }
    }

    /**
     * calculateBrightness（计算亮度）用于返回全图平均灰度。
     */
    private double calculateBrightness(BufferedImage image) {
        // 灰度总和。
        long sum = 0;
        // 像素数量。
        long count = 0;
        // 遍历像素。
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                // 累加灰度。
                sum += gray(image.getRGB(x, y));
                // 增加数量。
                count++;
            }
        }
        // 返回平均亮度。
        return count == 0 ? 0 : sum * 1.0 / count;
    }

    /**
     * calculateSharpness（计算清晰度）通过相邻像素灰度差估算细节丰富程度。
     */
    private double calculateSharpness(BufferedImage image) {
        // 灰度差总和。
        long diffSum = 0;
        // 差值数量。
        long diffCount = 0;
        // 遍历像素，比较右侧和下方邻居。
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                // 当前像素灰度。
                int current = gray(image.getRGB(x, y));
                // 比较右侧像素。
                if (x + 1 < image.getWidth()) {
                    diffSum += Math.abs(current - gray(image.getRGB(x + 1, y)));
                    diffCount++;
                }
                // 比较下方像素。
                if (y + 1 < image.getHeight()) {
                    diffSum += Math.abs(current - gray(image.getRGB(x, y + 1)));
                    diffCount++;
                }
            }
        }
        // 返回平均差值。
        return diffCount == 0 ? 0 : diffSum * 1.0 / diffCount;
    }

    /**
     * gray（灰度）用于把 RGB（红绿蓝颜色）转换为人眼亮度近似值。
     */
    private int gray(int rgb) {
        // 提取红色通道。
        int red = (rgb >> 16) & 0xff;
        // 提取绿色通道。
        int green = (rgb >> 8) & 0xff;
        // 提取蓝色通道。
        int blue = rgb & 0xff;
        // 返回加权灰度。
        return (int) Math.round(red * 0.299 + green * 0.587 + blue * 0.114);
    }

    /**
     * rejectReason（过滤原因）用于按优先级返回质量问题。
     */
    private String rejectReason(boolean blackFrame, boolean tooDark, boolean tooBright, boolean tooBlurred) {
        if (blackFrame) {
            return "黑屏或近似黑屏";
        }
        if (tooDark) {
            return "画面过暗";
        }
        if (tooBright) {
            return "画面过亮";
        }
        if (tooBlurred) {
            return "画面模糊或细节过少";
        }
        return null;
    }

    /**
     * invalid（无效结果）用于构建质量分析失败结果。
     */
    private VideoFrameQuality invalid(String reason) {
        return VideoFrameQuality.builder()
                .valid(false)
                .brightness(0)
                .sharpnessScore(0)
                .qualityScore(0)
                .rejectReason(reason)
                .build();
    }

    /**
     * round（四舍五入）用于让接口和数据库中的分数更容易阅读。
     */
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
