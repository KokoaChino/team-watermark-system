package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 创建支付订单 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Schema(description = "创建支付订单请求")
public class CreatePaymentOrderDTO {

    @NotNull(message = "购买点数不能为空")
    @Min(value = 1, message = "最少购买1点")
    @Schema(description = "购买点数", example = "1000")
    private Integer points;
}
