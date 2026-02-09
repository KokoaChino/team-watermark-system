package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.model.dto.*;
import com.github.kokoachino.model.vo.*;
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
 * @date 2026-02-09
 */
@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
@Tag(name = "团队管理", description = "团队协作相关接口")
public class TeamController {

    private final TeamService teamService;

    /**
     * 生成团队邀请码
     *
     * @param dto 生成邀请码参数
     * @return 邀请码信息
     */
    @PostMapping("/invite-code")
    @Operation(summary = "生成邀请码", description = "队长生成团队邀请码，可设置有效期和使用次数限制")
    public Result<InviteCodeVO> generateInviteCode(@Valid @RequestBody GenerateInviteCodeDTO dto) {
        Integer teamId = UserContext.getUser().getTeamId();
        String username = UserContext.getUser().getUsername();
        InviteCodeVO inviteCodeVO = teamService.generateInviteCode(teamId, dto, username);
        return Result.success(inviteCodeVO, "邀请码生成成功");
    }

    /**
     * 使用邀请码加入团队
     *
     * @param dto 加入团队参数
     * @return 加入后的团队信息
     */
    @PostMapping("/join")
    @Operation(summary = "加入团队", description = "使用邀请码加入团队，支持从文本中提取邀请码")
    public Result<TeamMemberVO> joinTeam(@Valid @RequestBody JoinTeamDTO dto) {
        Integer userId = UserContext.getUserId();
        String username = UserContext.getUser().getUsername();
        TeamMemberVO teamMemberVO = teamService.joinTeam(userId, username, dto);
        return Result.success(teamMemberVO, "加入团队成功");
    }

    /**
     * 使邀请码失效
     *
     * @param codeId 邀请码ID
     * @return 操作结果
     */
    @PutMapping("/invite-code/{codeId}/deactivate")
    @Operation(summary = "失效邀请码", description = "队长使指定邀请码失效")
    public Result<Object> deactivateInviteCode(@PathVariable Integer codeId) {
        Integer teamId = UserContext.getUser().getTeamId();
        teamService.deactivateInviteCode(codeId, teamId);
        return Result.success(null, "邀请码已失效");
    }

    /**
     * 获取团队的邀请码列表
     *
     * @return 邀请码列表
     */
    @GetMapping("/invite-codes")
    @Operation(summary = "获取邀请码列表", description = "获取当前团队的所有邀请码")
    public Result<List<InviteCodeVO>> getInviteCodes() {
        Integer teamId = UserContext.getUser().getTeamId();
        List<InviteCodeVO> codes = teamService.getInviteCodesByTeamId(teamId);
        return Result.success(codes);
    }

    /**
     * 获取邀请码的邀请记录
     *
     * @param codeId 邀请码ID
     * @return 邀请记录列表
     */
    @GetMapping("/invite-code/{codeId}/records")
    @Operation(summary = "获取邀请记录", description = "获取指定邀请码的邀请记录列表")
    public Result<List<InviteRecordVO>> getInviteRecords(@PathVariable Integer codeId) {
        List<InviteRecordVO> records = teamService.getInviteRecords(codeId);
        return Result.success(records);
    }

    /**
     * 退出当前团队
     *
     * @return 操作结果
     */
    @PostMapping("/leave")
    @Operation(summary = "退出团队", description = "退出当前团队，自动回归个人团队")
    public Result<Object> leaveTeam() {
        Integer userId = UserContext.getUserId();
        String username = UserContext.getUser().getUsername();
        teamService.leaveTeam(userId, username);
        return Result.success(null, "已退出团队");
    }

    /**
     * 踢出团队成员
     *
     * @param dto 踢出成员参数
     * @return 操作结果
     */
    @PostMapping("/kick")
    @Operation(summary = "踢出成员", description = "队长踢出指定成员")
    public Result<Object> kickMember(@Valid @RequestBody KickMemberDTO dto) {
        Integer teamId = UserContext.getUser().getTeamId();
        String operatorUsername = UserContext.getUser().getUsername();
        teamService.kickMember(teamId, dto.getUserId(), operatorUsername);
        return Result.success(null, "成员已被踢出");
    }

    /**
     * 获取当前团队信息
     *
     * @return 团队信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取团队信息", description = "获取当前用户所属团队的详细信息")
    public Result<TeamMemberVO> getCurrentTeamInfo() {
        Integer userId = UserContext.getUserId();
        TeamMemberVO teamMemberVO = teamService.getCurrentTeamInfo(userId);
        return Result.success(teamMemberVO);
    }
}
