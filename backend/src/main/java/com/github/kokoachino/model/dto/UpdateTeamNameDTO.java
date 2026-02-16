package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 修改团队名称 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-16
 */
@Data
@Schema(description = "修改团队名称请求")
public class UpdateTeamNameDTO {

    @Size(min = 1, max = 32, message = "团队名称长度需在1-32位之间")
    @Schema(description = "新团队名称", example = "设计团队A")
    private String name;
}
