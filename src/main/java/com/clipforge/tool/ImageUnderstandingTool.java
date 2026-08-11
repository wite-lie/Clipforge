package com.clipforge.tool;

import com.clipforge.api.dto.MaterialResponse;
import com.clipforge.model.vision.VisionModelClient;
import com.clipforge.model.vision.VisionModelRequest;
import com.clipforge.model.vision.VisionModelRequestMaterial;
import com.clipforge.model.vision.VisionModelResponse;
import com.clipforge.model.usage.ModelCallRecord;
import com.clipforge.model.usage.ModelUsageRecorder;
import com.clipforge.trace.TraceRecorder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ImageUnderstandingTool（图片理解工具）。
 *
 * <p>该工具负责把系统中的图片素材元数据转换为 VLM（视觉语言模型）请求，并调用 VisionModelClient（视觉模型客户端）得到结构化图片理解结果。
 *
 * <p>注意：该工具不是把 VLM（视觉语言模型）本身当成 Function Calling（函数调用）工具，而是封装“素材整理、请求构造、模型调用、结果结构化和 Trace（执行链路追踪）记录”这一整段受控业务动作。
 */
@Component
@RequiredArgsConstructor
public class ImageUnderstandingTool implements Tool {

    /** DEFAULT_INSTRUCTION（默认理解指令）用于在调用方没有传入指令时给视觉模型一个稳定观察目标。 */
    private static final String DEFAULT_INSTRUCTION = "请面向短视频创作规划理解图片，重点识别主体、场景、视觉风格、可用于分镜的画面元素、图片文字和潜在风险。";

    /** traceRecorder（执行链路记录器）用于记录图片理解工具调用过程。 */
    private final TraceRecorder traceRecorder;

    /** visionModelClient（视觉模型客户端）用于执行图片理解模型调用。 */
    private final VisionModelClient visionModelClient;

    /** modelUsageRecorder（模型用量记录器）用于记录真实 VLM（视觉语言模型）调用的 Token（词元）和耗时。 */
    private final ModelUsageRecorder modelUsageRecorder;

    /**
     * name（工具名称）用于标识图片理解工具。
     *
     * @return 工具名称
     */
    @Override
    public String name() {
        // 返回工具名称。
        return "image_understanding";
    }

    /**
     * description（工具描述）用于说明该工具负责图片理解。
     *
     * @return 工具描述
     */
    @Override
    public String description() {
        // 返回工具能力描述。
        return "理解一张或多张图片素材，输出主体、场景、风格、关键元素、文字元素、创作用途和风险提示";
    }

    /**
     * understand（理解图片）用于调用视觉模型并返回结构化图片理解响应。
     *
     * @param runId 运行编号
     * @param materials 图片素材列表
     * @param instruction 理解指令
     * @return 视觉模型响应
     */
    public VisionModelResponse understand(String runId, List<MaterialResponse> materials, String instruction) {
        // 统计图片素材数量。
        int materialCount = materials == null ? 0 : materials.size();
        // 记录图片理解开始事件。
        traceRecorder.record(runId, "IMAGE_UNDERSTANDING_TOOL_STARTED", "图片理解工具开始执行", "素材数量：" + materialCount);
        // 构造视觉模型请求。
        VisionModelRequest request = VisionModelRequest.builder()
                // 设置运行编号。
                .runId(runId)
                // 设置模型调用用途。
                .purpose("IMAGE_UNDERSTANDING")
                // 设置理解指令。
                .instruction(resolveInstruction(instruction))
                // 设置视觉模型请求素材列表。
                .materials(toVisionMaterials(materials))
                // 设置较低温度，保证图片理解结果更稳定。
                .temperature(0.2)
                // 设置最大输出 Token（词元）数量。
                .maxTokens(1200)
                // 构建请求。
                .build();
        // 调用视觉模型客户端，并记录模型用量。
        VisionModelResponse response = callVisionModelAndRecord(runId, request);
        // 记录图片理解完成事件。
        traceRecorder.record(runId, "IMAGE_UNDERSTANDING_TOOL_FINISHED", "图片理解工具执行完成", "结果数量：" + safeResultCount(response));
        // 返回视觉模型响应。
        return response;
    }

