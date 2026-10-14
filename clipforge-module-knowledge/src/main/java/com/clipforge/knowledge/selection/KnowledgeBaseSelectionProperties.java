package com.clipforge.knowledge.selection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * KnowledgeBaseSelectionProperties（知识库选择配置）。
 *
 * <p>该配置用于维护项目默认知识库、平台默认知识库和全库检索兜底开关。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.rag.selection")
public class KnowledgeBaseSelectionProperties {

    /** enableAllKnowledgeBaseFallback（启用全库检索兜底）表示具体知识库未命中时是否放开知识库过滤。 */
    private boolean enableAllKnowledgeBaseFallback = true;

    /** projectDefaultKnowledgeBases（项目默认知识库）按 projectId（项目编号）配置默认知识库编号列表。 */
    private Map<String, List<String>> projectDefaultKnowledgeBases = new LinkedHashMap<>();

    /** platformDefaultKnowledgeBases（平台默认知识库）按 platform（平台）配置默认知识库编号列表。 */
    private Map<String, List<String>> platformDefaultKnowledgeBases = new LinkedHashMap<>();

    /**
     * getProjectDefaultKnowledgeBases（获取项目默认知识库）用于避免配置为空时出现空指针。
     *
     * @return 项目默认知识库映射
     */
    public Map<String, List<String>> getProjectDefaultKnowledgeBases() {
        // 配置为空时返回空映射。
        if (projectDefaultKnowledgeBases == null) {
            return new LinkedHashMap<>();
        }
        return projectDefaultKnowledgeBases;
    }

    /**
     * getPlatformDefaultKnowledgeBases（获取平台默认知识库）用于避免配置为空时出现空指针。
     *
     * @return 平台默认知识库映射
     */
    public Map<String, List<String>> getPlatformDefaultKnowledgeBases() {
        // 配置为空时返回空映射。
        if (platformDefaultKnowledgeBases == null) {
            return new LinkedHashMap<>();
        }
        return platformDefaultKnowledgeBases;
    }

    /**
     * normalize（归一化）用于去掉空白知识库编号并去重。
     *
     * @param values 原始编号列表
     * @return 清洗后的编号列表
     */
    public List<String> normalize(List<String> values) {
        // 空列表直接返回空集合。
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        // 清洗空白和重复编号。
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
}
