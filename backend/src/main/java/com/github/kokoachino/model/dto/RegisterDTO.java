package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 注册 DTO
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Data
@Schema(description = "注册请求")
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 16, message = "用户名长度需在4-16位之间")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "username")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度需在6-16位之间")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "password123")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "user@example.com")
    private String email;

    @NotBlank(message = "人机验证码不能为空")
    @Size(min = 4, max = 6, message = "人机验证码长度为4-6位")
    @Schema(description = "人机验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "abcd")
    private String captcha;

    @NotBlank(message = "人机验证码标识不能为空")
    @Size(max = 64, message = "人机验证码标识长度不能超过64位")
    @Schema(description = "人机验证码标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "550e8400-e29b-41d4-a716-446655440000")
    private String captchaKey;

    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 6, max = 6, message = "邮箱验证码长度为6位")
    @Schema(description = "邮箱验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    private String emailCode;
}
