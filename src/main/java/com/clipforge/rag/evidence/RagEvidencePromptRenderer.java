package com.clipforge.rag.evidence;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RagEvidencePromptRenderer（RAG 证据提示词渲染器）。
 *
 * <p>该组件只负责把已经筛选好的证据渲染成 Prompt（提示词）文本，不参与检索排序。
 */
@Component
public class RagEvidencePromptRenderer {

    /**
     * render（渲染）用于把证据列表转换成可注入大模型的文本。
     *
     * @param evidences 证据列表
     * @param insufficientEvidenceMarker 是否加入证据不足提示
     * @return 提示词上下文
     */
    public String render(List<RagEvidence> evidences, boolean insufficientEvidenceMarker) {
        // 没有证据时返回保守生成提示或空文本。
        if (evidences == null || evidences.isEmpty()) {
            return insufficientEvidenceMarker ? "【知识库证据不足】当前没有可用的 RAG（检索增强生成）证据。生成时不要编造平台规则、专业事实或具体数据。" : "";
        }
        // 使用 StringBuilder（字符串构建器）拼接证据区。
        StringBuilder builder = new StringBuilder();
        for (RagEvidence evidence : evidences) {
            builder.append("【证据 ").append(evidence.getEvidenceIndex()).append("】\n");
            builder.append("来源：").append(defaultText(evidence.getTitlePath(), "未提供标题路径")).append("\n");
            builder.append("知识库编号：").append(defaultText(evidence.getKnowledgeBaseId(), "无")).append("\n");
            builder.append("文档编号：").append(defaultText(evidence.getDocumentId(), "无")).append("\n");
            builder.append("切片编号：").append(defaultText(evidence.getChunkId(), "无")).append("\n");
            builder.append("检索来源：").append(evidence.getSource() == null ? "UNKNOWN" : evidence.getSource().value()).append("\n");
            builder.append("相关性分数：").append(evidence.getFinalScore() == null ? "未提供" : evidence.getFinalScore()).append("\n");
            builder.append("内容：").append(defaultText(evidence.getContent(), "无内容"));
            if (evidence.isTruncated()) {
                builder.append("\n【注意：该证据因上下文预算已被截断】");
            }
            builder.append("\n\n");
        }
        return builder.toString().trim();
    }

    /**
     * defaultText（默认文本）用于避免 Prompt（提示词）里出现 null。
     *
     * @param value 原始文本
     * @param defaultValue 默认文本
     * @return 非空文本
     */
    private String defaultText(String value, String defaultValue) {
        // 空白值返回默认文本。
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
