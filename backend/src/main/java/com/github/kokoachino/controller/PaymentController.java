package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.model.dto.batch.CreatePaymentOrderDTO;
import com.github.kokoachino.model.vo.PaymentOrderVO;
import com.github.kokoachino.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;


/**
 * 支付控制器
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "支付管理", description = "支付宝支付相关接口")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    @Operation(summary = "创建支付订单", description = "创建支付宝支付订单，返回支付二维码")
    public Result<PaymentOrderVO> createOrder(@Valid @RequestBody CreatePaymentOrderDTO dto) {
        PaymentOrderVO vo = paymentService.createOrder(dto);
        return Result.success(vo);
    }

    @GetMapping("/query/{orderNo}")
    @Operation(summary = "查询订单状态", description = "查询支付订单状态，如已支付则自动充值点数")
    public Result<PaymentOrderVO> queryOrder(@PathVariable String orderNo) {
        PaymentOrderVO vo = paymentService.queryOrder(orderNo);
        return Result.success(vo);
    }

    @PostMapping("/alipay/notify")
    @Operation(summary = "支付宝回调", description = "支付宝异步通知接口")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        log.info("支付宝回调参数：{}", params);
        boolean success = paymentService.handleAlipayCallback(params);
        return success ? "success" : "fail";
    }
}
