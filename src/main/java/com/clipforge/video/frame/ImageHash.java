package com.clipforge.video.frame;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ImageHash（图像哈希）。
 *
 * <p>该对象封装平均哈希结果，并提供 Hamming Distance（汉明距离）计算能力。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageHash {

    /** value（哈希值）使用 long（长整数）保存 64 位平均哈希。 */
    private long value;

    /** hex（十六进制文本）用于保存到数据库和调试输出。 */
    private String hex;

    /**
     * distanceTo（计算距离）用于判断当前哈希与另一个哈希的差异程度。
     *
     * @param other 另一个图像哈希
     * @return 汉明距离，数值越小越相似
     */
    public int distanceTo(ImageHash other) {
        // 空哈希视为完全不同，避免误判为重复。
        if (other == null) {
            return 64;
        }
        // 对两个哈希异或后统计不同位数量。
        return Long.bitCount(value ^ other.value);
    }
}
