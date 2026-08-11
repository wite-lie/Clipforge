package com.clipforge.rag.selection;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DefaultKnowledgeBaseSelector（默认知识库选择器）。
 *
 * <p>第一版采用可解释的规则链：显式选择优先，其次项目默认、平台默认，最后按配置决定是否全库检索。
 */
@Component
@RequiredArgsConstructor
public class DefaultKnowledgeBaseSelector implements KnowledgeBaseSelector {

    /** properties（知识库选择配置）用于读取默认知识库和全库检索开关。 */
    private final KnowledgeBaseSelectionProperties properties;

    /**
     * select（选择）用于解析本次创作请求的知识库检索范围。
     *
     * @param request 知识库选择请求
     * @return 知识库选择结果
     */
    @Override
    public KnowledgeBaseSelectionResult select(KnowledgeBaseSelectionRequest request) {
        // 请求为空时没有任何业务线索，直接进入全库兜底或无选择分支。
        if (request == null) {
            return fallbackToAllOrNone("请求为空，无法选择具体知识库");
        }
        // 用户或前端显式传入的知识库拥有最高优先级。
        List<String> explicitIds = properties.normalize(request.getExplicitKnowledgeBaseIds());
        if (!explicitIds.isEmpty()) {
            return result(explicitIds, false, KnowledgeBaseSelectionSource.EXPLICIT_REQUEST, "使用请求显式传入的知识库编号");
        }
        // 项目默认知识库适合一个项目固定绑定一组领域知识库。
        List<String> projectDefaultIds = configuredIds(properties.getProjectDefaultKnowledgeBases().get(trimToNull(request.getProjectId())));
        if (!projectDefaultIds.isEmpty()) {
            return result(projectDefaultIds, false, KnowledgeBaseSelectionSource.PROJECT_DEFAULT, "命中项目默认知识库，projectId：" + request.getProjectId());
        }
        // 平台默认知识库用于自动注入平台规则，例如 douyin（抖音）规则知识库。
        List<String> platformDefaultIds = configuredIds(properties.getPlatformDefaultKnowledgeBases().get(normalizePlatform(request.getPlatform())));
        if (!platformDefaultIds.isEmpty()) {
            return result(platformDefaultIds, false, KnowledgeBaseSelectionSource.PLATFORM_DEFAULT, "命中平台默认知识库，platform：" + request.getPlatform());
        }
        // 具体范围都未命中时，按配置决定是否全库检索。
        return fallbackToAllOrNone("显式、项目默认、平台默认均未命中");
    }

    /**
     * configuredIds（读取配置编号）用于清洗配置中的知识库编号列表。
     *
     * @param ids 原始编号列表
     * @return 清洗后的编号列表
     */
    private List<String> configuredIds(List<String> ids) {
        // 复用配置对象中的清洗逻辑。
        return properties.normalize(ids);
    }

    /**
     * fallbackToAllOrNone（兜底为全库或无选择）用于处理未命中具体知识库的情况。
     *
     * @param reason 原始原因
     * @return 知识库选择结果
     */
    private KnowledgeBaseSelectionResult fallbackToAllOrNone(String reason) {
        // 如果开启全库检索兜底，则返回全库检索范围。
        if (properties.isEnableAllKnowledgeBaseFallback()) {
            return result(List.of(), true, KnowledgeBaseSelectionSource.ALL_KNOWLEDGE_BASES, reason + "，改为全库检索");
        }
        // 如果关闭全库检索兜底，则保持无选择，后续检索器可以继续静态降级。
        return result(List.of(), false, KnowledgeBaseSelectionSource.NONE, reason + "，且全库检索兜底未启用");
    }

    /**
     * result（构造结果）用于统一创建选择结果对象。
     *
     * @param ids 知识库编号列表
     * @param searchAll 是否全库检索
     * @param source 选择来源
     * @param reason 选择原因
     * @return 知识库选择结果
     */
    private KnowledgeBaseSelectionResult result(List<String> ids, boolean searchAll, KnowledgeBaseSelectionSource source, String reason) {
        // 使用 Builder（构建器）创建结果对象。
        return KnowledgeBaseSelectionResult.builder()
                .knowledgeBaseIds(ids == null ? List.of() : ids)
                .searchAllKnowledgeBases(searchAll)
                .source(source)
                .reason(reason)
                .build();
    }

    /**
     * normalizePlatform（归一化平台）用于把平台名转成配置键。
     *
     * @param platform 平台名
     * @return 平台配置键
     */
    private String normalizePlatform(String platform) {
        // 空平台无法匹配配置。
        if (platform == null || platform.isBlank()) {
            return null;
        }
        // 平台统一使用小写去空格形式。
        return platform.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * trimToNull（去空白并转空）用于读取配置映射。
     *
     * @param text 原始文本
     * @return 非空文本或 null
     */
    private String trimToNull(String text) {
        // 空白文本返回 null。
        if (text == null || text.isBlank()) {
            return null;
        }
        // 返回去空白后的文本。
        return text.trim();
    }
}
