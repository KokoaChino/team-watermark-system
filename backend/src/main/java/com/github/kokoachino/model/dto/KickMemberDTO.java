package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 踢出成员 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "踢出成员请求")
public class KickMemberDTO {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "要踢出的成员用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer userId;
}
