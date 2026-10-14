package com.clipforge.ai.infrastructure.rerank;
import com.clipforge.ai.api.rerank.*;

import java.util.Comparator;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MockRerankModelClient（模拟重排序模型客户端）。
 *
 * <p>该实现用于本地默认环境，不调用真实 Rerank Model（重排序模型），按照查询词在候选文本中的出现情况生成可预测分数。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.rerank", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockRerankModelClient implements RerankModelClient {

    /**
     * rerank（重排序）用于生成模拟重排序结果。
     *
     * @param request 重排序模型请求
     * @return 重排序模型响应
     */
    @Override
    public RerankModelResponse rerank(RerankModelRequest request) {
        // 读取候选列表。
        List<RerankCandidate> candidates = request == null || request.getCandidates() == null ? List.of() : request.getCandidates();
        // 解析返回数量。
        int topN = resolveTopN(request, candidates.size());
        // 生成模拟得分并排序。
        List<RerankScoredItem> results = candidates.stream()
                // 转换为得分项。
                .map(candidate -> RerankScoredItem.builder()
                        // 设置索引。
                        .index(candidate.getIndex())
                        // 设置候选编号。
                        .candidateId(candidate.getCandidateId())
                        // 设置模拟相关性分数。
                        .relevanceScore(score(request == null ? "" : request.getQuery(), candidate.getText()))
                        // 设置文档文本。
                        .documentText(candidate.getText())
                        // 构建对象。
                        .build())
                // 按相关性分数倒序。
                .sorted(Comparator.comparing(RerankScoredItem::getRelevanceScore, Comparator.reverseOrder()))
                // 截断到 topN（保留数量）。
                .limit(topN)
                // 收集为列表。
                .toList();
        // 构建模拟响应。
        return RerankModelResponse.builder()
                // 设置供应商。
                .provider("mock")
                // 设置模型名称。
                .modelName("mock-rerank")
                // 设置结果。
                .results(results)
                // 设置估算输入 Token（词元）。
                .promptTokens(estimateTokens(request))
                // 设置估算总 Token（词元）。
                .totalTokens(estimateTokens(request))
                // 构建响应。
                .build();
    }

    /**
     * score（打分）用于根据查询词命中情况生成模拟分数。
     *
     * @param query 查询文本
     * @param text 候选文本
     * @return 模拟相关性分数
     */
    private double score(String query, String text) {
        // 如果候选文本为空，则返回 0。
        if (text == null || text.isBlank()) {
            // 返回最低分。
            return 0.0;
        }
        // 初始化分数。
        double score = 0.1;
        // 遍历查询中的字符。
        for (int index = 0; index < query.length(); index++) {
            // 读取当前字符。
            String character = String.valueOf(query.charAt(index));
            // 如果候选文本包含该字符，则加分。
            if (!character.isBlank() && text.contains(character)) {
                // 累加命中分。
                score += 0.01;
            }
        }
        // 返回最高不超过 1 的分数。
        return Math.min(score, 1.0);
    }

    /**
     * resolveTopN（解析保留数量）用于处理请求数量边界。
     *
     * @param request 重排序请求
     * @param candidateSize 候选数量
     * @return 保留数量
     */
    private int resolveTopN(RerankModelRequest request, int candidateSize) {
        // 读取请求中的 topN（保留数量）。
        Integer topN = request == null ? null : request.getTopN();
        // 如果请求没有设置，则最多返回候选数量。
        int resolvedTopN = topN == null || topN <= 0 ? candidateSize : topN;
        // 返回合法数量。
        return Math.max(0, Math.min(resolvedTopN, candidateSize));
    }

    /**
     * estimateTokens（估算词元）用于本地模拟用量。
     *
     * @param request 重排序请求
     * @return 估算词元数量
     */
    private int estimateTokens(RerankModelRequest request) {
        // 如果请求为空，则返回 0。
        if (request == null) {
            // 返回 0。
            return 0;
        }
        // 统计查询文本长度。
        int length = request.getQuery() == null ? 0 : request.getQuery().length();
        // 统计候选文本长度。
        if (request.getCandidates() != null) {
            // 累加候选文本长度。
            length += request.getCandidates().stream()
                    // 读取候选文本长度。
                    .mapToInt(candidate -> candidate.getText() == null ? 0 : candidate.getText().length())
                    // 求和。
                    .sum();
        }
        // 中文场景按两个字符约等于一个 Token（词元）估算。
        return Math.max(1, (int) Math.ceil(length / 2.0));
    }
}