    /**
     * callVisionModelAndRecord（调用视觉模型并记录）用于统一处理 VLM（视觉语言模型）调用、Trace（执行链路追踪）和 Token（词元）用量记录。
     *
     * @param runId 运行编号
     * @param request 视觉模型请求
     * @return 视觉模型响应
     */
    private VisionModelResponse callVisionModelAndRecord(String runId, VisionModelRequest request) {
        // 记录调用开始时间，用于计算模型调用耗时。
        long startedAt = System.currentTimeMillis();
        try {
            // 调用视觉模型客户端。
            VisionModelResponse response = visionModelClient.understand(request);
            // 计算调用耗时。
            long latencyMs = System.currentTimeMillis() - startedAt;
            // 记录成功的模型调用明细。
            modelUsageRecorder.record(ModelCallRecord.builder()
                    // 设置运行编号。
                    .runId(runId)
                    // 设置供应商。
                    .provider(response.getProvider())
                    // 设置模型名称。
                    .modelName(response.getModelName())
                    // 设置调用用途。
                    .purpose(request.getPurpose())
                    // 设置输入 Token（词元）数量。
                    .promptTokens(response.getPromptTokens())
                    // 设置输出 Token（词元）数量。
                    .completionTokens(response.getCompletionTokens())
                    // 设置总 Token（词元）数量。
                    .totalTokens(response.getTotalTokens())
                    // 设置调用耗时。
                    .latencyMs(latencyMs)
                    // 设置调用成功。
                    .success(true)
                    // 构建模型调用记录。
                    .build());
            // 记录视觉模型调用完成事件。
            traceRecorder.record(runId, "IMAGE_UNDERSTANDING_MODEL_FINISHED", "视觉模型调用完成", "provider：" + response.getProvider() + "，model：" + response.getModelName() + "，promptTokens：" + response.getPromptTokens() + "，completionTokens：" + response.getCompletionTokens() + "，totalTokens：" + response.getTotalTokens() + "，latencyMs：" + latencyMs);
            // 返回视觉模型响应。
            return response;
        } catch (RuntimeException exception) {
            // 计算失败耗时。
            long latencyMs = System.currentTimeMillis() - startedAt;
            // 解析错误码。
            String errorCode = exception instanceof com.clipforge.common.BusinessException businessException ? businessException.getErrorCode().getCode() : com.clipforge.common.ErrorCode.INTERNAL_ERROR.getCode();
            // 记录失败的模型调用明细。
            modelUsageRecorder.record(ModelCallRecord.builder()
                    // 设置运行编号。
                    .runId(runId)
                    // 设置调用用途。
                    .purpose(request.getPurpose())
                    // 设置调用耗时。
                    .latencyMs(latencyMs)
                    // 设置调用失败。
                    .success(false)
                    // 设置错误码。
                    .errorCode(errorCode)
                    // 设置错误消息。
                    .errorMessage(exception.getMessage())
                    // 构建模型调用记录。
                    .build());
            // 记录视觉模型调用失败事件。
            traceRecorder.record(runId, "IMAGE_UNDERSTANDING_MODEL_FAILED", "视觉模型调用失败", "purpose：" + request.getPurpose() + "，errorCode：" + errorCode + "，latencyMs：" + latencyMs);
            // 继续抛出异常。
            throw exception;
        }
    }

    /**
     * toVisionMaterials（转换视觉素材）用于把接口层素材响应转换为视觉模型请求素材。
     *
     * @param materials 接口层素材响应列表
     * @return 视觉模型请求素材列表
     */
    private List<VisionModelRequestMaterial> toVisionMaterials(List<MaterialResponse> materials) {
        // 如果素材列表为空，则返回空列表。
        if (materials == null || materials.isEmpty()) {
            // 返回空列表。
            return List.of();
        }
        // 转换每个素材响应。
        return materials.stream()
                // 转换为视觉模型请求素材。
                .map(this::toVisionMaterial)
                // 收集为列表。
                .toList();
    }

    /**
     * toVisionMaterial（转换单个视觉素材）用于复制图片理解所需的素材元数据。
     *
     * @param material 素材响应
     * @return 视觉模型请求素材
     */
    private VisionModelRequestMaterial toVisionMaterial(MaterialResponse material) {
        // 构造视觉模型请求素材。
        return VisionModelRequestMaterial.builder()
                // 设置素材编号。
                .materialId(material.getMaterialId())
                // 设置原始文件名。
                .originalFilename(material.getOriginalFilename())
                // 设置文件路径。
                .filePath(material.getFilePath())
                // 设置内容类型。
                .contentType(material.getContentType())
                // 设置文件大小。
                .fileSize(material.getFileSize())
                // 构建请求素材。
                .build();
    }

    /**
     * resolveInstruction（解析理解指令）用于为空指令提供默认值。
     *
     * @param instruction 原始理解指令
     * @return 最终理解指令
     */
    private String resolveInstruction(String instruction) {
        // 如果调用方没有传入指令，则使用默认短视频创作理解指令。
        if (instruction == null || instruction.isBlank()) {
            // 返回默认理解指令。
            return DEFAULT_INSTRUCTION;
        }
        // 返回调用方传入的理解指令。
        return instruction;
    }

    /**
     * safeResultCount（安全结果数量）用于避免响应为空时出现空指针。
     *
     * @param response 视觉模型响应
     * @return 图片理解结果数量
     */
    private int safeResultCount(VisionModelResponse response) {
        // 如果响应或结果列表为空，则返回 0。
        if (response == null || response.getResults() == null) {
            // 返回 0。
            return 0;
        }
        // 返回结果列表数量。
        return response.getResults().size();
    }
}
