package com.clipforge.contract.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RetryKnowledgeDocumentProcessRequest（重试知识文档处理请求）。
 *
 * <p>该对象用于人工恢复接口指定从哪个阶段重新处理知识文档。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryKnowledgeDocumentProcessRequest {

    /** retryStage（重试阶段）为空时由后端根据最近失败阶段自动推断。 */
    private String retryStage;
}
