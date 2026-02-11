package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 点数流水实体
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@TableName("tw_point_transaction")
public class PointTransaction {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 团队ID
     */
    private Integer teamId;

    /**
     * 用户ID（操作人）
     */
    private Integer userId;

    /**
     * 交易类型：deduct-预扣，consume-实际消耗，refund-返还，recharge-充值
     */
    private String type;

    /**
     * 交易点数（正数表示增加，负数表示减少）
     */
    private Integer points;

    /**
     * 交易前余额
     */
    private Integer balanceBefore;

    /**
     * 交易后余额
     */
    private Integer balanceAfter;

    /**
     * 关联业务类型：batch_task-批量任务，payment-支付充值
     */
    private String bizType;

    /**
     * 关联业务ID
     */
    private String bizId;

    /**
     * 交易描述
     */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
