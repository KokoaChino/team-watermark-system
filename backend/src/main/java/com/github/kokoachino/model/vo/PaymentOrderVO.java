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

    @Schema(description = "订单ID", example = "1")
    private Integer id;

    @Schema(description = "订单号", example = "PO1234567890123")
    private String orderNo;

    @Schema(description = "购买点数", example = "1000")
    private Integer points;

    @Schema(description = "订单金额", example = "10.00")
    private BigDecimal amount;

    @Schema(description = "订单状态：pending-待支付，paid-已支付", example = "pending")
    private String status;

    @Schema(description = "支付二维码URL", example = "https://qr.alipay.com/abc123")
    private String qrCodeUrl;

    @Schema(description = "创建时间", example = "2026-02-10T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "支付完成时间", example = "2026-02-10T14:35:00")
    private LocalDateTime paidAt;
}
