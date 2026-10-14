package com.clipforge.server.api;

import com.clipforge.contract.material.MaterialResponse;
import com.clipforge.common.ApiResponse;
import com.clipforge.material.application.MaterialQueryUseCase;
import com.clipforge.material.application.MaterialUploadCommand;
import com.clipforge.material.application.MaterialUploadUseCase;
import com.clipforge.material.model.MaterialType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * MaterialController（素材控制器）。
 *
 * <p>该控制器负责图片、视频上传和素材元数据查询。
 */
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    /** materialUploadUseCase（素材上传用例）负责通过模块命令边界上传素材。 */
    private final MaterialUploadUseCase materialUploadUseCase;

    /** materialQueryUseCase（素材查询用例）负责通过模块边界读取素材。 */
    private final MaterialQueryUseCase materialQueryUseCase;

    /**
     * uploadImage（上传图片）接口用于保存图片文件和素材元数据。
     *
     * @param file 图片文件
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @param uploadBatchId 上传批次编号
     * @return 素材响应
     */
    @PostMapping("/images")
    public ApiResponse<MaterialResponse> uploadImage(@RequestParam("file") MultipartFile file,
                                                     @RequestParam(required = false) String userId,
                                                     @RequestParam(required = false) String projectId,
                                                     @RequestParam(required = false) String conversationId,
                                                     @RequestParam(required = false) String runId,
                                                     @RequestParam(required = false) String uploadBatchId) {
        // 调用素材上传用例上传图片。
        MaterialResponse response = materialUploadUseCase.upload(toUploadCommand(file, userId, projectId, conversationId, runId, uploadBatchId, MaterialType.IMAGE));
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }

    /**
     * uploadVideo（上传视频）接口用于保存视频文件和素材元数据。
     *
     * @param file 视频文件
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @param uploadBatchId 上传批次编号
     * @return 素材响应
     */
    @PostMapping("/videos")
    public ApiResponse<MaterialResponse> uploadVideo(@RequestParam("file") MultipartFile file,
                                                     @RequestParam(required = false) String userId,
                                                     @RequestParam(required = false) String projectId,
                                                     @RequestParam(required = false) String conversationId,
                                                     @RequestParam(required = false) String runId,
                                                     @RequestParam(required = false) String uploadBatchId) {
        // 调用素材上传用例上传视频。
        MaterialResponse response = materialUploadUseCase.upload(toUploadCommand(file, userId, projectId, conversationId, runId, uploadBatchId, MaterialType.VIDEO));
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }

    /**
     * queryMaterial（查询素材）接口用于按素材编号读取素材详情。
     *
     * @param materialId 素材编号
     * @return 素材响应
     */
    @GetMapping("/{materialId}")
    public ApiResponse<MaterialResponse> queryMaterial(@PathVariable String materialId) {
        // 调用素材应用服务查询素材。
        MaterialResponse response = materialQueryUseCase.queryMaterial(materialId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }

    /**
     * listMaterials（查询素材列表）接口用于按用户、项目、会话、运行任务和素材类型查询素材集合。
     *
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @param materialType 素材类型
     * @return 素材响应列表
     */
    @GetMapping
    public ApiResponse<List<MaterialResponse>> listMaterials(@RequestParam(required = false) String userId,
                                                             @RequestParam(required = false) String projectId,
                                                             @RequestParam(required = false) String conversationId,
                                                             @RequestParam(required = false) String runId,
                                                             @RequestParam(required = false) String materialType) {
        // 调用素材应用服务查询素材列表。
        List<MaterialResponse> responses = materialQueryUseCase.listMaterials(userId, projectId, conversationId, runId, materialType);
        // 返回统一成功响应。
        return ApiResponse.ok(responses);
    }

    /**
     * toUploadCommand（转上传命令）用于隔离 Web 层 MultipartFile（多部分文件）和素材模块上传边界。
     */
    private MaterialUploadCommand toUploadCommand(MultipartFile file, String userId, String projectId, String conversationId,
                                                  String runId, String uploadBatchId, MaterialType materialType) {
        if (file == null) {
            return MaterialUploadCommand.builder()
                    .materialType(materialType.value())
                    .userId(userId)
                    .projectId(projectId)
                    .conversationId(conversationId)
                    .runId(runId)
                    .uploadBatchId(uploadBatchId)
                    .build();
        }
        return MaterialUploadCommand.builder()
                .materialType(materialType.value())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .userId(userId)
                .projectId(projectId)
                .conversationId(conversationId)
                .runId(runId)
                .uploadBatchId(uploadBatchId)
                .content(file::getInputStream)
                .build();
    }
}
