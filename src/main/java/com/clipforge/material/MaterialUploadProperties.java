package com.clipforge.material;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MaterialUploadProperties（素材上传配置属性）。
 *
 * <p>该配置集中管理素材上传目录、并发限制、图片限制和视频限制。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.material.upload")
public class MaterialUploadProperties {

    /** rootPath（根路径）表示上传素材在本地磁盘中的保存目录。 */
    private String rootPath = "data/uploads/materials";

    /** maxConcurrent（最大并发数）表示当前单 JVM（Java 虚拟机）允许同时进入 multipart（多部分表单）解析的上传请求数量。 */
    private int maxConcurrent = 5;

    /** maxWaitMillis（最大等待毫秒）表示上传许可不足时最多等待多久，0 表示立即失败。 */
    private long maxWaitMillis = 0;

    /** maxImageFileSizeBytes（图片最大字节数）表示单张图片允许上传的最大体积。 */
    private long maxImageFileSizeBytes = 20L * 1024L * 1024L;

    /** maxVideoFileSizeBytes（视频最大字节数）表示单个视频允许上传的最大体积。 */
    private long maxVideoFileSizeBytes = 300L * 1024L * 1024L;

    /** allowedImageContentTypes（允许图片内容类型）表示当前允许上传的图片 MIME（媒体类型）列表。 */
    private List<String> allowedImageContentTypes = List.of("image/jpeg", "image/png", "image/webp");

    /** allowedVideoContentTypes（允许视频内容类型）表示当前允许上传的视频 MIME（媒体类型）列表。 */
    private List<String> allowedVideoContentTypes = List.of("video/mp4", "video/quicktime", "video/webm");
}
