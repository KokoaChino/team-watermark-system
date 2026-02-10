package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 支付订单 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Builder
@Schema(description = "支付订单信息")
public class PaymentOrderVO {

    @Schema(description = "订单ID")
    private Integer id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "购买点数")
    private Integer points;

    @Schema(description = "订单金额（元）")
    private BigDecimal amount;

    @Schema(description = "订单状态：pending-待支付，paid-已支付，invalid-已失效")
    private String status;

    @Schema(description = "支付宝支付二维码URL")
    private String qrCodeUrl;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "支付完成时间")
    private LocalDateTime paidAt;
}
