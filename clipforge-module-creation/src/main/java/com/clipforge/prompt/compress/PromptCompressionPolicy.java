package com.clipforge.prompt.compress;

/**
 * PromptCompressionPolicy（提示词压缩策略）。
 *
 * <p>该接口用于根据 Token Budget（词元预算）决策，对本次模型调用前的动态 Prompt（提示词）上下文做规则裁剪。
 */
public interface PromptCompressionPolicy {

    /**
     * compress（压缩）用于生成动态上下文的压缩覆盖文本。
     *
     * @param request 压缩请求
     * @return 压缩结果
     */
    PromptCompressionResult compress(PromptCompressionRequest request);
}
