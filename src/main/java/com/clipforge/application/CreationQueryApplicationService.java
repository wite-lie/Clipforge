package com.clipforge.application;

import com.clipforge.agent.CreationPlanResult;
import com.clipforge.api.dto.CreationEvaluationResultView;
import com.clipforge.api.dto.CreationPlanVersionView;
import com.clipforge.api.dto.CreationRevisionResultView;
import com.clipforge.api.dto.ModelCallRecordView;
import com.clipforge.api.dto.QueryModelCallRecordsResponse;
import com.clipforge.api.dto.QueryPlanResponse;
import com.clipforge.api.dto.QueryQualityReportResponse;
import com.clipforge.api.dto.QueryTraceResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.AgentTraceEventEntity;
import com.clipforge.infra.persistence.entity.CreationEvaluationResultEntity;
import com.clipforge.infra.persistence.entity.CreationPlanVersionEntity;
import com.clipforge.infra.persistence.entity.CreationResultEntity;
import com.clipforge.infra.persistence.entity.CreationRevisionResultEntity;
import com.clipforge.infra.persistence.entity.ModelCallRecordEntity;
import com.clipforge.infra.persistence.repository.AgentTraceEventRepository;
import com.clipforge.infra.persistence.repository.CreationEvaluationResultRepository;
import com.clipforge.infra.persistence.repository.CreationPlanVersionRepository;
import com.clipforge.infra.persistence.repository.CreationResultRepository;
import com.clipforge.infra.persistence.repository.CreationRevisionResultRepository;
import com.clipforge.infra.persistence.repository.ModelCallRecordRepository;
import com.clipforge.model.cost.ModelCallCost;
import com.clipforge.model.cost.ModelCostCalculator;
import com.clipforge.trace.TraceEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * CreationQueryApplicationService（创作查询应用服务）。
 *
 * <p>该服务负责按 runId（运行编号）查询已经持久化的创作方案和 Trace（执行链路追踪）。
 */
@Service
@RequiredArgsConstructor
public class CreationQueryApplicationService {

    /** creationResultRepositoryProvider（创作结果仓储提供器）用于在 db（数据库）Profile（配置环境）启用时获取仓储 Bean（组件）。 */
    private final ObjectProvider<CreationResultRepository> creationResultRepositoryProvider;

    /** agentTraceEventRepositoryProvider（智能体执行链路事件仓储提供器）用于在 db（数据库）Profile（配置环境）启用时获取仓储 Bean（组件）。 */
    private final ObjectProvider<AgentTraceEventRepository> agentTraceEventRepositoryProvider;

    /** modelCallRecordRepositoryProvider（模型调用记录仓储提供器）用于在 db（数据库）Profile（配置环境）启用时获取仓储 Bean（组件）。 */
    private final ObjectProvider<ModelCallRecordRepository> modelCallRecordRepositoryProvider;

    /** creationPlanVersionRepositoryProvider（创作方案版本仓储提供器）用于读取质量版本链。 */
    private final ObjectProvider<CreationPlanVersionRepository> creationPlanVersionRepositoryProvider;

    /** creationEvaluationResultRepositoryProvider（创作评估结果仓储提供器）用于读取结构化评估结果。 */
    private final ObjectProvider<CreationEvaluationResultRepository> creationEvaluationResultRepositoryProvider;

    /** creationRevisionResultRepositoryProvider（创作修正结果仓储提供器）用于读取修正前后版本关系。 */
    private final ObjectProvider<CreationRevisionResultRepository> creationRevisionResultRepositoryProvider;

    /** objectMapper（对象映射器）用于把 JSON（对象表示格式）字符串还原为 Java（爪哇语言）列表。 */
    private final ObjectMapper objectMapper;

    /** modelCostCalculator（模型成本计算器）用于把模型调用 Token（词元）换算为估算成本。 */
    private final ModelCostCalculator modelCostCalculator;

