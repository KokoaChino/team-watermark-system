package github.kokoachino.model.vo;

import lombok.Builder;
import lombok.Data;


/**
 * 验证码 VO
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Data
@Builder
public class CaptchaVO {

    private String base64; // 图片base64
    private String code;   // 验证码内容 (仅内部或测试使用，返回前端时不应包含此字段，或通过key匹配)
    private String key;    // 验证码标识
}
