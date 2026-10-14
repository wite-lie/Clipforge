package com.clipforge.creation.guardrail;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.contract.creation.CreationPlanResult;
import java.util.Collection;
import org.springframework.stereotype.Component;

/**
 * CreationPlanOutputValidator（创作方案输出校验器）。
 *
 * <p>该组件属于 Guardrails（护栏）的一部分，用于在返回结果前校验 Agent（智能体）输出是否完整。
 */
@Component
public class CreationPlanOutputValidator {

    /**
     * validate（校验）用于检查创作方案结果的关键字段。
     *
     * @param result 创作方案结果
     */
    public void validate(CreationPlanResult result) {
        if (result == null) {
            throw invalidOutput("创作方案结果不能为空");
        }
        if (!hasText(result.getTheme())) {
            throw invalidOutput("创作主题不能为空");
        }
        if (!hasText(result.getHook())) {
            throw invalidOutput("开头钩子不能为空");
        }
        if (isEmpty(result.getStoryboard())) {
            throw invalidOutput("分镜脚本不能为空");
        }
        if (isEmpty(result.getTitleCandidates())) {
            throw invalidOutput("标题候选不能为空");
        }
        if (result.getScore() < 0 || result.getScore() > 100) {
            throw invalidOutput("效果评分必须在 0 到 100 之间");
        }
    }

    /**
     * invalidOutput（非法输出异常）用于创建统一的智能体输出异常。
     *
     * @param message 异常消息
     * @return 业务异常
     */
    private BusinessException invalidOutput(String message) {
        return new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID, message);
    }

    /**
     * hasText（是否有文本）用于判断字符串去掉空白后是否仍有内容。
     *
     * @param value 原始字符串
     * @return 是否有有效文本
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * isEmpty（集合是否为空）用于避免校验器依赖 Spring 工具类。
     *
     * @param values 集合
     * @return 是否为空
     */
    private boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}
