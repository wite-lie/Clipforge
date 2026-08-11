package com.clipforge.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreateProjectRequest（创建项目请求）。
 *
 * <p>该对象承载创建 Project（项目）时提交的项目名称、描述和用户编号。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    /** userId（用户编号）表示项目所属用户，当前学习阶段可以手动传入。 */
    private String userId;

    /** name（项目名称）表示项目的人类可读名称。 */
    @NotBlank(message = "项目名称不能为空")
    private String name;

    /** description（项目描述）用于说明项目目标和背景。 */
    private String description;
}
