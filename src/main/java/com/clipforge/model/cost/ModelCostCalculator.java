package com.clipforge.model.cost;

import com.clipforge.infra.persistence.entity.ModelCallRecordEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ModelCostCalculator（模型成本计算器）。
 *
 * <p>该组件根据模型调用记录和配置单价估算成本，所有金额只用于项目复盘和成本治理，不替代供应商账单。
 */
@Component
public class ModelCostCalculator {

    /** THOUSAND（一千）用于把 Token（词元）数量换算成每千 Token（词元）价格。 */
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    /** modelCostProperties（模型成本配置）用于读取模型单价。 */
    private final ModelCostProperties modelCostProperties;

    /**
     * ModelCostCalculator（模型成本计算器）构造函数。
     *
     * @param modelCostProperties 模型成本配置
     */
    public ModelCostCalculator(ModelCostProperties modelCostProperties) {
        this.modelCostProperties = modelCostProperties;
    }

    /**
     * calculate（计算）用于估算单条模型调用记录的成本。
     *
     * @param entity 模型调用记录实体
     * @return 模型调用成本
     */
    public ModelCallCost calculate(ModelCallRecordEntity entity) {
        ModelCostPrice price = findPrice(entity == null ? null : entity.getProvider(), entity == null ? null : entity.getModelName());
        boolean matched = price != null;
        ModelCostPrice effectivePrice = matched ? price : new ModelCostPrice();
        BigDecimal inputCost = cost(entity == null ? null : entity.getPromptTokens(), effectivePrice.getInputPricePer1kTokens());
        BigDecimal outputCost = cost(entity == null ? null : entity.getCompletionTokens(), effectivePrice.getOutputPricePer1kTokens());
        return ModelCallCost.builder()
                .inputCost(inputCost)
                .outputCost(outputCost)
                .totalCost(inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP))
                .currency(modelCostProperties.getCurrency())
                .priceMatched(matched)
                .build();
    }

    /**
     * calculateTotal（计算总成本）用于估算一组模型调用记录的成本。
     *
     * @param entities 模型调用记录列表
     * @return 模型调用成本
     */
    public ModelCallCost calculateTotal(List<ModelCallRecordEntity> entities) {
        BigDecimal inputCost = BigDecimal.ZERO;
        BigDecimal outputCost = BigDecimal.ZERO;
        boolean allMatched = true;
        for (ModelCallRecordEntity entity : entities == null ? List.<ModelCallRecordEntity>of() : entities) {
            ModelCallCost cost = calculate(entity);
            inputCost = inputCost.add(cost.getInputCost());
            outputCost = outputCost.add(cost.getOutputCost());
            allMatched = allMatched && Boolean.TRUE.equals(cost.getPriceMatched());
        }
        return ModelCallCost.builder()
                .inputCost(inputCost.setScale(6, RoundingMode.HALF_UP))
                .outputCost(outputCost.setScale(6, RoundingMode.HALF_UP))
                .totalCost(inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP))
                .currency(modelCostProperties.getCurrency())
                .priceMatched(allMatched)
                .build();
    }

    /**
     * findPrice（查找价格）用于按供应商和模型名称读取单价配置。
     */
    private ModelCostPrice findPrice(String provider, String modelName) {
        if (provider == null || provider.isBlank() || modelName == null || modelName.isBlank()) {
            return null;
        }
        return modelCostProperties.getPricing()
                .getOrDefault(provider, java.util.Map.of())
                .get(modelName);
    }

    /**
     * cost（成本）用于按 Token（词元）数量和每千 Token（词元）价格计算金额。
     */
    private BigDecimal cost(Integer tokens, BigDecimal pricePer1kTokens) {
        if (tokens == null || tokens <= 0 || pricePer1kTokens == null) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(tokens)
                .divide(THOUSAND, 8, RoundingMode.HALF_UP)
                .multiply(pricePer1kTokens)
                .setScale(6, RoundingMode.HALF_UP);
    }
}
