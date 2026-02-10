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
@Schema(description = "团队邀请码信息")
public class InviteCodeVO {

    @Schema(description = "邀请码ID", example = "1")
    private Integer id;

    @Schema(description = "邀请码", example = "ABC123")
    private String code;

    @Schema(description = "有效期截止时间", example = "2026-02-20T23:59:59")
    private LocalDateTime validUntil;

    @Schema(description = "最大使用次数", example = "10")
    private Integer maxUses;

    @Schema(description = "已使用次数", example = "3")
    private Integer usesCount;

    @Schema(description = "状态：active-有效，inactive-已失效", example = "active")
    private String status;

    @Schema(description = "分享文本", example = "快来加入设计团队A！邀请码：ABC123")
    private String shareText;

    @Schema(description = "创建时间", example = "2026-02-10T14:30:00")
    private LocalDateTime createdAt;
}
