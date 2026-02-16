package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 支付订单实体
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@TableName("tw_payment_order")
public class PaymentOrder {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 团队ID
     */
    private Integer teamId;

    /**
     * 支付人ID
     */
    private Integer userId;

    /**
     * 购买点数
     */
    private Integer points;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 订单状态：pending-待支付，paid-已支付，invalid-已失效
     */
    private String status;

    /**
     * 支付宝交易号
     */
    private String alipayTradeNo;

    /**
     * 支付完成时间
     */
    private LocalDateTime paidAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
