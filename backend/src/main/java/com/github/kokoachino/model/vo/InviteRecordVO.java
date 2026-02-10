package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 邀请记录 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Builder
@Schema(description = "团队邀请记录信息")
public class InviteRecordVO {

    @Schema(description = "记录ID", example = "1")
    private Integer id;

    @Schema(description = "邀请码", example = "ABC123")
    private String inviteCode;

    @Schema(description = "被邀请人用户ID", example = "5")
    private Integer userId;

    @Schema(description = "被邀请人用户名", example = "new_member")
    private String username;

    @Schema(description = "加入时间", example = "2026-02-10T14:30:00")
    private LocalDateTime joinedAt;
}
