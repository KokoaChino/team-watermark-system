package com.github.kokoachino.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.kokoachino.common.enums.InviteCodeStatusEnum;
import com.github.kokoachino.common.enums.LogUserStatusEnum;
import com.github.kokoachino.common.enums.PointChangeTypeEnum;
import com.github.kokoachino.common.enums.PointSourceTypeEnum;
import com.github.kokoachino.common.enums.TeamEventTypeEnum;
import com.github.kokoachino.common.enums.TeamRoleEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.InviteCodeUtils;
import com.github.kokoachino.common.util.TeamContext;
import com.github.kokoachino.mapper.BatchTaskMapper;
import com.github.kokoachino.mapper.FontMapper;
import com.github.kokoachino.mapper.PaymentOrderMapper;
import com.github.kokoachino.mapper.PointChangeLogMapper;
import com.github.kokoachino.mapper.TeamInviteCodeMapper;
import com.github.kokoachino.mapper.TeamMapper;
import com.github.kokoachino.mapper.TeamMemberMapper;
import com.github.kokoachino.mapper.TeamEventLogMapper;
import com.github.kokoachino.mapper.UserMapper;
import com.github.kokoachino.mapper.WatermarkResourceLogMapper;
import com.github.kokoachino.mapper.WatermarkTemplateDraftMapper;
import com.github.kokoachino.mapper.WatermarkTemplateMapper;
import com.github.kokoachino.model.dto.GenerateInviteCodeDTO;
import com.github.kokoachino.model.dto.JoinTeamDTO;
import com.github.kokoachino.model.dto.PointChangeLogRecordDTO;
import com.github.kokoachino.model.dto.TeamEventLogRecordDTO;
import com.github.kokoachino.model.dto.TransferLeaderDTO;
import com.github.kokoachino.model.dto.UpdateTeamNameDTO;
import com.github.kokoachino.model.entity.BatchTask;
import com.github.kokoachino.model.entity.Font;
import com.github.kokoachino.model.entity.PaymentOrder;
import com.github.kokoachino.model.entity.PointChangeLog;
import com.github.kokoachino.model.entity.Team;
import com.github.kokoachino.model.entity.TeamEventLog;
import com.github.kokoachino.model.entity.TeamInviteCode;
import com.github.kokoachino.model.entity.TeamMember;
import com.github.kokoachino.model.entity.User;
import com.github.kokoachino.model.entity.WatermarkResourceLog;
import com.github.kokoachino.model.entity.WatermarkTemplate;
import com.github.kokoachino.model.entity.WatermarkTemplateDraft;
import com.github.kokoachino.model.vo.InviteCodeVO;
import com.github.kokoachino.model.vo.InviteRecordVO;
import com.github.kokoachino.model.vo.TeamMemberVO;
import com.github.kokoachino.model.vo.UserVO;
import com.github.kokoachino.service.LogUserStatusSyncService;
import com.github.kokoachino.service.PointChangeLogService;
import com.github.kokoachino.service.TeamEventLogService;
import com.github.kokoachino.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 团队服务实现类
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Service
@RequiredArgsConstructor
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    private final TeamMemberMapper teamMemberMapper;
    private final TeamInviteCodeMapper inviteCodeMapper;
    private final TeamEventLogMapper teamEventLogMapper;
    private final UserMapper userMapper;
    private final BatchTaskMapper batchTaskMapper;
    private final PointChangeLogMapper pointChangeLogMapper;
    private final WatermarkResourceLogMapper watermarkResourceLogMapper;
    private final WatermarkTemplateMapper watermarkTemplateMapper;
    private final WatermarkTemplateDraftMapper watermarkTemplateDraftMapper;
    private final FontMapper fontMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final TeamEventLogService teamEventLogService;
    private final LogUserStatusSyncService logUserStatusSyncService;
    private final PointChangeLogService pointChangeLogService;

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
    public InviteCodeVO generateInviteCode(Integer teamId, Integer userId, String username, GenerateInviteCodeDTO dto) {
        if (!TeamContext.isLeader()) {
            throw new BizException(ResultCode.NOT_TEAM_LEADER);
        }
        String rawCode;
        do {
            rawCode = InviteCodeUtils.generateRawCode();
        } while (inviteCodeMapper.selectCount(new LambdaQueryWrapper<TeamInviteCode>().eq(TeamInviteCode::getCode, rawCode)) > 0);
        TeamInviteCode inviteCode = new TeamInviteCode();
        inviteCode.setTeamId(teamId);
        inviteCode.setCode(rawCode);
        inviteCode.setValidUntil(dto.getValidUntil());
        inviteCode.setMaxUses(dto.getMaxUses());
        inviteCode.setUsesCount(0);
        inviteCode.setStatus(InviteCodeStatusEnum.ACTIVE.getValue());
        inviteCode.setCreatedById(userId);
        inviteCodeMapper.insert(inviteCode);
        Team team = this.getById(teamId);
        String shareText = InviteCodeUtils.generateShareText(team.getName(), rawCode);
        Map<String, Object> afterData = new HashMap<>();
        afterData.put("status", inviteCode.getStatus());
        afterData.put("validUntil", inviteCode.getValidUntil());
        afterData.put("maxUses", inviteCode.getMaxUses());
        afterData.put("usesCount", inviteCode.getUsesCount());
        Map<String, Object> details = new HashMap<>();
        details.put("shareText", shareText);
        teamEventLogService.record(TeamEventLogRecordDTO.builder()
                .teamId(teamId)
                .eventType(TeamEventTypeEnum.INVITE_CODE_CREATE)
                .operatorUserId(userId)
                .operatorUsername(username)
                .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .inviteCodeId(inviteCode.getId())
                .inviteCode(rawCode)
                .afterData(afterData)
                .details(details)
                .build());
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
        TeamInviteCode inviteCode = inviteCodeMapper.selectOne(new LambdaQueryWrapper<TeamInviteCode>().eq(TeamInviteCode::getCode, rawCode));
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
        TeamMember existingMember = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getUserId, userId)
                .eq(TeamMember::getTeamId, newTeamId));
        if (existingMember != null) {
            throw new BizException(ResultCode.ALREADY_IN_TEAM);
        }
        Long usedCount = teamEventLogMapper.selectCount(new LambdaQueryWrapper<TeamEventLog>()
                .eq(TeamEventLog::getTeamId, newTeamId)
                .eq(TeamEventLog::getEventType, TeamEventTypeEnum.MEMBER_JOIN.getValue())
                .eq(TeamEventLog::getInviteCodeId, inviteCode.getId())
                .and(wrapper -> wrapper.eq(TeamEventLog::getAffectedUserId, userId)
                        .or()
                        .eq(TeamEventLog::getOperatorUserId, userId)));
        if (usedCount != null && usedCount > 0) {
            throw new BizException(ResultCode.INVITE_CODE_ALREADY_USED_BY_USER);
        }
        TeamMember currentMember = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        Integer oldTeamId = null;
        String oldTeamName = null;
        int pointsToTransfer = 0;
        int reservedPoints = 0;
        if (currentMember != null) {
            oldTeamId = currentMember.getTeamId();
            Team oldTeam = this.getById(oldTeamId);
            long memberCount = teamMemberMapper.selectCount(new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, oldTeamId));
            oldTeamName = oldTeam != null ? oldTeam.getName() : null;
            if (oldTeam != null
                    && memberCount > 1
                    && TeamRoleEnum.LEADER.getValue().equals(currentMember.getRole())
                    && userId.equals(oldTeam.getLeaderId())) {
                transferLeaderToEarliestMember(oldTeam, userId, username, "join_other_team");
            }
            boolean isSingleMemberTeam = memberCount == 1;
            int oldTeamBalance = oldTeam != null && oldTeam.getPointBalance() != null ? oldTeam.getPointBalance() : 0;
            boolean shouldTransferPoints = isSingleMemberTeam
                    && oldTeamBalance > 0
                    && Boolean.TRUE.equals(dto.getTransferPoints());
            if (shouldTransferPoints) {
                pointsToTransfer = oldTeamBalance;
            } else if (isSingleMemberTeam && oldTeamBalance > 0) {
                reservedPoints = oldTeamBalance;
            }
            teamMemberMapper.deleteById(currentMember.getId());
            logUserStatusSyncService.syncUserStatus(oldTeamId, userId, username, LogUserStatusEnum.LEFT);
            if (oldTeam != null && isSingleMemberTeam) {
                clearTeamAssets(oldTeamId);
                if (reservedPoints > 0) {
                    oldTeam.setPointBalance(reservedPoints);
                    oldTeam.setOwnerId(userId);
                    oldTeam.setLeaderId(userId);
                    this.updateById(oldTeam);
                } else {
                    this.removeById(oldTeamId);
                }
            }
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
            int balanceBefore = newTeam.getPointBalance() == null ? 0 : newTeam.getPointBalance();
            int balanceAfter = balanceBefore + pointsToTransfer;
            newTeam.setPointBalance(balanceAfter);
            this.updateById(newTeam);
            pointChangeLogService.record(PointChangeLogRecordDTO.builder()
                    .teamId(newTeamId)
                    .changeType(PointChangeTypeEnum.RECHARGE)
                    .operatorUserId(userId)
                    .operatorUsername(username)
                    .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                    .sourceType(PointSourceTypeEnum.TEAM_TRANSFER)
                    .sourceId(username + "加入团队")
                    .points(pointsToTransfer)
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .description("成员加入团队并转移原团队余额")
                    .build());
        }
        Team team = this.getById(newTeamId);
        Map<String, Object> details = new HashMap<>();
        details.put("targetTeamName", team.getName());
        details.put("previousTeamId", oldTeamId);
        details.put("previousTeamName", oldTeamName);
        details.put("transferredPoints", pointsToTransfer);
        details.put("reservedPoints", reservedPoints);
        teamEventLogService.record(TeamEventLogRecordDTO.builder()
                .teamId(newTeamId)
                .eventType(TeamEventTypeEnum.MEMBER_JOIN)
                .operatorUserId(userId)
                .operatorUsername(username)
                .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .affectedUserId(userId)
                .affectedUsername(username)
                .affectedUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .inviteCodeId(inviteCode.getId())
                .inviteCode(rawCode)
                .details(details)
                .build());
        return buildTeamMemberVO(newTeamId, TeamRoleEnum.MEMBER.getValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateInviteCode(Integer codeId, Integer teamId, Integer operatorUserId, String operatorUsername) {
        TeamInviteCode inviteCode = inviteCodeMapper.selectById(codeId);
        if (inviteCode == null) {
            throw new BizException(ResultCode.INVITE_CODE_NOT_FOUND);
        }
        if (!inviteCode.getTeamId().equals(teamId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (!TeamContext.isLeader()) {
            throw new BizException(ResultCode.NOT_TEAM_LEADER);
        }
        Map<String, Object> beforeData = Map.of("status", inviteCode.getStatus());
        inviteCode.setStatus(InviteCodeStatusEnum.INACTIVE.getValue());
        inviteCodeMapper.updateById(inviteCode);
        Map<String, Object> afterData = Map.of("status", inviteCode.getStatus());
        teamEventLogService.record(TeamEventLogRecordDTO.builder()
                .teamId(teamId)
                .eventType(TeamEventTypeEnum.INVITE_CODE_DEACTIVATE)
                .operatorUserId(operatorUserId)
                .operatorUsername(operatorUsername)
                .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .inviteCodeId(inviteCode.getId())
                .inviteCode(inviteCode.getCode())
                .beforeData(beforeData)
                .afterData(afterData)
                .build());
    }

    @Override
    public List<InviteCodeVO> getInviteCodesByTeamId(Integer teamId) {
        List<TeamInviteCode> codes = inviteCodeMapper.selectList(new LambdaQueryWrapper<TeamInviteCode>()
                .eq(TeamInviteCode::getTeamId, teamId)
                .orderByDesc(TeamInviteCode::getCreatedAt));
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
        return teamEventLogService.getInviteRecords(codeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamMemberVO leaveTeam(Integer userId, String username) {
        TeamMember member = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        if (member == null) {
            throw new BizException(ResultCode.MEMBER_NOT_FOUND);
        }
        Team team = this.getById(member.getTeamId());
        long memberCount = teamMemberMapper.selectCount(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, team.getId()));
        if (memberCount <= 1) {
            throw new BizException(ResultCode.CANNOT_LEAVE_PERSONAL_TEAM);
        }
        if (TeamRoleEnum.LEADER.getValue().equals(member.getRole()) && team.getLeaderId().equals(userId)) {
            transferLeaderToEarliestMember(team, userId, username, "manual_leave");
        }
        Map<String, Object> details = new HashMap<>();
        details.put("teamName", team.getName());
        details.put("role", member.getRole());
        details.put("reason", "manual_leave");
        teamEventLogService.record(TeamEventLogRecordDTO.builder()
                .teamId(team.getId())
                .eventType(TeamEventTypeEnum.MEMBER_LEAVE)
                .operatorUserId(userId)
                .operatorUsername(username)
                .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .affectedUserId(userId)
                .affectedUsername(username)
                .affectedUserStatus(LogUserStatusEnum.LEFT.getValue())
                .details(details)
                .build());
        logUserStatusSyncService.syncUserStatus(team.getId(), userId, username, LogUserStatusEnum.LEFT);
        teamMemberMapper.deleteById(member.getId());
        int restoredPoints = consumeReservedPoints(userId);
        createPersonalTeam(userId, username, restoredPoints);
        return buildTeamMemberVO(getCurrentTeamId(userId), TeamRoleEnum.LEADER.getValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickMember(Integer teamId, Integer operatorUserId, String operatorUsername, Integer targetUserId) {
        TeamMember targetMember = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getUserId, targetUserId)
                .eq(TeamMember::getTeamId, teamId));
        if (targetMember == null) {
            throw new BizException(ResultCode.MEMBER_NOT_FOUND);
        }
        if (!TeamContext.isLeader()) {
            throw new BizException(ResultCode.NOT_TEAM_LEADER);
        }
        Integer leaderUserId = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getRole, TeamRoleEnum.LEADER.getValue())).getUserId();
        if (targetUserId.equals(leaderUserId)) {
            throw new BizException(ResultCode.CANNOT_KICK_SELF);
        }
        User targetUser = userMapper.selectById(targetUserId);
        Map<String, Object> details = new HashMap<>();
        details.put("role", targetMember.getRole());
        details.put("reason", "leader_kick");
        teamEventLogService.record(TeamEventLogRecordDTO.builder()
                .teamId(teamId)
                .eventType(TeamEventTypeEnum.MEMBER_KICK)
                .operatorUserId(operatorUserId)
                .operatorUsername(operatorUsername)
                .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .affectedUserId(targetUserId)
                .affectedUsername(targetUser != null ? targetUser.getUsername() : null)
                .affectedUserStatus(LogUserStatusEnum.LEFT.getValue())
                .details(details)
                .build());
        logUserStatusSyncService.syncUserStatus(teamId, targetUserId, targetUser != null ? targetUser.getUsername() : null, LogUserStatusEnum.LEFT);
        teamMemberMapper.deleteById(targetMember.getId());
        if (targetUser != null) {
            int restoredPoints = consumeReservedPoints(targetUserId);
            createPersonalTeam(targetUserId, targetUser.getUsername(), restoredPoints);
        }
    }

    @Override
    public TeamMemberVO getCurrentTeamInfo(Integer userId) {
        TeamMember member = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        if (member == null) {
            return null;
        }
        return buildTeamMemberVO(member.getTeamId(), member.getRole());
    }

    @Override
    public Integer getCurrentTeamId(Integer userId) {
        TeamMember member = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        if (member == null) {
            throw new BizException(ResultCode.MEMBER_NOT_FOUND);
        }
        return member.getTeamId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamMemberVO updateTeamName(Integer teamId, Integer userId, String username, UpdateTeamNameDTO dto) {
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BizException(ResultCode.TEAM_NOT_FOUND);
        }
        if (!TeamContext.isLeader()) {
            throw new BizException(ResultCode.NOT_TEAM_LEADER);
        }
        String oldName = team.getName();
        team.setName(dto.getName());
        this.updateById(team);
        teamEventLogService.record(TeamEventLogRecordDTO.builder()
                .teamId(teamId)
                .eventType(TeamEventTypeEnum.TEAM_RENAME)
                .operatorUserId(userId)
                .operatorUsername(username)
                .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .beforeData(Map.of("teamName", oldName))
                .afterData(Map.of("teamName", dto.getName()))
                .build());
        return buildTeamMemberVO(teamId, TeamRoleEnum.LEADER.getValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamMemberVO transferLeader(Integer teamId, Integer currentLeaderId, String username, TransferLeaderDTO dto) {
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BizException(ResultCode.TEAM_NOT_FOUND);
        }
        if (!TeamContext.isLeader()) {
            throw new BizException(ResultCode.NOT_TEAM_LEADER);
        }
        Integer newLeaderId = dto.getNewLeaderId();
        if (newLeaderId.equals(currentLeaderId)) {
            throw new BizException(ResultCode.CANNOT_TRANSFER_TO_SELF);
        }
        TeamMember newLeaderMember = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getUserId, newLeaderId)
                .eq(TeamMember::getTeamId, teamId));
        if (newLeaderMember == null) {
            throw new BizException(ResultCode.MEMBER_NOT_FOUND);
        }
        TeamMember currentLeaderMember = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getUserId, currentLeaderId)
                .eq(TeamMember::getTeamId, teamId));
        currentLeaderMember.setRole(TeamRoleEnum.MEMBER.getValue());
        teamMemberMapper.updateById(currentLeaderMember);
        newLeaderMember.setRole(TeamRoleEnum.LEADER.getValue());
        teamMemberMapper.updateById(newLeaderMember);
        team.setLeaderId(newLeaderId);
        this.updateById(team);
        User newLeader = userMapper.selectById(newLeaderId);
        teamEventLogService.record(TeamEventLogRecordDTO.builder()
                .teamId(teamId)
                .eventType(TeamEventTypeEnum.LEADER_TRANSFER)
                .operatorUserId(currentLeaderId)
                .operatorUsername(username)
                .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .affectedUserId(newLeaderId)
                .affectedUsername(newLeader != null ? newLeader.getUsername() : null)
                .affectedUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .beforeData(Map.of("leaderId", currentLeaderId, "leaderName", username))
                .afterData(Map.of("leaderId", newLeaderId, "leaderName", newLeader != null ? newLeader.getUsername() : ""))
                .build());
        return buildTeamMemberVO(teamId, TeamRoleEnum.MEMBER.getValue());
    }

    private TeamMemberVO buildTeamMemberVO(Integer teamId, String currentRole) {
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BizException(ResultCode.TEAM_NOT_FOUND);
        }
        User leader = userMapper.selectById(team.getLeaderId());
        List<TeamMember> members = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId));
        Map<Integer, TeamMember> memberMap = members.stream().collect(Collectors.toMap(TeamMember::getUserId, m -> m));
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

    private void transferLeaderToEarliestMember(Team team, Integer currentLeaderId, String currentLeaderName, String reason) {
        List<TeamMember> otherMembers = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, team.getId())
                .ne(TeamMember::getUserId, currentLeaderId)
                .orderByAsc(TeamMember::getJoinedAt, TeamMember::getCreatedAt, TeamMember::getId));
        if (otherMembers.isEmpty()) {
            return;
        }
        TeamMember nextLeader = otherMembers.getFirst();
        nextLeader.setRole(TeamRoleEnum.LEADER.getValue());
        teamMemberMapper.updateById(nextLeader);
        team.setLeaderId(nextLeader.getUserId());
        this.updateById(team);
        User nextLeaderUser = userMapper.selectById(nextLeader.getUserId());
        Map<String, Object> details = new HashMap<>();
        details.put("teamName", team.getName());
        details.put("reason", reason);
        teamEventLogService.record(TeamEventLogRecordDTO.builder()
                .teamId(team.getId())
                .eventType(TeamEventTypeEnum.LEADER_TRANSFER)
                .operatorUserId(currentLeaderId)
                .operatorUsername(currentLeaderName)
                .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .affectedUserId(nextLeader.getUserId())
                .affectedUsername(nextLeaderUser != null ? nextLeaderUser.getUsername() : null)
                .affectedUserStatus(nextLeaderUser != null ? LogUserStatusEnum.ACTIVE.getValue() : LogUserStatusEnum.DELETED.getValue())
                .beforeData(Map.of("leaderId", currentLeaderId, "leaderName", currentLeaderName != null ? currentLeaderName : ""))
                .afterData(Map.of("leaderId", nextLeader.getUserId(), "leaderName", nextLeaderUser != null ? nextLeaderUser.getUsername() : ""))
                .details(details)
                .build());
    }

    private void clearTeamAssets(Integer teamId) {
        List<Integer> templateIds = watermarkTemplateMapper.selectList(new LambdaQueryWrapper<WatermarkTemplate>()
                        .select(WatermarkTemplate::getId)
                        .eq(WatermarkTemplate::getTeamId, teamId))
                .stream()
                .map(WatermarkTemplate::getId)
                .toList();
        if (!templateIds.isEmpty()) {
            watermarkTemplateDraftMapper.delete(new LambdaQueryWrapper<WatermarkTemplateDraft>()
                    .in(WatermarkTemplateDraft::getSourceTemplateId, templateIds));
        }
        batchTaskMapper.delete(new LambdaQueryWrapper<BatchTask>().eq(BatchTask::getTeamId, teamId));
        watermarkResourceLogMapper.delete(new LambdaQueryWrapper<WatermarkResourceLog>().eq(WatermarkResourceLog::getTeamId, teamId));
        pointChangeLogMapper.delete(new LambdaQueryWrapper<PointChangeLog>().eq(PointChangeLog::getTeamId, teamId));
        teamEventLogMapper.delete(new LambdaQueryWrapper<TeamEventLog>().eq(TeamEventLog::getTeamId, teamId));
        inviteCodeMapper.delete(new LambdaQueryWrapper<TeamInviteCode>().eq(TeamInviteCode::getTeamId, teamId));
        paymentOrderMapper.delete(new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getTeamId, teamId));
        watermarkTemplateMapper.delete(new LambdaQueryWrapper<WatermarkTemplate>().eq(WatermarkTemplate::getTeamId, teamId));
        fontMapper.delete(new LambdaQueryWrapper<Font>().eq(Font::getTeamId, teamId));
        teamMemberMapper.delete(new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId));
    }

    private int consumeReservedPoints(Integer userId) {
        List<Team> ownedTeams = this.list(new LambdaQueryWrapper<Team>().eq(Team::getOwnerId, userId));
        int restoredPoints = 0;
        for (Team ownedTeam : ownedTeams) {
            long memberCount = teamMemberMapper.selectCount(new LambdaQueryWrapper<TeamMember>()
                    .eq(TeamMember::getTeamId, ownedTeam.getId()));
            if (memberCount == 0) {
                if (ownedTeam.getPointBalance() != null && ownedTeam.getPointBalance() > 0) {
                    restoredPoints += ownedTeam.getPointBalance();
                }
                clearTeamAssets(ownedTeam.getId());
                this.removeById(ownedTeam.getId());
            }
        }
        return restoredPoints;
    }
}
