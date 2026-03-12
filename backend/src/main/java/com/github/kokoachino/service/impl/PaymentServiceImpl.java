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
import com.github.kokoachino.common.util.RandomStringUtils;
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
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;


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
        int maxPoints = systemProperties.getPoint().getMaxPointsPerOrder();
        if (dto.getPoints() > maxPoints) {
            throw new BizException(ResultCode.PAYMENT_POINTS_EXCEED_LIMIT);
        }
        BigDecimal amount = BigDecimal.valueOf(dto.getPoints())
                .multiply(BigDecimal.valueOf(systemProperties.getPoint().getPrice()))
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
            request.setNotifyUrl(buildNotifyUrl(alipayConfig.getNotifyUrl()));
            AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
            model.setOutTradeNo(order.getOrderNo());
            model.setTotalAmount(amount.toString());
            model.setSubject("水印系统点数充值 - " + dto.getPoints() + "点");
            model.setBody("购买点数：" + dto.getPoints() + "点，单价：" + systemProperties.getPoint().getPrice() + "元/点");
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
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderVO queryOrder(String orderNo, boolean forceSync) {
        PaymentOrder order = paymentOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResultCode.PAYMENT_ORDER_NOT_FOUND);
        }
        if (forceSync && "pending".equals(order.getStatus())) {
            syncOrderStatusFromAlipay(orderNo);
            order = paymentOrderMapper.selectByOrderNo(orderNo);
            if (order == null) {
                throw new BizException(ResultCode.PAYMENT_ORDER_NOT_FOUND);
            }
        }
        return buildPaymentOrderVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAlipayCallback(Map<String, String> params) {
        try {
            if (params == null || params.isEmpty()) {
                log.warn("支付宝回调参数为空，忽略本次回调");
                return false;
            }
            if (params.get("sign") == null || params.get("sign").trim().isEmpty()) {
                log.warn("支付宝回调缺少签名参数，忽略本次回调");
                return false;
            }
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    alipayConfig.getSignType()
            );
            if (!signVerified) {
                log.warn("支付宝回调签名验证失败");
                return false;
            }
            String orderNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String alipayTradeNo = params.get("trade_no");
            if (orderNo == null || orderNo.trim().isEmpty()) {
                log.warn("支付宝回调缺少订单号，忽略本次回调");
                return false;
            }
            PaymentOrder order = paymentOrderMapper.selectByOrderNo(orderNo);
            if (order == null) {
                log.error("支付宝回调订单不存在：orderNo={}", orderNo);
                return false;
            }
            if ("paid".equals(order.getStatus())) {
                return true;
            }
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                markOrderPaidAndRecharge(orderNo, alipayTradeNo);
                log.info("支付宝回调处理成功：orderNo={}", orderNo);
                return true;
            }
            return false;
        } catch (AlipayApiException e) {
            log.warn("支付宝回调签名校验异常：{}", e.getMessage());
            return false;
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("处理支付宝回调失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyAndRecharge(String orderNo) {
        PaymentOrderVO vo = queryOrder(orderNo, true);
        return "paid".equals(vo.getStatus());
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomNum = String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
        String randomStr = RandomStringUtils.generate(12);
        return String.format("PAY%s-%s-%s", randomNum, date, randomStr);
    }

    /**
     * 构建支付宝异步通知地址。
     * 支持仅配置穿透域名，服务端会自动补齐回调路径，避免回调打到根路径触发鉴权异常。
     */
    private String buildNotifyUrl(String notifyUrl) {
        if (notifyUrl == null || notifyUrl.trim().isEmpty()) {
            throw new BizException(ResultCode.PAYMENT_CREATE_FAILED);
        }
        String normalized = notifyUrl.trim().replaceAll("/+$", "");
        final String callbackPath = "/api/payment/alipay/notify";
        if (normalized.endsWith(callbackPath)) {
            return normalized;
        }
        return normalized + callbackPath;
    }

    /**
     * 强制向支付宝同步订单状态。
     * 当支付宝返回交易不存在时，通常是用户尚未支付或订单状态尚未落库，此场景按未支付处理即可。
     */
    private void syncOrderStatusFromAlipay(String orderNo) {
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    markOrderPaidAndRecharge(orderNo, response.getTradeNo());
                }
                return;
            }
            if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                log.info("支付宝订单尚未查询到交易记录：orderNo={}", orderNo);
                return;
            }
            log.warn("支付宝订单查询失败：orderNo={}, code={}, subCode={}, subMsg={}",
                    orderNo, response.getCode(), response.getSubCode(), response.getSubMsg());
        } catch (AlipayApiException e) {
            log.error("查询支付宝订单失败：orderNo={}", orderNo, e);
        }
    }

    private void markOrderPaidAndRecharge(String orderNo, String alipayTradeNo) {
        int affected = paymentOrderMapper.markPaidIfPending(orderNo, alipayTradeNo, LocalDateTime.now());
        if (affected <= 0) {
            log.info("订单已处理，跳过重复入账：orderNo={}", orderNo);
            return;
        }
        PaymentOrder paidOrder = paymentOrderMapper.selectByOrderNo(orderNo);
        if (paidOrder == null) {
            throw new BizException(ResultCode.PAYMENT_ORDER_NOT_FOUND);
        }
        pointService.rechargePoints(
                paidOrder.getTeamId(), paidOrder.getUserId(), paidOrder.getPoints(),
                "payment", paidOrder.getOrderNo(),
                "支付宝充值：" + paidOrder.getPoints() + "点"
        );
        log.info("支付订单入账成功：orderNo={}, points={}", orderNo, paidOrder.getPoints());
    }

    private PaymentOrderVO buildPaymentOrderVO(PaymentOrder order) {
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
}
