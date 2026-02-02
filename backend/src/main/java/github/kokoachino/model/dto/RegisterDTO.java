package github.kokoachino.model.dto;

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
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 16, message = "用户名长度需在4-16位之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度需在6-16位之间")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "人机验证码不能为空")
    private String captcha;

    @NotBlank(message = "人机验证码标识不能为空")
    private String captchaKey;

    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;
}
