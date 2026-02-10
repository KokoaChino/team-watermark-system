package com.github.kokoachino.service;

import com.github.kokoachino.model.dto.CreatePaymentOrderDTO;
import com.github.kokoachino.model.vo.PaymentOrderVO;
import java.util.Map;


/**
 * 支付服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
public interface PaymentService {

    /**
     * 创建支付订单
     *
     * @param dto 订单信息
     * @return 订单VO（包含支付二维码）
     */
    PaymentOrderVO createOrder(CreatePaymentOrderDTO dto);

    /**
     * 查询订单状态
     *
     * @param orderNo 订单号
     * @return 订单VO
     */
    PaymentOrderVO queryOrder(String orderNo);

    /**
     * 处理支付宝回调
     *
     * @param params 回调参数
     * @return 是否处理成功
     */
    boolean handleAlipayCallback(Map<String, String> params);

    /**
     * 验证订单并充值点数
     *
     * @param orderNo 订单号
     * @return 是否成功
     */
    boolean verifyAndRecharge(String orderNo);
}
