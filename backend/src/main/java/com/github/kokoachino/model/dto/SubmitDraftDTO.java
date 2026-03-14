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

    @NotNull(message = "提交方式不能为空")
    @Schema(description = "提交方式：CREATE=新建模板，UPDATE=修改源模板", example = "UPDATE")
    private SubmitAction submitAction;

    public enum SubmitAction {
        CREATE,
        UPDATE
    }
}
