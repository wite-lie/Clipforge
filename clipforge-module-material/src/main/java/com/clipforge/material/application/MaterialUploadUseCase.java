package com.clipforge.material.application;

import com.clipforge.contract.material.MaterialResponse;

/**
 * MaterialUploadUseCase（素材上传用例）。
 *
 * <p>该接口是素材模块面向外部的命令边界，负责上传素材并创建后续处理任务。
 */
public interface MaterialUploadUseCase {

    /**
     * upload（上传素材）根据命令中的素材类型保存图片或视频素材。
     *
     * @param command 素材上传命令
     * @return 素材响应
     */
    MaterialResponse upload(MaterialUploadCommand command);
}
