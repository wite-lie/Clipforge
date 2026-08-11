package com.clipforge.api;

import com.clipforge.api.dto.MaterialUnderstandingResponse;
import com.clipforge.application.MaterialUnderstandingQueryApplicationService;
import com.clipforge.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MaterialUnderstandingController（素材理解结果接口）。
 *
 * <p>该控制器用于查询已经完成的素材理解结果，不负责触发 VLM（视觉语言模型）调用。
 */
@RestController
@Profile("db")
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialUnderstandingController {

    /** queryApplicationService（查询应用服务）用于读取素材理解结果。 */
    private final MaterialUnderstandingQueryApplicationService queryApplicationService;

    /**
     * queryImageUnderstanding（查询图片理解结果）用于返回图片素材最新成功的视觉理解结果。
     *
     * @param materialId 素材编号
     * @return 统一接口响应
     */
    @GetMapping("/{materialId}/understanding")
    public ApiResponse<MaterialUnderstandingResponse> queryImageUnderstanding(@PathVariable String materialId) {
        // 调用应用服务查询图片理解结果。
        MaterialUnderstandingResponse response = queryApplicationService.queryLatestImageUnderstanding(materialId);
        // 包装为统一成功响应。
        return ApiResponse.ok(response);
    }
}
