package github.kokoachino.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    private String code;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