    /**
     * queryPlanByRunId（按运行编号查询创作方案）用于读取数据库中的创作方案结果。
     *
     * @param runId Agent（智能体）运行编号
     * @return 查询创作方案响应
     */
    public QueryPlanResponse queryPlanByRunId(String runId) {
        // 获取创作结果仓储，如果当前环境未启用 db（数据库）Profile（配置环境），则抛出明确业务异常。
        CreationResultRepository repository = requiredCreationResultRepository();
        // 按 runId（运行编号）查询创作结果实体。
        CreationResultEntity entity = repository.findByRunId(runId)
                // 如果查不到数据，抛出数据不存在业务异常。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到创作方案，runId：" + runId));
        // 把数据库实体还原为领域层创作方案结果。
        CreationPlanResult result = toCreationPlanResult(entity);
        // 使用 Builder（建造者模式）组装查询响应。
        return QueryPlanResponse.builder()
                // 设置 Agent（智能体）运行编号。
                .runId(entity.getRunId())
                // 设置创作任务编号。
                .taskId(entity.getTaskId())
                // 设置创作结果编号。
                .resultId(entity.getResultId())
                // 设置创作方案结果。
                .result(result)
                // 构建查询响应。
                .build();
    }

    /**
     * queryTraceByRunId（按运行编号查询执行链路）用于读取数据库中的执行链路事件。
     *
     * @param runId Agent（智能体）运行编号
     * @return 查询执行链路响应
     */
    public QueryTraceResponse queryTraceByRunId(String runId) {
        // 获取执行链路事件仓储，如果当前环境未启用 db（数据库）Profile（配置环境），则抛出明确业务异常。
        AgentTraceEventRepository repository = requiredAgentTraceEventRepository();
        // 按 runId（运行编号）查询执行链路事件实体列表。
        List<AgentTraceEventEntity> entities = repository.listByRunId(runId);
        // 如果执行链路为空，说明没有找到对应运行记录。
        if (entities.isEmpty()) {
            // 抛出数据不存在业务异常。
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到执行链路，runId：" + runId);
        }
        // 把数据库实体列表转换为领域层 TraceEvent（执行链路事件）列表。
        List<TraceEvent> traceEvents = entities.stream()
                // 转换单条执行链路事件。
                .map(this::toTraceEvent)
                // 收集为列表。
                .toList();
        // 使用 Builder（建造者模式）组装查询响应。
        return QueryTraceResponse.builder()
                // 设置 Agent（智能体）运行编号。
                .runId(runId)
                // 设置执行链路事件列表。
                .traceEvents(traceEvents)
                // 构建查询响应。
                .build();
    }

    /**
     * queryModelCallRecordsByRunId（按运行编号查询模型调用记录）用于读取一次运行的模型 Token（词元）消耗明细。
     *
     * @param runId Agent（智能体）运行编号
     * @return 查询模型调用记录响应
     */
    public QueryModelCallRecordsResponse queryModelCallRecordsByRunId(String runId) {
        // 获取模型调用记录仓储，如果当前环境未启用 db（数据库）Profile（配置环境），则抛出明确业务异常。
        ModelCallRecordRepository repository = requiredModelCallRecordRepository();
        // 按 runId（运行编号）查询模型调用记录。
        List<ModelCallRecordEntity> entities = repository.listByRunId(runId);
        // 如果记录为空，说明没有找到模型调用明细。
        if (entities.isEmpty()) {
            // 抛出数据不存在业务异常。
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到模型调用记录，runId：" + runId);
        }
        // 转换接口视图列表。
        List<ModelCallRecordView> records = entities.stream()
                // 转换单条模型调用记录。
                .map(this::toModelCallRecordView)
                // 收集为列表。
                .toList();
        // 计算本次运行的总成本。
        ModelCallCost totalCost = modelCostCalculator.calculateTotal(entities);
        // 使用 Builder（建造者模式）组装响应。
        return QueryModelCallRecordsResponse.builder()
                // 设置运行编号。
                .runId(runId)
                // 汇总输入 Token（词元）。
                .totalPromptTokens(sumInteger(entities.stream().map(ModelCallRecordEntity::getPromptTokens).toList()))
                // 汇总输出 Token（词元）。
                .totalCompletionTokens(sumInteger(entities.stream().map(ModelCallRecordEntity::getCompletionTokens).toList()))
                // 汇总总 Token（词元）。
                .totalTokens(sumInteger(entities.stream().map(ModelCallRecordEntity::getTotalTokens).toList()))
                // 设置估算输入成本。
                .estimatedInputCost(totalCost.getInputCost())
                // 设置估算输出成本。
                .estimatedOutputCost(totalCost.getOutputCost())
                // 设置估算总成本。
                .estimatedTotalCost(totalCost.getTotalCost())
                // 设置币种。
                .currency(totalCost.getCurrency())
                // 设置是否全部命中价格。
                .priceMatched(totalCost.getPriceMatched())
                // 设置明细列表。
                .records(records)
                // 构建响应。
                .build();
    }

