package com.clipforge.application;

import com.clipforge.contract.knowledge.ModelUsageGroupView;
import com.clipforge.contract.knowledge.ModelUsageSummaryResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.memory.infrastructure.persistence.entity.ModelCallRecordEntity;
import com.clipforge.memory.infrastructure.persistence.repository.ModelCallRecordRepository;
import com.clipforge.model.cost.ModelCallCost;
import com.clipforge.model.cost.ModelCostCalculator;
import com.clipforge.observability.usage.ModelCallRecordQuery;
import com.clipforge.observability.usage.ModelUsageGroupBy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * ModelUsageQueryApplicationService（模型用量查询应用服务）。
 *
 * <p>该服务基于 cf_model_call_record（模型调用记录表）聚合 Token（词元）、耗时和估算成本，为成本治理和质量复盘提供查询能力。
 */
@Service
@RequiredArgsConstructor
public class ModelUsageQueryApplicationService {

    /** modelCallRecordRepositoryProvider（模型调用记录仓储提供器）用于查询模型调用明细。 */
    private final ObjectProvider<ModelCallRecordRepository> modelCallRecordRepositoryProvider;

    /** modelCostCalculator（模型成本计算器）用于按配置单价估算成本。 */
    private final ModelCostCalculator modelCostCalculator;

    /**
     * querySummary（查询汇总）用于按条件和分组维度统计模型用量。
     *
     * @param query 查询条件
     * @param groupBy 分组维度
     * @return 模型用量汇总响应
     */
    public ModelUsageSummaryResponse querySummary(ModelCallRecordQuery query, ModelUsageGroupBy groupBy) {
        // 查询符合条件的模型调用记录。
        List<ModelCallRecordEntity> records = requiredModelCallRecordRepository().listByQuery(query);
        // 解析分组维度，空值时不分组。
        ModelUsageGroupBy effectiveGroupBy = groupBy == null ? ModelUsageGroupBy.NONE : groupBy;
        // 计算整体成本。
        ModelCallCost totalCost = modelCostCalculator.calculateTotal(records);
        // 构造汇总响应。
        return ModelUsageSummaryResponse.builder()
                .groupBy(effectiveGroupBy.name())
                .totalCallCount(records.size())
                .successCallCount(successCount(records))
                .failedCallCount(records.size() - successCount(records))
                .totalPromptTokens(sumPromptTokens(records))
                .totalCompletionTokens(sumCompletionTokens(records))
                .totalTokens(sumTotalTokens(records))
                .totalLatencyMs(sumLatency(records))
                .averageLatencyMs(averageLatency(records))
                .estimatedInputCost(totalCost.getInputCost())
                .estimatedOutputCost(totalCost.getOutputCost())
                .estimatedTotalCost(totalCost.getTotalCost())
                .currency(totalCost.getCurrency())
                .priceMatched(totalCost.getPriceMatched())
                .groups(buildGroups(records, effectiveGroupBy))
                .build();
    }

    /**
     * buildGroups（构建分组）用于按指定维度聚合模型调用记录。
     */
    private List<ModelUsageGroupView> buildGroups(List<ModelCallRecordEntity> records, ModelUsageGroupBy groupBy) {
        if (groupBy == ModelUsageGroupBy.NONE) {
            return List.of();
        }
        Map<String, List<ModelCallRecordEntity>> groups = new LinkedHashMap<>();
        for (ModelCallRecordEntity record : records) {
            String key = groupKey(record, groupBy);
            groups.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(record);
        }
        return groups.entrySet().stream()
                .map(entry -> toGroupView(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * toGroupView（转分组视图）用于把一组模型调用记录转换为接口分组结果。
     */
    private ModelUsageGroupView toGroupView(String groupKey, List<ModelCallRecordEntity> records) {
        ModelCallCost cost = modelCostCalculator.calculateTotal(records);
        return ModelUsageGroupView.builder()
                .groupKey(groupKey)
                .groupName(groupKey)
                .callCount(records.size())
                .successCallCount(successCount(records))
                .failedCallCount(records.size() - successCount(records))
                .totalPromptTokens(sumPromptTokens(records))
                .totalCompletionTokens(sumCompletionTokens(records))
                .totalTokens(sumTotalTokens(records))
                .totalLatencyMs(sumLatency(records))
                .averageLatencyMs(averageLatency(records))
                .estimatedInputCost(cost.getInputCost())
                .estimatedOutputCost(cost.getOutputCost())
                .estimatedTotalCost(cost.getTotalCost())
                .currency(cost.getCurrency())
                .priceMatched(cost.getPriceMatched())
                .build();
    }

    /**
     * groupKey（分组键）用于根据分组维度生成稳定文本键。
     */
    private String groupKey(ModelCallRecordEntity record, ModelUsageGroupBy groupBy) {
        return switch (groupBy) {
            case RUN -> valueOrUnknown(record.getRunId());
            case PROJECT -> valueOrUnknown(record.getProjectId());
            case CONVERSATION -> valueOrUnknown(record.getConversationId());
            case DATE -> record.getCreateTime() == null ? "UNKNOWN" : record.getCreateTime().toLocalDate().toString();
            case PROVIDER -> valueOrUnknown(record.getProvider());
            case MODEL -> valueOrUnknown(record.getModelName());
            case PURPOSE -> valueOrUnknown(record.getPurpose());
            case PROVIDER_MODEL_PURPOSE -> valueOrUnknown(record.getProvider()) + "/" + valueOrUnknown(record.getModelName()) + "/" + valueOrUnknown(record.getPurpose());
            case NONE -> "ALL";
        };
    }

    /**
     * successCount（成功数量）用于统计成功调用次数。
     */
    private int successCount(List<ModelCallRecordEntity> records) {
        return (int) records.stream().filter(record -> Boolean.TRUE.equals(record.getSuccess())).count();
    }

    /**
     * sumPromptTokens（汇总输入词元）用于统计 Prompt（提示词）消耗。
     */
    private int sumPromptTokens(List<ModelCallRecordEntity> records) {
        return records.stream().map(ModelCallRecordEntity::getPromptTokens).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    /**
     * sumCompletionTokens（汇总输出词元）用于统计模型回答消耗。
     */
    private int sumCompletionTokens(List<ModelCallRecordEntity> records) {
        return records.stream().map(ModelCallRecordEntity::getCompletionTokens).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    /**
     * sumTotalTokens（汇总总词元）用于统计总 Token（词元）消耗。
     */
    private int sumTotalTokens(List<ModelCallRecordEntity> records) {
        return records.stream().map(ModelCallRecordEntity::getTotalTokens).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    /**
     * sumLatency（汇总耗时）用于统计总模型调用耗时。
     */
    private long sumLatency(List<ModelCallRecordEntity> records) {
        return records.stream().map(ModelCallRecordEntity::getLatencyMs).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
    }

    /**
     * averageLatency（平均耗时）用于统计平均模型调用耗时。
     */
    private long averageLatency(List<ModelCallRecordEntity> records) {
        if (records.isEmpty()) {
            return 0L;
        }
        return sumLatency(records) / records.size();
    }

    /**
     * valueOrUnknown（空值转未知）用于稳定接口分组键。
     */
    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value;
    }

    /**
     * requiredModelCallRecordRepository（获取必需模型调用记录仓储）用于处理未启用数据库的场景。
     */
    private ModelCallRecordRepository requiredModelCallRecordRepository() {
        ModelCallRecordRepository repository = modelCallRecordRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "模型用量统计需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }
}
