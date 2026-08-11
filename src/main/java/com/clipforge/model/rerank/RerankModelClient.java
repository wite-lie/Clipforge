package com.clipforge.model.rerank;

/**
 * RerankModelClient（重排序模型客户端）。
 *
 * <p>该接口屏蔽具体供应商差异，专门用于调用 Cross-Encoder（交叉编码器）类 Rerank Model（重排序模型），不复用 ChatModel（聊天模型）。
 */
public interface RerankModelClient {

    /**
     * rerank（重排序）用于给 query（查询文本）和候选文档计算相关性分数。
     *
     * @param request 重排序模型请求
     * @return 重排序模型响应
     */
    RerankModelResponse rerank(RerankModelRequest request);
}
