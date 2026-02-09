package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 邀请码 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Builder
@Schema(description = "邀请码信息")
public class InviteCodeVO {

    @Schema(description = "邀请码 ID")
    private Integer id;

    @Schema(description = "邀请码")
    private String code;

    @Schema(description = "分享文本")
    private String shareText;

    @Schema(description = "有效期截止时间")
    private LocalDateTime validUntil;

    @Schema(description = "最大使用次数")
    private Integer maxUses;

    @Schema(description = "已使用次数")
    private Integer usesCount;

    @Schema(description = "状态：active-有效，inactive-已失效")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
