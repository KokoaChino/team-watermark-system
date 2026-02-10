package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 点数流水 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Builder
@Schema(description = "点数流水记录")
public class PointTransactionVO {

    @Schema(description = "流水ID")
    private Integer id;

    @Schema(description = "交易类型：deduct-预扣，consume-实际消耗，refund-返还，recharge-充值")
    private String type;

    @Schema(description = "交易点数（正数表示增加，负数表示减少）")
    private Integer points;

    @Schema(description = "交易前余额")
    private Integer balanceBefore;

    @Schema(description = "交易后余额")
    private Integer balanceAfter;

    @Schema(description = "关联业务类型")
    private String bizType;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
