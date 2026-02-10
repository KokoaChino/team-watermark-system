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

    @Schema(description = "流水ID", example = "1")
    private Integer id;

    @Schema(description = "交易类型：deduct-预扣，refund-返还，recharge-充值", example = "deduct")
    private String type;

    @Schema(description = "交易点数（正数表示增加，负数表示减少）", example = "-50")
    private Integer points;

    @Schema(description = "交易前余额", example = "10000")
    private Integer balanceBefore;

    @Schema(description = "交易后余额", example = "9950")
    private Integer balanceAfter;

    @Schema(description = "关联业务类型", example = "batch_task")
    private String bizType;

    @Schema(description = "交易描述", example = "批量任务预扣点数：50点")
    private String description;

    @Schema(description = "创建时间", example = "2026-02-10T14:30:00")
    private LocalDateTime createdAt;
}
