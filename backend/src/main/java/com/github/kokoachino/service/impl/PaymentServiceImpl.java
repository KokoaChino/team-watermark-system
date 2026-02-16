package com.github.kokoachino.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.TeamContext;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.config.AlipayConfig;
import com.github.kokoachino.config.SystemProperties;
import com.github.kokoachino.mapper.PaymentOrderMapper;
import com.github.kokoachino.model.dto.CreatePaymentOrderDTO;
import com.github.kokoachino.model.entity.PaymentOrder;
import com.github.kokoachino.model.vo.PaymentOrderVO;
import com.github.kokoachino.service.PaymentService;
import com.github.kokoachino.service.PointService;
import com.github.kokoachino.common.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;


/**
 * 支付服务实现
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;
    private final SystemProperties systemProperties;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PointService pointService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderVO createOrder(CreatePaymentOrderDTO dto) {
        Integer userId = UserContext.getUserId();
        Integer teamId = TeamContext.getTeamId();
        BigDecimal amount = BigDecimal.valueOf(dto.getPoints())
                .multiply(BigDecimal.valueOf(systemProperties.getPointPrice()))
                .setScale(2, RoundingMode.HALF_UP);
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(generateOrderNo());
        order.setTeamId(teamId);
        order.setUserId(userId);
        order.setPoints(dto.getPoints());
        order.setAmount(amount);
        order.setStatus("pending");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        paymentOrderMapper.insert(order);
        try {
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(alipayConfig.getNotifyUrl());
            AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
            model.setOutTradeNo(order.getOrderNo());
            model.setTotalAmount(amount.toString());
            model.setSubject("水印系统点数充值 - " + dto.getPoints() + "点");
            model.setBody("购买点数：" + dto.getPoints() + "点，单价：" + systemProperties.getPointPrice() + "元/点");
            request.setBizModel(model);
            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("支付宝预创建订单成功：orderNo={}, qrCode={}", order.getOrderNo(), response.getQrCode());
                String qrCodeBase64 = QrCodeUtil.generateBase64QrCode(response.getQrCode());
                return PaymentOrderVO.builder()
                        .id(order.getId())
                        .orderNo(order.getOrderNo())
                        .points(order.getPoints())
                        .amount(order.getAmount())
                        .status(order.getStatus())
                        .qrCodeBase64(qrCodeBase64)
                        .createdAt(order.getCreatedAt())
                        .build();
            } else {
                log.error("支付宝预创建订单失败：orderNo={}, msg={}", order.getOrderNo(), response.getMsg());
                throw new BizException(ResultCode.PAYMENT_CREATE_FAILED);
            }
        } catch (AlipayApiException e) {
            log.error("支付宝接口调用失败", e);
            throw new BizException(ResultCode.PAYMENT_CREATE_FAILED);
        }
    }

    @Override
    public PaymentOrderVO queryOrder(String orderNo) {
        PaymentOrder order = paymentOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResultCode.PAYMENT_ORDER_NOT_FOUND);
        }
        if ("pending".equals(order.getStatus())) {
            try {
                AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
                request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");
                AlipayTradeQueryResponse response = alipayClient.execute(request);
                if (response.isSuccess() && "TRADE_SUCCESS".equals(response.getTradeStatus())) {
                    order.setStatus("paid");
                    order.setAlipayTradeNo(response.getTradeNo());
                    order.setPaidAt(LocalDateTime.now());
                    order.setUpdatedAt(LocalDateTime.now());
                    paymentOrderMapper.updateById(order);
                    pointService.rechargePoints(
                            order.getTeamId(), order.getUserId(), order.getPoints(),
                            "payment", order.getOrderNo(),
                            "支付宝充值：" + order.getPoints() + "点"
                    );
                }
            } catch (AlipayApiException e) {
                log.error("查询支付宝订单失败", e);
            }
        }
        return PaymentOrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .points(order.getPoints())
                .amount(order.getAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAlipayCallback(Map<String, String> params) {
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    alipayConfig.getSignType()
            );
            if (!signVerified) {
                log.error("支付宝回调签名验证失败");
                return false;
            }
            String orderNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String alipayTradeNo = params.get("trade_no");
            PaymentOrder order = paymentOrderMapper.selectByOrderNo(orderNo);
            if (order == null) {
                log.error("支付宝回调订单不存在：orderNo={}", orderNo);
                return false;
            }
            if ("paid".equals(order.getStatus())) {
                return true;
            }
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                order.setStatus("paid");
                order.setAlipayTradeNo(alipayTradeNo);
                order.setPaidAt(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());
                paymentOrderMapper.updateById(order);
                pointService.rechargePoints(
                        order.getTeamId(), order.getUserId(), order.getPoints(),
                        "payment", order.getOrderNo(),
                        "支付宝充值：" + order.getPoints() + "点"
                );
                log.info("支付宝回调处理成功：orderNo={}, points={}", orderNo, order.getPoints());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("处理支付宝回调失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyAndRecharge(String orderNo) {
        PaymentOrderVO vo = queryOrder(orderNo);
        return "paid".equals(vo.getStatus());
    }

    private String generateOrderNo() {
        return "PO" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }
}
