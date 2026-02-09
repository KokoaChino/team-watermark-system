package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;


/**
 * 团队成员 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Builder
@Schema(description = "团队成员信息")
public class TeamMemberVO {

    @Schema(description = "团队 ID")
    private Integer teamId;

    @Schema(description = "团队名称")
    private String teamName;

    @Schema(description = "团队点数余额")
    private Integer pointBalance;

    @Schema(description = "队长 ID")
    private Integer leaderId;

    @Schema(description = "队长用户名")
    private String leaderName;

    @Schema(description = "当前用户角色")
    private String role;

    @Schema(description = "当前用户 ID")
    private Integer userId;

    @Schema(description = "当前用户名")
    private String username;
}