    /**
     * queryQualityReportByRunId（按运行编号查询质量复盘）用于聚合最终方案、评估事件、修正事件和模型成本。
     *
     * @param runId Agent（智能体）运行编号
     * @return 查询质量复盘响应
     */
    public QueryQualityReportResponse queryQualityReportByRunId(String runId) {
        // 获取创作结果仓储。
        CreationResultRepository resultRepository = requiredCreationResultRepository();
        // 获取执行链路仓储。
        AgentTraceEventRepository traceRepository = requiredAgentTraceEventRepository();
        // 获取模型调用记录仓储。
        ModelCallRecordRepository modelCallRecordRepository = requiredModelCallRecordRepository();
        // 查询最终创作结果。
        CreationResultEntity resultEntity = resultRepository.findByRunId(runId)
                // 如果查不到结果，抛出数据不存在异常。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到创作方案，runId：" + runId));
        // 查询执行链路事件。
        List<AgentTraceEventEntity> traceEntities = traceRepository.listByRunId(runId);
        // 如果执行链路为空，说明没有复盘基础。
        if (traceEntities.isEmpty()) {
            // 抛出数据不存在异常。
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到执行链路，runId：" + runId);
        }
        // 查询模型调用记录，质量复盘允许为空，兼容纯规则或早期运行记录。
        List<ModelCallRecordEntity> modelCallEntities = modelCallRecordRepository.listByRunId(runId);
        // 转换执行链路事件。
        List<TraceEvent> traceEvents = traceEntities.stream()
                // 转换单条事件。
                .map(this::toTraceEvent)
                // 收集为列表。
                .toList();
        // 筛选评估事件。
        List<TraceEvent> evaluationEvents = traceEvents.stream()
                // 匹配评估相关事件。
                .filter(event -> event.getEventType() != null && event.getEventType().startsWith("EVALUATION"))
                // 收集为列表。
                .toList();
        // 筛选修正事件。
        List<TraceEvent> revisionEvents = traceEvents.stream()
                // 匹配修正相关事件。
                .filter(event -> event.getEventType() != null && event.getEventType().startsWith("REVISION"))
                // 收集为列表。
                .toList();
        // 转换模型调用记录视图。
        List<ModelCallRecordView> modelCallRecords = modelCallEntities.stream()
                // 转换单条模型调用记录。
                .map(this::toModelCallRecordView)
                // 收集为列表。
                .toList();
        // 查询方案版本链。
        List<CreationPlanVersionEntity> versionEntities = optionalCreationPlanVersionRepository().map(repository -> repository.listByRunId(runId)).orElse(List.of());
        // 查询结构化评估结果。
        List<CreationEvaluationResultEntity> evaluationResultEntities = optionalCreationEvaluationResultRepository().map(repository -> repository.listByRunId(runId)).orElse(List.of());
        // 查询修正前后关系。
        List<CreationRevisionResultEntity> revisionResultEntities = optionalCreationRevisionResultRepository().map(repository -> repository.listByRunId(runId)).orElse(List.of());
        // 转换版本视图。
        List<CreationPlanVersionView> versions = versionEntities.stream().map(this::toCreationPlanVersionView).toList();
        // 转换评估视图。
        List<CreationEvaluationResultView> evaluations = evaluationResultEntities.stream().map(this::toCreationEvaluationResultView).toList();
        // 转换修正视图。
        List<CreationRevisionResultView> revisions = revisionResultEntities.stream().map(this::toCreationRevisionResultView).toList();
        // 计算本次运行的总成本。
        ModelCallCost totalCost = modelCostCalculator.calculateTotal(modelCallEntities);
        // 使用 Builder（建造者模式）组装质量复盘响应。
        return QueryQualityReportResponse.builder()
                // 设置运行编号。
                .runId(runId)
                // 设置任务编号。
                .taskId(resultEntity.getTaskId())
                // 设置结果编号。
                .resultId(resultEntity.getResultId())
                // 设置最终结果。
                .finalResult(toCreationPlanResult(resultEntity))
                // 设置最终评分。
                .finalScore(resultEntity.getScore())
                // 设置是否触发评估。
                .evaluationTriggered(!evaluationEvents.isEmpty())
                // 设置是否触发修正。
                .revisionTriggered(hasEvent(traceEvents, "REVISION_STARTED"))
                // 设置修正次数。
                .revisionCount(countEvent(traceEvents, "REVISION_STARTED"))
                // 设置总输入 Token（词元）。
                .totalPromptTokens(sumInteger(modelCallEntities.stream().map(ModelCallRecordEntity::getPromptTokens).toList()))
                // 设置总输出 Token（词元）。
                .totalCompletionTokens(sumInteger(modelCallEntities.stream().map(ModelCallRecordEntity::getCompletionTokens).toList()))
                // 设置总 Token（词元）。
                .totalTokens(sumInteger(modelCallEntities.stream().map(ModelCallRecordEntity::getTotalTokens).toList()))
                // 设置按用途汇总 Token（词元）。
                .tokensByPurpose(sumTokensByPurpose(modelCallEntities))
                // 设置估算输入成本。
                .estimatedInputCost(totalCost.getInputCost())
                // 设置估算输出成本。
                .estimatedOutputCost(totalCost.getOutputCost())
                // 设置估算总成本。
                .estimatedTotalCost(totalCost.getTotalCost())
                // 设置币种。
                .currency(totalCost.getCurrency())
                // 设置是否全部命中价格。
                .priceMatched(totalCost.getPriceMatched())
                // 设置按用途汇总成本。
                .costByPurpose(sumCostByPurpose(modelCallEntities))
                // 设置最终版本编号。
                .finalVersionId(resolveFinalVersionId(versionEntities))
                // 设置方案版本列表。
                .versions(versions)
                // 设置结构化评估结果列表。
                .evaluations(evaluations)
                // 设置修正记录列表。
                .revisions(revisions)
                // 设置评估事件。
                .evaluationEvents(evaluationEvents)
                // 设置修正事件。
                .revisionEvents(revisionEvents)
                // 设置模型调用明细。
                .modelCallRecords(modelCallRecords)
                // 构建响应。
                .build();
    }

