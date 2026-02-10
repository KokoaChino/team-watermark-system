package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;


/**
 * 点数余额 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Builder
@Schema(description = "点数余额信息")
public class PointBalanceVO {

    @Schema(description = "团队ID")
    private Integer teamId;

    @Schema(description = "团队名称")
    private String teamName;

    @Schema(description = "当前点数余额")
    private Integer balance;
}
