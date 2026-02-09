package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "有效期不能为空")
    @Schema(description = "有效期截止时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-12-31T23:59:59")
    private LocalDateTime validUntil;

    @NotNull(message = "最大使用次数不能为空")
    @Min(value = 1, message = "最大使用次数至少为1")
    @Max(value = 100, message = "最大使用次数不能超过100")
    @Schema(description = "最大使用次数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer maxUses;
}
