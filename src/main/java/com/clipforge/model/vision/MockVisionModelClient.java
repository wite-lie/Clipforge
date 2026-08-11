package com.clipforge.model.vision;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MockVisionModelClient（模拟视觉模型客户端）。
 *
 * <p>该实现不调用真实 VLM（视觉语言模型），而是根据图片文件名和理解指令返回稳定的结构化图片理解结果，便于 Phase 6.3（阶段 6.3）先验证图片理解链路。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.vision", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockVisionModelClient implements VisionModelClient {

    /**
     * understand（理解图片）用于返回稳定的模拟图片理解结果。
     *
     * @param request 视觉模型请求
     * @return 视觉模型响应
     */
    @Override
    public VisionModelResponse understand(VisionModelRequest request) {
        // 创建图片理解结果列表。
        List<ImageUnderstandingResult> results = new ArrayList<>();
        // 遍历请求中的图片素材。
        for (VisionModelRequestMaterial material : safeMaterials(request)) {
            // 为每一张图片构造模拟理解结果。
            results.add(buildMockResult(material));
        }
        // 返回模拟视觉模型响应。
        return VisionModelResponse.builder()
                // 设置供应商为 mock（模拟）。
                .provider("mock")
                // 设置模型名称。
                .modelName("mock-vision-model")
                // 设置原始内容，便于测试时观察。
                .rawContent("MockVisionModelClient（模拟视觉模型客户端）返回了 " + results.size() + " 张图片的结构化理解结果。")
                // 设置结构化结果。
                .results(results)
                // 模拟输入 Token（词元）消耗。
                .promptTokens(0)
                // 模拟输出 Token（词元）消耗。
                .completionTokens(0)
                // 模拟总 Token（词元）消耗。
                .totalTokens(0)
                // 构建响应。
                .build();
    }

    /**
     * safeMaterials（安全素材列表）用于避免请求或素材列表为空时出现空指针。
     *
     * @param request 视觉模型请求
     * @return 非空素材列表
     */
    private List<VisionModelRequestMaterial> safeMaterials(VisionModelRequest request) {
        // 如果请求为空或素材列表为空，则返回空列表。
        if (request == null || request.getMaterials() == null) {
            // 返回空列表。
            return List.of();
        }
        // 返回请求中的素材列表。
        return request.getMaterials();
    }

    /**
     * buildMockResult（构造模拟结果）用于按图片文件名生成可预测的图片理解结果。
     *
     * @param material 图片素材
     * @return 图片理解结果
     */
    private ImageUnderstandingResult buildMockResult(VisionModelRequestMaterial material) {
        // 判断图片是否看起来是卡通插画。
        boolean illustration = containsIgnoreCase(material.getOriginalFilename(), "cartoon")
                || containsIgnoreCase(material.getOriginalFilename(), "illustration")
                || containsIgnoreCase(material.getOriginalFilename(), "插画");
        // 根据风格判断设置视觉风格。
        String visualStyle = illustration ? "卡通插画风格" : "真实海洋摄影风格";
        // 根据风格判断设置创作用途。
        String creativeUsage = illustration ? "适合作为知识点转场或趣味解释画面" : "适合作为开头吸引注意或展示虎鲸真实形态的主画面";
        // 返回结构化图片理解结果。
        return ImageUnderstandingResult.builder()
                // 设置素材编号。
                .materialId(material.getMaterialId())
                // 设置主体。
                .mainSubject("虎鲸")
                // 设置场景。
                .scene("海洋环境，可能包含水面、浪花或虎鲸跃出水面的动态瞬间")
                // 设置视觉风格。
                .visualStyle(visualStyle)
                // 设置关键元素。
                .keyElements(List.of("黑白分明的体色", "圆润头部", "高耸背鳍", "海水与浪花", "适合科普讲解的动物主体"))
                // Phase 6.3 暂不做真实 OCR（光学字符识别），因此返回空文字元素。
                .textElements(List.of())
                // 设置创作用途。
                .creativeUsage(creativeUsage)
                // 设置风险提示。
                .riskNotes(List.of("当前为 Mock（模拟）理解结果，不能代表真实图像识别结论", "真实接入 VLM（视觉语言模型）后需要判断图片清晰度和版权风险"))
                // 设置摘要。
                .summary("画面主体是虎鲸，适合用于动物科普短视频中展示虎鲸外形、海洋栖息环境和强辨识度黑白体色。")
                // 构建结果。
                .build();
    }

    /**
     * containsIgnoreCase（忽略大小写包含）用于判断文件名是否包含某个关键词。
     *
     * @param value 原始文本
     * @param keyword 关键词
     * @return 包含时返回 true（真）
     */
    private boolean containsIgnoreCase(String value, String keyword) {
        // 如果原始文本或关键词为空，则返回 false（假）。
        if (value == null || keyword == null) {
            // 返回 false（假）。
            return false;
        }
        // 忽略大小写判断是否包含关键词。
        return value.toLowerCase().contains(keyword.toLowerCase());
    }
}
