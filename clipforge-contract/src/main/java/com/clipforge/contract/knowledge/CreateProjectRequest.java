package com.clipforge.contract.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreateProjectRequest（创建项目请求）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @NotBlank(message = "用户编号不能为空")
    private String userId;

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String description;
}
