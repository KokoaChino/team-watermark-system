package com.github.kokoachino.model.dto;

import com.github.kokoachino.common.enums.LoginType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 登录 DTO
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Data
@Schema(description = "登录请求")
public class LoginDTO {

    @NotBlank(message = "账户不能为空")
    @Size(max = 50, message = "账户长度不能超过50位")
    @Schema(description = "账户（用户名或邮箱）", requiredMode = Schema.RequiredMode.REQUIRED, example = "user@example.com")
    private String account;

    @Size(min = 6, max = 16, message = "密码长度需在6-16位之间")
    @Schema(description = "密码（密码登录时必填）", example = "123456")
    private String password;

    @Size(max = 6, message = "邮箱验证码长度不能超过6位")
    @Schema(description = "邮箱验证码（验证码登录时必填）", example = "123456")
    private String emailCode;

    /**
     * @see LoginType
     */
    @NotBlank(message = "登录类型不能为空")
    @Schema(description = "登录类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "password")
    private String loginType;
}