package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 生成邀请码 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "生成邀请码请求")
public class GenerateInviteCodeDTO {

    @Schema(description = "有效期截止时间，null表示无限制", example = "2026-12-31T23:59:59")
    private LocalDateTime validUntil;

    @Min(value = 1, message = "最大使用次数至少为1")
    @Max(value = 100, message = "最大使用次数不能超过100")
    @Schema(description = "最大使用次数，null表示无限制", example = "10")
    private Integer maxUses;
}
