package com.clipforge.knowledge.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KnowledgeDocumentParseRequest（知识文档解析请求）。
 *
 * <p>该对象是文档解析端口的输入契约，只携带解析所需的文件快照信息，
 * 避免解析器直接依赖数据库 Entity（实体）或应用服务。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentParseRequest {

    /** documentId（文档编号）用于把 OCR（光学字符识别）或解析错误关联回业务文档。 */
    private String documentId;

    /** originalFilename（原始文件名）用于识别文件扩展名和构造 OCR 请求。 */
    private String originalFilename;

    /** filePath（本地文件路径）是解析器实际读取的文件位置。 */
    private String filePath;

    /** contentType（内容类型）用于后续扩展更严格的文件类型校验。 */
    private String contentType;

    /** sourceType（来源类型）用于记录解析来源上下文。 */
    private String sourceType;
}
