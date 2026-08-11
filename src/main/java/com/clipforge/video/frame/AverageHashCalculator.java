package com.clipforge.video.frame;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import org.springframework.stereotype.Component;

/**
 * AverageHashCalculator（平均哈希计算器）。
 *
 * <p>该实现把图片缩放为 8x8 灰度图，通过灰度是否高于平均值生成 64 位哈希，用于轻量相似帧去重。
 */
@Component
public class AverageHashCalculator {

    /**
     * calculate（计算）用于对图片生成平均哈希。
     *
     * @param image 原始图片
     * @param hashSize 哈希边长，默认 8
     * @return 图像哈希
     */
    public ImageHash calculate(BufferedImage image, int hashSize) {
        // 校验图片不能为空。
        if (image == null) {
            // 返回空哈希。
            return ImageHash.builder().value(0L).hex("0000000000000000").build();
        }
        // 规范哈希尺寸，当前最多支持 8x8 生成 64 位哈希。
        int size = Math.max(1, Math.min(8, hashSize));
        // 创建缩放后的灰度采样矩阵。
        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        // 创建绘图上下文。
        Graphics2D graphics = resized.createGraphics();
        try {
            // 使用双线性插值，让缩放结果更稳定。
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // 绘制缩放图。
            graphics.drawImage(image, 0, 0, size, size, null);
        } finally {
            // 释放绘图资源。
            graphics.dispose();
        }
        // 保存灰度值。
        int[] grays = new int[size * size];
        // 灰度总和。
        long sum = 0;
        // 遍历缩放后的像素。
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // 读取灰度值。
                int gray = resized.getRaster().getSample(x, y, 0);
                // 保存灰度值。
                grays[y * size + x] = gray;
                // 累加灰度。
                sum += gray;
            }
        }
        // 计算平均灰度。
        double average = sum * 1.0 / grays.length;
        // 创建哈希值。
        long hash = 0L;
        // 逐位写入哈希。
        for (int index = 0; index < grays.length; index++) {
            // 灰度大于等于平均值时置 1。
            if (grays[index] >= average) {
                // 设置对应位。
                hash |= (1L << index);
            }
        }
        // 返回哈希对象。
        return ImageHash.builder()
                .value(hash)
                .hex(String.format("%016x", hash))
                .build();
    }
}