    /**
     * requiredCreationResultRepository（获取必需的创作结果仓储）用于处理未启用持久化的场景。
     *
     * @return 创作结果仓储
     */
    private CreationResultRepository requiredCreationResultRepository() {
        // 从 Spring（Spring 框架）容器中获取创作结果仓储。
        CreationResultRepository repository = creationResultRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常，提示调用方需要启用数据库环境。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "查询接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * requiredAgentTraceEventRepository（获取必需的执行链路仓储）用于处理未启用持久化的场景。
     *
     * @return 执行链路事件仓储
     */
    private AgentTraceEventRepository requiredAgentTraceEventRepository() {
        // 从 Spring（Spring 框架）容器中获取执行链路事件仓储。
        AgentTraceEventRepository repository = agentTraceEventRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常，提示调用方需要启用数据库环境。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "查询接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * requiredModelCallRecordRepository（获取必需的模型调用记录仓储）用于处理未启用持久化的场景。
     *
     * @return 模型调用记录仓储
     */
    private ModelCallRecordRepository requiredModelCallRecordRepository() {
        // 从 Spring（Spring 框架）容器中获取模型调用记录仓储。
        ModelCallRecordRepository repository = modelCallRecordRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常，提示调用方需要启用数据库环境。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "模型调用记录查询接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * optionalCreationPlanVersionRepository（可选创作方案版本仓储）用于兼容旧环境或未执行迁移脚本的测试。
     */
    private java.util.Optional<CreationPlanVersionRepository> optionalCreationPlanVersionRepository() {
        return java.util.Optional.ofNullable(creationPlanVersionRepositoryProvider.getIfAvailable());
    }

    /**
     * optionalCreationEvaluationResultRepository（可选创作评估结果仓储）用于兼容旧环境或未执行迁移脚本的测试。
     */
    private java.util.Optional<CreationEvaluationResultRepository> optionalCreationEvaluationResultRepository() {
        return java.util.Optional.ofNullable(creationEvaluationResultRepositoryProvider.getIfAvailable());
    }

    /**
     * optionalCreationRevisionResultRepository（可选创作修正结果仓储）用于兼容旧环境或未执行迁移脚本的测试。
     */
    private java.util.Optional<CreationRevisionResultRepository> optionalCreationRevisionResultRepository() {
        return java.util.Optional.ofNullable(creationRevisionResultRepositoryProvider.getIfAvailable());
    }

    /**
     * toModelCallRecordView（转模型调用记录视图）用于把数据库实体转换为接口响应对象。
     *
     * @param entity 模型调用记录实体
     * @return 模型调用记录视图
     */
    private ModelCallRecordView toModelCallRecordView(ModelCallRecordEntity entity) {
        // 计算单次模型调用的估算成本。
        ModelCallCost cost = modelCostCalculator.calculate(entity);
        // 使用 Builder（建造者模式）组装模型调用记录视图。
        return ModelCallRecordView.builder()
                // 设置记录编号。
                .recordId(entity.getRecordId())
                // 设置供应商。
                .provider(entity.getProvider())
                // 设置模型名称。
                .modelName(entity.getModelName())
                // 设置调用用途。
                .purpose(entity.getPurpose())
                // 设置输入 Token（词元）。
                .promptTokens(entity.getPromptTokens())
                // 设置输出 Token（词元）。
                .completionTokens(entity.getCompletionTokens())
                // 设置总 Token（词元）。
                .totalTokens(entity.getTotalTokens())
                // 设置估算输入成本。
                .estimatedInputCost(cost.getInputCost())
                // 设置估算输出成本。
                .estimatedOutputCost(cost.getOutputCost())
                // 设置估算总成本。
                .estimatedTotalCost(cost.getTotalCost())
                // 设置币种。
                .currency(cost.getCurrency())
                // 设置是否命中价格。
                .priceMatched(cost.getPriceMatched())
                // 设置调用耗时。
                .latencyMs(entity.getLatencyMs())
                // 设置成功状态。
                .success(entity.getSuccess())
                // 设置错误码。
                .errorCode(entity.getErrorCode())
                // 设置错误消息。
                .errorMessage(entity.getErrorMessage())
                // 构建视图对象。
                .build();
    }

    /**
     * sumInteger（整数求和）用于汇总可能为空的 Token（词元）数。
     *
     * @param values 整数列表
     * @return 汇总值
     */
    private Integer sumInteger(List<Integer> values) {
        // 过滤空值后求和。
        return values.stream()
                // 过滤空值。
                .filter(value -> value != null)
                // 转为 int（整数）。
                .mapToInt(Integer::intValue)
                // 求和。
                .sum();
    }

    /**
     * sumTokensByPurpose（按用途汇总词元）用于观察生成、评估和修正分别消耗多少 Token（词元）。
     *
     * @param entities 模型调用记录实体列表
     * @return 按用途汇总的 Token（词元）Map（映射）
     */
    private Map<String, Integer> sumTokensByPurpose(List<ModelCallRecordEntity> entities) {
        // 创建有序 Map（映射），让接口输出更稳定。
        Map<String, Integer> result = new LinkedHashMap<>();
        // 遍历模型调用记录。
        for (ModelCallRecordEntity entity : entities) {
            // 读取调用用途，空值时使用 UNKNOWN（未知）。
            String purpose = entity.getPurpose() == null || entity.getPurpose().isBlank() ? "UNKNOWN" : entity.getPurpose();
            // 读取总 Token（词元），空值按 0 处理。
            Integer totalTokens = entity.getTotalTokens() == null ? 0 : entity.getTotalTokens();
            // 累加当前用途的 Token（词元）。
            result.put(purpose, result.getOrDefault(purpose, 0) + totalTokens);
        }
        // 返回汇总结果。
        return result;
    }

    /**
     * sumCostByPurpose（按用途汇总成本）用于观察生成、评估、修正、视觉理解、嵌入和重排序分别消耗多少成本。
     *
     * @param entities 模型调用记录实体列表
     * @return 按用途汇总的估算成本 Map（映射）
     */
    private Map<String, BigDecimal> sumCostByPurpose(List<ModelCallRecordEntity> entities) {
        // 创建有序 Map（映射），保持接口输出稳定。
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        // 遍历模型调用记录。
        for (ModelCallRecordEntity entity : entities) {
            // 读取调用用途，空值时使用 UNKNOWN（未知）。
            String purpose = entity.getPurpose() == null || entity.getPurpose().isBlank() ? "UNKNOWN" : entity.getPurpose();
            // 计算当前记录成本。
            ModelCallCost cost = modelCostCalculator.calculate(entity);
            // 累加到该用途的成本桶。
            result.put(purpose, result.getOrDefault(purpose, BigDecimal.ZERO).add(cost.getTotalCost()));
        }
        // 返回汇总结果。
        return result;
    }

    /**
     * resolveFinalVersionId（解析最终版本编号）用于从版本链里找到最终采用的方案版本。
     */
    private String resolveFinalVersionId(List<CreationPlanVersionEntity> entities) {
        return entities.stream()
                .filter(entity -> Boolean.TRUE.equals(entity.getFinalVersion()))
                .map(CreationPlanVersionEntity::getVersionId)
                .findFirst()
                .orElse(null);
    }

    /**
     * toCreationPlanVersionView（转方案版本视图）用于把数据库版本实体转换为接口响应。
     */
    private CreationPlanVersionView toCreationPlanVersionView(CreationPlanVersionEntity entity) {
        return CreationPlanVersionView.builder()
                .versionId(entity.getVersionId())
                .versionNo(entity.getVersionNo())
                .versionType(entity.getVersionType())
                .sourceVersionId(entity.getSourceVersionId())
                .score(entity.getScore())
                .createdByPurpose(entity.getCreatedByPurpose())
                .modelCallRecordId(entity.getModelCallRecordId())
                .finalVersion(entity.getFinalVersion())
                .planJson(entity.getPlanJson())
                .createTime(entity.getCreateTime())
                .build();
    }

    /**
     * toCreationEvaluationResultView（转评估结果视图）用于把数据库评估实体转换为接口响应。
     */
    private CreationEvaluationResultView toCreationEvaluationResultView(CreationEvaluationResultEntity entity) {
        return CreationEvaluationResultView.builder()
                .evaluationId(entity.getEvaluationId())
                .versionId(entity.getVersionId())
                .evaluatorType(entity.getEvaluatorType())
                .score(entity.getScore())
                .dimensionScoresJson(entity.getDimensionScoresJson())
                .strengthsJson(entity.getStrengthsJson())
                .weaknessesJson(entity.getWeaknessesJson())
                .revisionRequired(entity.getRevisionRequired())
                .revisionAdviceJson(entity.getRevisionAdviceJson())
                .passed(entity.getPassed())
                .modelCallRecordId(entity.getModelCallRecordId())
                .createTime(entity.getCreateTime())
                .build();
    }

    /**
     * toCreationRevisionResultView（转修正结果视图）用于把数据库修正实体转换为接口响应。
     */
    private CreationRevisionResultView toCreationRevisionResultView(CreationRevisionResultEntity entity) {
        return CreationRevisionResultView.builder()
                .revisionId(entity.getRevisionId())
                .sourceVersionId(entity.getSourceVersionId())
                .targetVersionId(entity.getTargetVersionId())
                .evaluationId(entity.getEvaluationId())
                .revisionReason(entity.getRevisionReason())
                .revisionSummary(entity.getRevisionSummary())
                .modelCallRecordId(entity.getModelCallRecordId())
                .createTime(entity.getCreateTime())
                .build();
    }

    /**
     * hasEvent（是否存在事件）用于判断某类 Trace（执行链路追踪）事件是否发生。
     *
     * @param events 执行链路事件列表
     * @param eventType 事件类型
     * @return 是否存在该事件
     */
    private boolean hasEvent(List<TraceEvent> events, String eventType) {
        // 判断是否存在指定事件类型。
        return events.stream().anyMatch(event -> eventType.equals(event.getEventType()));
    }

    /**
     * countEvent（统计事件次数）用于统计修正等关键事件发生次数。
     *
     * @param events 执行链路事件列表
     * @param eventType 事件类型
     * @return 事件次数
     */
    private int countEvent(List<TraceEvent> events, String eventType) {
        // 统计指定事件类型数量。
        return (int) events.stream().filter(event -> eventType.equals(event.getEventType())).count();
    }

    /**
     * toCreationPlanResult（转创作方案结果）用于把数据库实体还原为接口可返回的领域对象。
     *
     * @param entity 创作结果实体
     * @return 创作方案结果
     */
    private CreationPlanResult toCreationPlanResult(CreationResultEntity entity) {
        // 使用 Builder（建造者模式）组装创作方案结果。
        return CreationPlanResult.builder()
                // 设置创作主题。
                .theme(entity.getTheme())
                // 设置目标受众。
                .targetAudience(entity.getTargetAudience())
                // 设置开头钩子。
                .hook(entity.getHook())
                // 还原分镜脚本列表。
                .storyboard(parseStringList(entity.getStoryboardJson()))
                // 设置旁白脚本。
                .voiceoverScript(entity.getVoiceoverScript())
                // 还原字幕建议列表。
                .subtitleSuggestions(parseStringList(entity.getSubtitleSuggestionsJson()))
                // 还原标题候选列表。
                .titleCandidates(parseStringList(entity.getTitleCandidatesJson()))
                // 设置发布文案。
                .publishCopy(entity.getPublishCopy())
                // 还原话题标签列表。
                .hashtags(parseStringList(entity.getHashtagsJson()))
                // 设置效果评分，数据库为空时使用 0 作为兜底。
                .score(entity.getScore() == null ? 0 : entity.getScore())
                // 还原改进建议列表。
                .improvementSuggestions(parseStringList(entity.getImprovementSuggestionsJson()))
                // 构建创作方案结果。
                .build();
    }

    /**
     * toTraceEvent（转执行链路事件）用于把数据库实体转换为领域层 TraceEvent（执行链路事件）。
     *
     * @param entity 执行链路事件实体
     * @return 执行链路事件
     */
    private TraceEvent toTraceEvent(AgentTraceEventEntity entity) {
        // 创建 TraceEvent（执行链路事件）并把 LocalDateTime（本地时间）补充为系统默认时区偏移。
        return new TraceEvent(
                // 设置 Agent（智能体）运行编号。
                entity.getRunId(),
                // 设置事件类型。
                entity.getEventType(),
                // 设置事件消息。
                entity.getMessage(),
                // 设置事件详情。
                entity.getDetail(),
                // 设置事件发生时间，当前数据库字段没有时区，按系统默认时区推导偏移。
                entity.getOccurredAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );
    }

    /**
     * parseStringList（解析字符串列表）用于把 JSON（对象表示格式）数组字符串还原为字符串列表。
     *
     * @param json JSON（对象表示格式）数组字符串
     * @return 字符串列表
     */
    private List<String> parseStringList(String json) {
        // 如果数据库字段为空，返回空列表，避免调用方处理空指针。
        if (json == null || json.isBlank()) {
            // 返回不可变空列表。
            return List.of();
        }
        try {
            // 使用 Jackson（JSON 处理工具）把 JSON（对象表示格式）数组解析为字符串列表。
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException exception) {
            // 解析失败表示持久化数据格式异常，统一按内部错误处理。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创作方案 JSON 字段解析失败", exception);
        }
    }
}
