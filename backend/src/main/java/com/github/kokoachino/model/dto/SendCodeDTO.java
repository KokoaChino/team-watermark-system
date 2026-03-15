package com.github.kokoachino.model.dto;

import com.github.kokoachino.common.enums.VerificationCodeTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 发送验证码 DTO
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Data
@Schema(description = "发送验证码请求")
public class SendCodeDTO {

    @Size(max = 100, message = "邮箱长度不能超过100位")
    @Schema(description = "邮箱（注册、找回密码、修改邮箱时必填）", example = "user@example.com")
    private String email;

    @Size(max = 50, message = "账户长度不能超过50位")
    @Schema(description = "账户（登录验证码时可填写用户名或邮箱）", example = "kokoachino")
    private String account;

    /**
     * @see VerificationCodeTypeEnum
     */
    @NotBlank(message = "业务类型不能为空")
    @Schema(description = "验证码类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "register")
    private String type;
}
