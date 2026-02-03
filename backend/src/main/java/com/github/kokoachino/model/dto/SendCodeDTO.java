package com.github.kokoachino.model.dto;

import com.github.kokoachino.common.enums.VerificationCodeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "user@example.com")
    private String email;

    /**
     * @see VerificationCodeType
     */
    @NotBlank(message = "业务类型不能为空")
    @Schema(description = "验证码类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "register")
    private String type;
}