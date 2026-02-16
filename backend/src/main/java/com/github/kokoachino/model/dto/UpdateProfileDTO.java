package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 修改个人信息 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "修改个人信息请求")
public class UpdateProfileDTO {

    @Size(min = 4, max = 16, message = "用户名长度需在4-16位之间")
    @Schema(description = "新用户名", example = "newUsername")
    private String username;

    @Size(min = 6, max = 16, message = "新密码长度需在6-16位之间")
    @Schema(description = "新密码", example = "newPassword123")
    private String newPassword;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "新邮箱（修改邮箱时必填）", example = "newemail@example.com")
    private String newEmail;

    @Size(min = 6, max = 6, message = "邮箱验证码长度为6位")
    @Schema(description = "新邮箱验证码（修改邮箱时必填）", example = "123456")
    private String emailCode;

    @AssertTrue(message = "至少需要修改一项信息")
    public boolean isAtLeastOneFieldPresent() {
        return username != null || newPassword != null || newEmail != null;
    }

    @AssertTrue(message = "修改邮箱时，邮箱验证码必须填写")
    public boolean isEmailChangeValid() {
        if (newEmail != null) {
            return emailCode != null;
        }
        return true;
    }
}
