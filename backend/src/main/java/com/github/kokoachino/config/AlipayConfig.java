package com.github.kokoachino.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 支付宝配置
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay.easy")
public class AlipayConfig {

    private String protocol;
    private String gatewayHost;
    private String signType;
    private String appId;
    private String merchantPrivateKey;
    private String alipayPublicKey;
    private String notifyUrl;

    @Bean
    public AlipayClient alipayClient() {
        String gatewayUrl = protocol + "://" + gatewayHost + "/gateway.do";
        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                merchantPrivateKey,
                "json",
                "UTF-8",
                alipayPublicKey,
                signType
        );
    }
}
