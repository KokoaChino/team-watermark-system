package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 转让队长身份 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-16
 */
@Data
@Schema(description = "转让队长身份请求")
public class TransferLeaderDTO {

    @NotNull(message = "新队长用户ID不能为空")
    @Schema(description = "新队长用户ID", example = "2")
    private Integer newLeaderId;
}
