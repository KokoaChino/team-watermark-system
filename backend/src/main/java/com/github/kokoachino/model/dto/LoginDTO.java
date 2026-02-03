package com.github.kokoachino.model.dto;

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
public class LoginDTO {

    @NotBlank(message = "账户不能为空")
    private String account;

    @Size(min = 6, max = 16, message = "密码长度需在6-16位之间")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String captcha;

    @NotBlank(message = "人机验证码标识不能为空")
    private String captchaKey;

    @NotBlank(message = "登录类型不能为空")
    private String loginType;
}