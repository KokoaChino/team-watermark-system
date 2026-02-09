package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 加入团队 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "加入团队请求")
public class JoinTeamDTO {

    @NotBlank(message = "邀请码不能为空")
    @Size(max = 200, message = "邀请码文本长度不能超过200")
    @Schema(description = "邀请码文本（可以包含【】包装的邀请码）", requiredMode = Schema.RequiredMode.REQUIRED, 
            example = "快来加入XX团队【AB4D82】，一起协作处理图片水印吧！")
    private String inviteCodeText;
}
