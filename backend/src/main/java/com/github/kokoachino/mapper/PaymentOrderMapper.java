package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


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
    @Select("SELECT * FROM tw_payment_order WHERE order_no = #{orderNo}")
    PaymentOrder selectByOrderNo(@Param("orderNo") String orderNo);
}
