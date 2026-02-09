package com.github.kokoachino.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.kokoachino.common.enums.InviteCodeStatus;
import com.github.kokoachino.common.enums.TeamRole;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.InviteCodeUtils;
import com.github.kokoachino.mapper.*;
import com.github.kokoachino.model.dto.*;
import com.github.kokoachino.model.entity.*;
import com.github.kokoachino.model.vo.*;
import com.github.kokoachino.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 团队服务实现类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Service
@RequiredArgsConstructor
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    private final TeamMemberMapper teamMemberMapper;
    private final TeamInviteCodeMapper inviteCodeMapper;
    private final TeamInviteRecordMapper inviteRecordMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createPersonalTeam(Integer userId, String username, Integer initialPoints) {
        // 1. 创建团队
        Team team = new Team();
        team.setName(username + "的个人团队");
        team.setPointBalance(initialPoints);
        team.setLeaderId(userId);
        this.save(team);
        // 2. 添加成员关系
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole(TeamRole.LEADER.getValue());
        teamMemberMapper.insert(member);
        return team.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InviteCodeVO generateInviteCode(Integer teamId, GenerateInviteCodeDTO dto, String username) {
        // 1. 生成唯一邀请码
        String rawCode;
        do {
            rawCode = InviteCodeUtils.generateRawCode();
        } while (inviteCodeMapper.selectCount(
                new LambdaQueryWrapper<TeamInviteCode>()
                        .eq(TeamInviteCode::getCode, rawCode)
        ) > 0);
        // 2. 创建邀请码记录
        TeamInviteCode inviteCode = new TeamInviteCode();
        inviteCode.setTeamId(teamId);
        inviteCode.setCode(rawCode);
        inviteCode.setValidUntil(dto.getValidUntil());
        inviteCode.setMaxUses(dto.getMaxUses());
        inviteCode.setUsesCount(0);
        inviteCode.setStatus(InviteCodeStatus.ACTIVE.getValue());
        inviteCodeMapper.insert(inviteCode);
        // 3. 获取团队名称
        Team team = this.getById(teamId);
        String shareText = InviteCodeUtils.generateShareText(team.getName(), rawCode);
        return InviteCodeVO.builder()
                .id(inviteCode.getId())
                .code(rawCode)
                .shareText(shareText)
                .validUntil(inviteCode.getValidUntil())
                .maxUses(inviteCode.getMaxUses())
                .usesCount(0)
                .status(inviteCode.getStatus())
                .createdAt(inviteCode.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamMemberVO joinTeam(Integer userId, String username, JoinTeamDTO dto) {
        // 1. 从文本中提取邀请码
        String rawCode = InviteCodeUtils.extractCodeFromText(dto.getInviteCodeText());
        if (rawCode == null) {
            throw new BizException(ResultCode.INVITE_CODE_INVALID);
        }
        // 2. 查询邀请码
        TeamInviteCode inviteCode = inviteCodeMapper.selectOne(
                new LambdaQueryWrapper<TeamInviteCode>()
                        .eq(TeamInviteCode::getCode, rawCode)
        );
        if (inviteCode == null) {
            throw new BizException(ResultCode.INVITE_CODE_INVALID);
        }
        // 3. 校验邀请码状态
        if (!InviteCodeStatus.ACTIVE.getValue().equals(inviteCode.getStatus())) {
            throw new BizException(ResultCode.INVITE_CODE_INVALID);
        }
        // 4. 校验是否过期
        if (inviteCode.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.INVITE_CODE_EXPIRED);
        }
        // 5. 校验使用次数
        if (inviteCode.getUsesCount() >= inviteCode.getMaxUses()) {
            throw new BizException(ResultCode.INVITE_CODE_USED_UP);
        }
        Integer teamId = inviteCode.getTeamId();
        // 6. 检查用户是否已在该团队中
        TeamMember existingMember = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getTeamId, teamId)
        );
        if (existingMember != null) {
            throw new BizException(ResultCode.ALREADY_IN_TEAM);
        }
        // 7. 删除用户原有的团队成员关系（退出原团队）
        teamMemberMapper.delete(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
        );
        // 8. 添加到新团队
        TeamMember newMember = new TeamMember();
        newMember.setTeamId(teamId);
        newMember.setUserId(userId);
        newMember.setRole(TeamRole.MEMBER.getValue());
        teamMemberMapper.insert(newMember);
        // 9. 更新邀请码使用次数
        inviteCode.setUsesCount(inviteCode.getUsesCount() + 1);
        inviteCodeMapper.updateById(inviteCode);
        // 10. 记录邀请记录
        TeamInviteRecord record = new TeamInviteRecord();
        record.setInviteCodeId(inviteCode.getId());
        record.setTeamId(teamId);
        record.setUserId(userId);
        record.setUsername(username);
        inviteRecordMapper.insert(record);
        // 11. 返回团队信息
        return buildTeamMemberVO(teamId, userId, username, TeamRole.MEMBER.getValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateInviteCode(Integer codeId, Integer teamId) {
        TeamInviteCode inviteCode = inviteCodeMapper.selectById(codeId);
        if (inviteCode == null) {
            throw new BizException(ResultCode.INVITE_CODE_NOT_FOUND);
        }
        if (!inviteCode.getTeamId().equals(teamId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        inviteCode.setStatus(InviteCodeStatus.INACTIVE.getValue());
        inviteCodeMapper.updateById(inviteCode);
    }

    @Override
    public List<InviteCodeVO> getInviteCodesByTeamId(Integer teamId) {
        List<TeamInviteCode> codes = inviteCodeMapper.selectList(
                new LambdaQueryWrapper<TeamInviteCode>()
                        .eq(TeamInviteCode::getTeamId, teamId)
                        .orderByDesc(TeamInviteCode::getCreatedAt)
        );
        Team team = this.getById(teamId);
        return codes.stream().map(code -> {
            String shareText = InviteCodeStatus.ACTIVE.getValue().equals(code.getStatus())
                    ? InviteCodeUtils.generateShareText(team.getName(), code.getCode())
                    : null;
            return InviteCodeVO.builder()
                    .id(code.getId())
                    .code(code.getCode())
                    .shareText(shareText)
                    .validUntil(code.getValidUntil())
                    .maxUses(code.getMaxUses())
                    .usesCount(code.getUsesCount())
                    .status(code.getStatus())
                    .createdAt(code.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<InviteRecordVO> getInviteRecords(Integer codeId) {
        List<TeamInviteRecord> records = inviteRecordMapper.selectByInviteCodeId(codeId);
        return records.stream().map(record -> InviteRecordVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .username(record.getUsername())
                .joinedAt(record.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveTeam(Integer userId, String username) {
        // 1. 获取用户当前团队
        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
        );
        if (member == null) {
            throw new BizException(ResultCode.MEMBER_NOT_FOUND);
        }
        // 2. 检查是否是个人团队（队长不能退出自己的个人团队）
        Team team = this.getById(member.getTeamId());
        if (TeamRole.LEADER.getValue().equals(member.getRole()) && team.getLeaderId().equals(userId)) {
            throw new BizException(ResultCode.CANNOT_LEAVE_PERSONAL_TEAM);
        }
        // 3. 删除团队成员关系
        teamMemberMapper.deleteById(member.getId());
        // 4. 创建新的个人团队
        createPersonalTeam(userId, username, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickMember(Integer teamId, Integer targetUserId, String operatorUsername) {
        // 1. 检查目标用户是否在团队中
        TeamMember targetMember = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, targetUserId)
                        .eq(TeamMember::getTeamId, teamId)
        );
        if (targetMember == null) {
            throw new BizException(ResultCode.MEMBER_NOT_FOUND);
        }
        // 2. 不能踢出自己
        if (targetUserId.equals(teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getRole, TeamRole.LEADER.getValue())
        ).getUserId())) {
            throw new BizException(ResultCode.CANNOT_KICK_SELF);
        }
        // 3. 删除成员关系
        teamMemberMapper.deleteById(targetMember.getId());
        // 4. 获取被踢出用户的信息
        User targetUser = userMapper.selectById(targetUserId);
        // 5. 为被踢出用户创建个人团队
        createPersonalTeam(targetUserId, targetUser.getUsername(), 0);
    }

    @Override
    public TeamMemberVO getCurrentTeamInfo(Integer userId) {
        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
        );
        if (member == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return buildTeamMemberVO(member.getTeamId(), userId, user.getUsername(), member.getRole());
    }

    /**
     * 构建团队成员VO
     */
    private TeamMemberVO buildTeamMemberVO(Integer teamId, Integer userId, String username, String role) {
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BizException(ResultCode.TEAM_NOT_FOUND);
        }
        User leader = userMapper.selectById(team.getLeaderId());
        return TeamMemberVO.builder()
                .teamId(teamId)
                .teamName(team.getName())
                .pointBalance(team.getPointBalance())
                .leaderId(team.getLeaderId())
                .leaderName(leader != null ? leader.getUsername() : null)
                .role(role)
                .userId(userId)
                .username(username)
                .build();
    }
}
