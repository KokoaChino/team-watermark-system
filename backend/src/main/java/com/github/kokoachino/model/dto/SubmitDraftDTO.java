package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 提交草稿 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "提交草稿请求")
public class SubmitDraftDTO {

    @NotNull(message = "是否强制新建不能为空")
    @Schema(description = "是否强制新建（冲突时选择新建）", example = "false")
    private Boolean forceCreateNew;
}
