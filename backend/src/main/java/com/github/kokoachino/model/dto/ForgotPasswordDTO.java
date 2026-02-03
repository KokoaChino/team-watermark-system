package com.github.kokoachino.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 找回密码 DTO
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Data
public class ForgotPasswordDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码长度固定为6位")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度需在6-16位之间")
    private String newPassword;
}
