package com.clipforge.rag;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * KnowledgeUploadProperties（知识库上传配置属性）。
 *
 * <p>该配置集中管理 RAG（检索增强生成）知识文件上传目录、并发限制、允许类型和单文件业务大小限制。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.rag.upload")
public class KnowledgeUploadProperties {

    /** rootPath（根路径）表示知识文件在本地磁盘中的保存目录。 */
    private String rootPath = "data/uploads/knowledge";

    /** maxConcurrent（最大并发数）表示当前单 JVM（Java 虚拟机）允许同时处理的知识文件上传请求数量。 */
    private int maxConcurrent = 3;

    /** maxWaitMillis（最大等待毫秒）表示上传许可不足时最多等待多久，0 表示立即失败。 */
    private long maxWaitMillis = 0;

    /** maxFileSizeBytes（单文件最大字节数）用于业务层二次限制，默认 20MB。 */
    private long maxFileSizeBytes = 20L * 1024L * 1024L;

    /** allowedExtensions（允许扩展名）当前 Phase 7.12（阶段 7.12）支持 Markdown（标记文档）、TXT（纯文本）、PDF（便携式文档格式）和 Word（文字处理文档）。 */
    private List<String> allowedExtensions = List.of(".md", ".txt", ".pdf", ".doc", ".docx");

    /** allowedContentTypes（允许内容类型）用于限制上传文件 MIME（媒体类型）。 */
    private List<String> allowedContentTypes = List.of(
            "text/markdown",
            "text/plain",
            "application/octet-stream",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
}
