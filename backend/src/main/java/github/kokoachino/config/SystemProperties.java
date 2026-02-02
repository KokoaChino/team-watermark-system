package github.kokoachino.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * @author kokoachino
 * @date 2026-02-02
 */
@Data
@Component
@ConfigurationProperties(prefix = "system")
public class SystemProperties {

    /**
     * 初始点数
     */
    private Integer initialPoints = 10000;
    
    /**
     * 验证码有效期（分钟）
     */
    private Integer captchaExpiration = 2;
    
    /**
     * 邮箱验证码有效期（分钟）
     */
    private Integer emailCodeExpiration = 5;
}
