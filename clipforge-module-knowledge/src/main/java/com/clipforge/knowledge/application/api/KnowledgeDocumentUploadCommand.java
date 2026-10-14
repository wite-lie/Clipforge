package com.clipforge.knowledge.application.api;

import lombok.Builder;
import lombok.Getter;

/**
 * KnowledgeDocumentUploadCommand（知识文档上传命令）承载 Web 层解析后的上传参数。
 *
 * <p>该对象不依赖 MultipartFile（多部分上传文件）等 Spring Web 类型，便于知识库上传用例继续下沉到 Knowledge 模块。
 */
@Getter
@Builder
public class KnowledgeDocumentUploadCommand {

    /** knowledgeBaseId（知识库编号）标识文件归属的知识库。 */
    private final String knowledgeBaseId;

    /** userId（用户编号）为空时由应用服务继承知识库用户。 */
    private final String userId;

    /** originalFilename（原始文件名）来自客户端上传文件名。 */
    private final String originalFilename;

    /** contentType（内容类型）来自上传请求头。 */
    private final String contentType;

    /** size（文件大小）用于上传限制校验和落库。 */
    private final long size;

    /** empty（是否为空文件）用于保留 MultipartFile#isEmpty 的语义。 */
    private final boolean empty;

    /** content（上传内容）用于存储层读取文件流。 */
    private final KnowledgeDocumentUploadContent content;
}
