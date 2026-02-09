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
@Schema(description = "邀请记录信息")
public class InviteRecordVO {

    @Schema(description = "记录 ID")
    private Integer id;

    @Schema(description = "被邀请人用户 ID")
    private Integer userId;

    @Schema(description = "被邀请人用户名")
    private String username;

    @Schema(description = "加入时间")
    private LocalDateTime joinedAt;
}
