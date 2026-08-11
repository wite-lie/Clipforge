package com.clipforge.model.vision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VisionModelRequestMaterial（视觉模型请求素材）。
 *
 * <p>该对象描述一次图片理解调用中的单个图片素材。Phase 6.3（阶段 6.3）先传本地文件路径，Phase 6.4（阶段 6.4）真实 VLM（视觉语言模型）接入时再适配 Base64（基础 64 编码）或 URL（统一资源定位符）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisionModelRequestMaterial {

    /** materialId（素材编号）表示系统内部素材唯一编号。 */
    private String materialId;

    /** originalFilename（原始文件名）表示用户上传时的文件名。 */
    private String originalFilename;

    /** filePath（文件路径）表示图片在本地文件系统或对象存储中的位置。 */
    private String filePath;

    /** contentType（内容类型）表示图片 MIME（媒体类型），例如 image/jpeg。 */
    private String contentType;

    /** fileSize（文件大小）表示图片字节数。 */
    private Long fileSize;
}
