package github.kokoachino.model.dto;

import jakarta.validation.constraints.NotBlank;
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
    private String account; // 用户名或邮箱

    private String password;

    private String captcha;

    @NotBlank(message = "登录类型不能为空")
    private String loginType; // password 或 captcha
}
