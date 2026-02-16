package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.List;


/**
 * 团队信息 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Builder
@Schema(description = "团队信息")
public class TeamMemberVO {

    @Schema(description = "团队 ID", example = "1")
    private Integer teamId;

    @Schema(description = "团队名称", example = "设计团队A")
    private String teamName;

    @Schema(description = "团队点数余额", example = "9850")
    private Integer pointBalance;

    @Schema(description = "队长 ID", example = "2")
    private Integer leaderId;

    @Schema(description = "队长用户名", example = "team_leader")
    private String leaderName;

    @Schema(description = "当前用户角色：leader-队长，member-成员", example = "member")
    private String role;

    @Schema(description = "团队成员列表")
    private List<UserVO> members;
}
