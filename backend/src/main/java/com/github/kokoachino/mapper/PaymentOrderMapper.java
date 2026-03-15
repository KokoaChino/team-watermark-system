package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;

/**
 * 支付订单 Mapper 接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    /**
     * 根据订单号查询
     */
    PaymentOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 将待支付订单更新为已支付（仅当当前状态为 pending 时生效）
     */
    int markPaidIfPending(@Param("orderNo") String orderNo,
                          @Param("alipayTradeNo") String alipayTradeNo,
                          @Param("paidAt") LocalDateTime paidAt);
}
