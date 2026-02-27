package com.github.kokoachino.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.kokoachino.common.enums.EventTypeEnum;
import com.github.kokoachino.common.enums.InviteCodeStatusEnum;
import com.github.kokoachino.common.enums.TeamRoleEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.InviteCodeUtils;
import com.github.kokoachino.mapper.*;
import com.github.kokoachino.model.dto.*;
import com.github.kokoachino.model.entity.*;
import com.github.kokoachino.model.vo.*;
import com.github.kokoachino.service.OperationLogService;
import com.github.kokoachino.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;
    private final OperationLogService operationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createPersonalTeam(Integer userId, String username, Integer initialPoints) {
        Team team = new Team();
        team.setName(username + "的团队");
        team.setPointBalance(initialPoints);
        team.setLeaderId(userId);
        team.setOwnerId(userId);
        this.save(team);
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole(TeamRoleEnum.LEADER.getValue());
        teamMemberMapper.insert(member);
        return team.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InviteCodeVO generateInviteCode(Integer teamId, GenerateInviteCodeDTO dto, String username) {
        String rawCode;
        do {
            rawCode = InviteCodeUtils.generateRawCode();
        } while (inviteCodeMapper.selectCount(
                new LambdaQueryWrapper<TeamInviteCode>()
                        .eq(TeamInviteCode::getCode, rawCode)
        ) > 0);
        TeamInviteCode inviteCode = new TeamInviteCode();
        inviteCode.setTeamId(teamId);
        inviteCode.setCode(rawCode);
        inviteCode.setValidUntil(dto.getValidUntil());
        inviteCode.setMaxUses(dto.getMaxUses());
        inviteCode.setUsesCount(0);
        inviteCode.setStatus(InviteCodeStatusEnum.ACTIVE.getValue());
        inviteCodeMapper.insert(inviteCode);
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
        String rawCode = InviteCodeUtils.extractCodeFromText(dto.getInviteCodeText());
        if (rawCode == null) {
            throw new BizException(ResultCode.INVITE_CODE_INVALID);
        }
        TeamInviteCode inviteCode = inviteCodeMapper.selectOne(
                new LambdaQueryWrapper<TeamInviteCode>()
                        .eq(TeamInviteCode::getCode, rawCode)
        );
        if (inviteCode == null) {
            throw new BizException(ResultCode.INVITE_CODE_INVALID);
        }
        if (!InviteCodeStatusEnum.ACTIVE.getValue().equals(inviteCode.getStatus())) {
            throw new BizException(ResultCode.INVITE_CODE_INVALID);
        }
        if (inviteCode.getValidUntil() != null && inviteCode.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.INVITE_CODE_EXPIRED);
        }
        if (inviteCode.getMaxUses() != null && inviteCode.getUsesCount() >= inviteCode.getMaxUses()) {
            throw new BizException(ResultCode.INVITE_CODE_USED_UP);
        }
        Integer newTeamId = inviteCode.getTeamId();
        TeamMember existingMember = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getTeamId, newTeamId)
        );
        if (existingMember != null) {
            throw new BizException(ResultCode.ALREADY_IN_TEAM);
        }
        TeamMember currentMember = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
        );
        Integer oldTeamId = null;
        int pointsToTransfer = 0;
        if (currentMember != null) {
            oldTeamId = currentMember.getTeamId();
            long memberCount = teamMemberMapper.selectCount(
                    new LambdaQueryWrapper<TeamMember>()
                            .eq(TeamMember::getTeamId, oldTeamId)
            );
            Team oldTeam = this.getById(oldTeamId);
            if (memberCount == 1 && oldTeam.getPointBalance() > 0 && Boolean.TRUE.equals(dto.getTransferPoints())) {
                pointsToTransfer = oldTeam.getPointBalance();
                oldTeam.setPointBalance(0);
                this.updateById(oldTeam);
            }
            teamMemberMapper.deleteById(currentMember.getId());
        }
        TeamMember newMember = new TeamMember();
        newMember.setTeamId(newTeamId);
        newMember.setUserId(userId);
        newMember.setRole(TeamRoleEnum.MEMBER.getValue());
        teamMemberMapper.insert(newMember);
        inviteCode.setUsesCount(inviteCode.getUsesCount() + 1);
        inviteCodeMapper.updateById(inviteCode);
        if (pointsToTransfer > 0) {
            Team newTeam = this.getById(newTeamId);
            newTeam.setPointBalance(newTeam.getPointBalance() + pointsToTransfer);
            this.updateById(newTeam);
        }
        Team team = this.getById(newTeamId);
        Map<String, Object> details = new java.util.HashMap<>();
        details.put("inviteCode", rawCode);
        details.put("inviteCodeId", inviteCode.getId());
        details.put("previousTeam", oldTeamId != null ? "已退出原团队" : "无原团队");
        if (pointsToTransfer > 0) {
            details.put("transferredPoints", pointsToTransfer);
        }
        operationLogService.log(EventTypeEnum.TEAM_JOIN, newTeamId, userId, username, null, team.getName(), null, null, details);
        return buildTeamMemberVO(newTeamId, TeamRoleEnum.MEMBER.getValue());
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
        inviteCode.setStatus(InviteCodeStatusEnum.INACTIVE.getValue());
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
            String shareText = InviteCodeStatusEnum.ACTIVE.getValue().equals(code.getStatus())
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
        List<OperationLog> logs = operationLogMapper.selectInviteRecordsByInviteCodeId(codeId);
        return logs.stream().map(log -> {
            String inviteCode = null;
            if (log.getDetails() != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> details = objectMapper.readValue(log.getDetails(), Map.class);
                    inviteCode = (String) details.get("inviteCode");
                } catch (Exception ignored) {
                }
            }
            return InviteRecordVO.builder()
                    .id(log.getId())
                    .inviteCode(inviteCode)
                    .userId(log.getUserId())
                    .username(log.getUsername())
                    .joinedAt(log.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamMemberVO leaveTeam(Integer userId, String username) {
        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
        );
        if (member == null) {
            throw new BizException(ResultCode.MEMBER_NOT_FOUND);
        }
        Team team = this.getById(member.getTeamId());
        if (TeamRoleEnum.LEADER.getValue().equals(member.getRole()) && team.getLeaderId().equals(userId)) {
            throw new BizException(ResultCode.CANNOT_LEAVE_PERSONAL_TEAM);
        }
        operationLogService.log(EventTypeEnum.TEAM_LEAVE, team.getId(), userId, username, null, team.getName(),
                null, null, Map.of("role", member.getRole()));
        teamMemberMapper.deleteById(member.getId());
        createPersonalTeam(userId, username, 0);
        return buildTeamMemberVO(getCurrentTeamId(userId), TeamRoleEnum.LEADER.getValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickMember(Integer teamId, Integer targetUserId, String operatorUsername) {
        TeamMember targetMember = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, targetUserId)
                        .eq(TeamMember::getTeamId, teamId)
        );
        if (targetMember == null) {
            throw new BizException(ResultCode.MEMBER_NOT_FOUND);
        }
        if (targetUserId.equals(teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getRole, TeamRoleEnum.LEADER.getValue())
        ).getUserId())) {
            throw new BizException(ResultCode.CANNOT_KICK_SELF);
        }
        teamMemberMapper.deleteById(targetMember.getId());
        User targetUser = userMapper.selectById(targetUserId);
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
        return buildTeamMemberVO(member.getTeamId(), member.getRole());
    }

    @Override
    public Integer getCurrentTeamId(Integer userId) {
        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
        );
        if (member == null) {
            throw new BizException(ResultCode.MEMBER_NOT_FOUND);
        }
        return member.getTeamId();
    }

    @Override
    public boolean isTeamLeader(Integer userId, Integer teamId) {
        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getTeamId, teamId)
        );
        return member != null && TeamRoleEnum.LEADER.getValue().equals(member.getRole());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamMemberVO updateTeamName(Integer teamId, Integer userId, String username, UpdateTeamNameDTO dto) {
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BizException(ResultCode.TEAM_NOT_FOUND);
        }
        if (!isTeamLeader(userId, teamId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        String oldName = team.getName();
        team.setName(dto.getName());
        this.updateById(team);
        operationLogService.log(EventTypeEnum.TEAM_UPDATE, teamId, dto.getName(),
                Map.of("oldName", oldName, "newName", dto.getName()));
        return buildTeamMemberVO(teamId, TeamRoleEnum.LEADER.getValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamMemberVO transferLeader(Integer teamId, Integer currentLeaderId, String username, TransferLeaderDTO dto) {
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BizException(ResultCode.TEAM_NOT_FOUND);
        }
        if (!isTeamLeader(currentLeaderId, teamId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        Integer newLeaderId = dto.getNewLeaderId();
        if (newLeaderId.equals(currentLeaderId)) {
            throw new BizException(ResultCode.CANNOT_TRANSFER_TO_SELF);
        }
        TeamMember newLeaderMember = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, newLeaderId)
                        .eq(TeamMember::getTeamId, teamId)
        );
        if (newLeaderMember == null) {
            throw new BizException(ResultCode.MEMBER_NOT_FOUND);
        }
        TeamMember currentLeaderMember = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, currentLeaderId)
                        .eq(TeamMember::getTeamId, teamId)
        );
        currentLeaderMember.setRole(TeamRoleEnum.MEMBER.getValue());
        teamMemberMapper.updateById(currentLeaderMember);
        newLeaderMember.setRole(TeamRoleEnum.LEADER.getValue());
        teamMemberMapper.updateById(newLeaderMember);
        team.setLeaderId(newLeaderId);
        this.updateById(team);
        User newLeader = userMapper.selectById(newLeaderId);
        operationLogService.log(EventTypeEnum.TEAM_TRANSFER, teamId, team.getName(),
                Map.of("oldLeaderId", currentLeaderId, "newLeaderId", newLeaderId, "newLeaderName", newLeader.getUsername()));
        return buildTeamMemberVO(teamId, TeamRoleEnum.MEMBER.getValue());
    }

    private TeamMemberVO buildTeamMemberVO(Integer teamId, String currentRole) {
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BizException(ResultCode.TEAM_NOT_FOUND);
        }
        User leader = userMapper.selectById(team.getLeaderId());
        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
        );
        Map<Integer, TeamMember> memberMap = members.stream()
                .collect(java.util.stream.Collectors.toMap(TeamMember::getUserId, m -> m));
        List<UserVO> memberVOList = new ArrayList<>();
        if (leader != null) {
            TeamMember leaderMember = memberMap.get(leader.getId());
            memberVOList.add(UserVO.builder()
                    .id(leader.getId())
                    .username(leader.getUsername())
                    .email(leader.getEmail())
                    .role("leader")
                    .joinedAt(leaderMember != null && leaderMember.getJoinedAt() != null ? leaderMember.getJoinedAt().toString() : null)
                    .build());
        }
        List<UserVO> otherMembers = members.stream()
                .filter(m -> !m.getUserId().equals(team.getLeaderId()))
                .map(m -> {
                    User user = userMapper.selectById(m.getUserId());
                    if (user != null) {
                        return UserVO.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .role(m.getRole())
                                .joinedAt(m.getJoinedAt() != null ? m.getJoinedAt().toString() : null)
                                .build();
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(u -> com.github.kokoachino.common.util.PinyinUtils.toPinyin(u.getUsername())))
                .toList();
        memberVOList.addAll(otherMembers);
        return TeamMemberVO.builder()
                .teamId(teamId)
                .teamName(team.getName())
                .pointBalance(team.getPointBalance())
                .leaderId(team.getLeaderId())
                .leaderName(leader != null ? leader.getUsername() : null)
                .role(currentRole)
                .members(memberVOList)
                .build();
    }
}
