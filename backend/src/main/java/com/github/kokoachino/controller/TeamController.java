package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.common.util.TeamContext;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.model.dto.GenerateInviteCodeDTO;
import com.github.kokoachino.model.dto.JoinTeamDTO;
import com.github.kokoachino.model.dto.KickMemberDTO;
import com.github.kokoachino.model.dto.TransferLeaderDTO;
import com.github.kokoachino.model.dto.UpdateTeamNameDTO;
import com.github.kokoachino.model.vo.InviteCodeVO;
import com.github.kokoachino.model.vo.InviteRecordVO;
import com.github.kokoachino.model.vo.TeamMemberVO;
import com.github.kokoachino.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;


/**
 * 团队控制层
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
@Tag(name = "团队管理", description = "团队协作相关接口")
public class TeamController {

    private final TeamService teamService;

    @PostMapping("/invite-code")
    @Operation(summary = "生成邀请码", description = "队长生成团队邀请码，可设置有效期和使用次数限制")
    public Result<InviteCodeVO> generateInviteCode(@Valid @RequestBody GenerateInviteCodeDTO dto) {
        Integer teamId = TeamContext.getTeamId();
        Integer userId = UserContext.getUserId();
        String username = UserContext.getUser().getUsername();
        InviteCodeVO inviteCodeVO = teamService.generateInviteCode(teamId, userId, username, dto);
        return Result.success(inviteCodeVO, "邀请码生成成功");
    }

    @PostMapping("/join")
    @Operation(summary = "加入团队", description = "使用邀请码加入团队，支持从文本中提取邀请码")
    public Result<TeamMemberVO> joinTeam(@Valid @RequestBody JoinTeamDTO dto) {
        Integer userId = UserContext.getUserId();
        String username = UserContext.getUser().getUsername();
        TeamMemberVO teamMemberVO = teamService.joinTeam(userId, username, dto);
        return Result.success(teamMemberVO, "加入团队成功");
    }

    @PutMapping("/invite-code/{codeId}/deactivate")
    @Operation(summary = "失效邀请码", description = "队长使指定邀请码失效")
    public Result<Object> deactivateInviteCode(@PathVariable Integer codeId) {
        Integer teamId = TeamContext.getTeamId();
        Integer operatorUserId = UserContext.getUserId();
        String operatorUsername = UserContext.getUser().getUsername();
        teamService.deactivateInviteCode(codeId, teamId, operatorUserId, operatorUsername);
        return Result.success(null, "邀请码已失效");
    }

    @GetMapping("/invite-codes")
    @Operation(summary = "获取邀请码列表", description = "获取当前团队的所有邀请码")
    public Result<List<InviteCodeVO>> getInviteCodes() {
        Integer teamId = TeamContext.getTeamId();
        return Result.success(teamService.getInviteCodesByTeamId(teamId));
    }

    @GetMapping("/invite-code/{codeId}/records")
    @Operation(summary = "获取邀请记录", description = "获取指定邀请码的邀请记录列表")
    public Result<List<InviteRecordVO>> getInviteRecords(@PathVariable Integer codeId) {
        return Result.success(teamService.getInviteRecords(codeId));
    }

    @PostMapping("/leave")
    @Operation(summary = "退出团队", description = "退出当前团队，自动创建个人团队")
    public Result<TeamMemberVO> leaveTeam() {
        Integer userId = UserContext.getUserId();
        String username = UserContext.getUser().getUsername();
        return Result.success(teamService.leaveTeam(userId, username), "已退出团队");
    }

    @PostMapping("/kick")
    @Operation(summary = "踢出成员", description = "队长踢出指定成员")
    public Result<Object> kickMember(@Valid @RequestBody KickMemberDTO dto) {
        Integer teamId = TeamContext.getTeamId();
        Integer operatorUserId = UserContext.getUserId();
        String operatorUsername = UserContext.getUser().getUsername();
        teamService.kickMember(teamId, operatorUserId, operatorUsername, dto.getUserId());
        return Result.success(null, "成员已被踢出");
    }

    @GetMapping("/info")
    @Operation(summary = "获取团队信息", description = "获取当前用户所属团队的详细信息")
    public Result<TeamMemberVO> getCurrentTeamInfo() {
        Integer userId = UserContext.getUserId();
        return Result.success(teamService.getCurrentTeamInfo(userId));
    }

    @PutMapping("/name")
    @Operation(summary = "修改团队名称", description = "队长修改当前团队名称")
    public Result<TeamMemberVO> updateTeamName(@Valid @RequestBody UpdateTeamNameDTO dto) {
        Integer teamId = TeamContext.getTeamId();
        Integer userId = UserContext.getUserId();
        String username = UserContext.getUser().getUsername();
        return Result.success(teamService.updateTeamName(teamId, userId, username, dto), "团队名称修改成功");
    }

    @PostMapping("/transfer-leader")
    @Operation(summary = "转让队长", description = "队长将队长身份转让给其他成员")
    public Result<TeamMemberVO> transferLeader(@Valid @RequestBody TransferLeaderDTO dto) {
        Integer teamId = TeamContext.getTeamId();
        Integer userId = UserContext.getUserId();
        String username = UserContext.getUser().getUsername();
        return Result.success(teamService.transferLeader(teamId, userId, username, dto), "队长身份已转让");
    }
}
