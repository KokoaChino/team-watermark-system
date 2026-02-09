package com.github.kokoachino.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.kokoachino.common.enums.InviteCodeStatus;
import com.github.kokoachino.common.enums.TeamRole;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.InviteCodeUtils;
import com.github.kokoachino.mapper.*;
import com.github.kokoachino.model.dto.*;
import com.github.kokoachino.model.entity.*;
import com.github.kokoachino.model.vo.*;
import com.github.kokoachino.service.impl.TeamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 团队服务测试类
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TeamServiceTest {

    @Mock
    private TeamMapper teamMapper;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private TeamInviteCodeMapper inviteCodeMapper;

    @Mock
    private TeamInviteRecordMapper inviteRecordMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private TeamServiceImpl teamService;

    private User testUser;
    private Team testTeam;
    private TeamMember testTeamMember;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(teamService, "baseMapper", teamMapper);

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testTeam = new Team();
        testTeam.setId(1);
        testTeam.setName("测试团队");
        testTeam.setPointBalance(10000);
        testTeam.setLeaderId(1);

        testTeamMember = new TeamMember();
        testTeamMember.setId(1);
        testTeamMember.setTeamId(1);
        testTeamMember.setUserId(1);
        testTeamMember.setRole(TeamRole.LEADER.getValue());
    }

    @Test
    void createPersonalTeam_Success() {
        // Arrange
        when(teamMapper.insert(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(1);
            return 1;
        });
        when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

        // Act
        Integer teamId = teamService.createPersonalTeam(1, "testuser", 10000);

        // Assert
        assertEquals(1, teamId);
        verify(teamMapper).insert(any(Team.class));
        verify(teamMemberMapper).insert(any(TeamMember.class));
    }

    @Test
    void generateInviteCode_Success() {
        // Arrange
        GenerateInviteCodeDTO dto = new GenerateInviteCodeDTO();
        dto.setValidUntil(LocalDateTime.now().plusDays(7));
        dto.setMaxUses(10);

        when(inviteCodeMapper.selectCount(any())).thenReturn(0L);
        when(inviteCodeMapper.insert(any(TeamInviteCode.class))).thenAnswer(invocation -> {
            TeamInviteCode code = invocation.getArgument(0);
            code.setId(1);
            return 1;
        });
        when(teamMapper.selectById(1)).thenReturn(testTeam);

        // Act
        InviteCodeVO result = teamService.generateInviteCode(1, dto, "testuser");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCode());
        assertEquals(6, result.getCode().length());
        assertEquals(10, result.getMaxUses());
        assertEquals(0, result.getUsesCount());
        assertEquals(InviteCodeStatus.ACTIVE.getValue(), result.getStatus());
        assertNotNull(result.getShareText());
        verify(inviteCodeMapper).insert(any(TeamInviteCode.class));
    }

    @Test
    void joinTeam_Success() {
        // Arrange - 使用有效的邀请码字符（排除0, O, 1, I, L）
        JoinTeamDTO dto = new JoinTeamDTO();
        dto.setInviteCodeText("快来加入测试团队【ABCD23】，一起协作处理图片水印吧！");

        TeamInviteCode inviteCode = new TeamInviteCode();
        inviteCode.setId(1);
        inviteCode.setTeamId(2);
        inviteCode.setCode("ABCD23");
        inviteCode.setValidUntil(LocalDateTime.now().plusDays(7));
        inviteCode.setMaxUses(10);
        inviteCode.setUsesCount(0);
        inviteCode.setStatus(InviteCodeStatus.ACTIVE.getValue());

        Team targetTeam = new Team();
        targetTeam.setId(2);
        targetTeam.setName("目标团队");
        targetTeam.setPointBalance(5000);
        targetTeam.setLeaderId(2);

        User leader = new User();
        leader.setId(2);
        leader.setUsername("leader");

        when(inviteCodeMapper.selectOne(any())).thenReturn(inviteCode);
        when(teamMemberMapper.selectOne(any())).thenReturn(null); // 不在团队中
        when(teamMapper.selectById(2)).thenReturn(targetTeam);
        when(userMapper.selectById(2)).thenReturn(leader);
        when(teamMemberMapper.delete(any())).thenReturn(1);
        when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);
        when(inviteCodeMapper.updateById(any(TeamInviteCode.class))).thenReturn(1);
        when(inviteRecordMapper.insert(any(TeamInviteRecord.class))).thenReturn(1);

        // Act
        TeamMemberVO result = teamService.joinTeam(1, "testuser", dto);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTeamId());
        assertEquals(TeamRole.MEMBER.getValue(), result.getRole());
        verify(teamMemberMapper).delete(any());
        verify(teamMemberMapper).insert(any(TeamMember.class));
        verify(inviteCodeMapper).updateById(any(TeamInviteCode.class));
        verify(inviteRecordMapper).insert(any(TeamInviteRecord.class));
    }

    @Test
    void joinTeam_InvalidCodeFormat() {
        // Arrange
        JoinTeamDTO dto = new JoinTeamDTO();
        dto.setInviteCodeText("无效的邀请码文本");

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            teamService.joinTeam(1, "testuser", dto);
        });
        assertEquals(ResultCode.INVITE_CODE_INVALID, exception.getResultCode());
    }

    @Test
    void joinTeam_CodeNotFound() {
        // Arrange
        JoinTeamDTO dto = new JoinTeamDTO();
        dto.setInviteCodeText("快来加入测试团队【ABCD12】");

        when(inviteCodeMapper.selectOne(any())).thenReturn(null);

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            teamService.joinTeam(1, "testuser", dto);
        });
        assertEquals(ResultCode.INVITE_CODE_INVALID, exception.getResultCode());
    }

    @Test
    void joinTeam_CodeExpired() {
        // Arrange - 使用有效的邀请码字符（排除0, O, 1, I, L）
        JoinTeamDTO dto = new JoinTeamDTO();
        dto.setInviteCodeText("快来加入测试团队【ABCD23】");

        TeamInviteCode inviteCode = new TeamInviteCode();
        inviteCode.setCode("ABCD23");
        inviteCode.setValidUntil(LocalDateTime.now().minusDays(1)); // 已过期
        inviteCode.setStatus(InviteCodeStatus.ACTIVE.getValue());

        when(inviteCodeMapper.selectOne(any())).thenReturn(inviteCode);

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            teamService.joinTeam(1, "testuser", dto);
        });
        assertEquals(ResultCode.INVITE_CODE_EXPIRED, exception.getResultCode());
    }

    @Test
    void joinTeam_CodeUsedUp() {
        // Arrange - 使用有效的邀请码字符（排除0, O, 1, I, L）
        JoinTeamDTO dto = new JoinTeamDTO();
        dto.setInviteCodeText("快来加入测试团队【ABCD23】");

        TeamInviteCode inviteCode = new TeamInviteCode();
        inviteCode.setCode("ABCD23");
        inviteCode.setValidUntil(LocalDateTime.now().plusDays(7));
        inviteCode.setMaxUses(5);
        inviteCode.setUsesCount(5); // 已达上限
        inviteCode.setStatus(InviteCodeStatus.ACTIVE.getValue());

        when(inviteCodeMapper.selectOne(any())).thenReturn(inviteCode);

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            teamService.joinTeam(1, "testuser", dto);
        });
        assertEquals(ResultCode.INVITE_CODE_USED_UP, exception.getResultCode());
    }

    @Test
    void joinTeam_AlreadyInTeam() {
        // Arrange - 使用有效的邀请码字符（排除0, O, 1, I, L）
        JoinTeamDTO dto = new JoinTeamDTO();
        dto.setInviteCodeText("快来加入测试团队【ABCD23】");

        TeamInviteCode inviteCode = new TeamInviteCode();
        inviteCode.setTeamId(1);
        inviteCode.setCode("ABCD23");
        inviteCode.setValidUntil(LocalDateTime.now().plusDays(7));
        inviteCode.setMaxUses(10);
        inviteCode.setUsesCount(0);
        inviteCode.setStatus(InviteCodeStatus.ACTIVE.getValue());

        when(inviteCodeMapper.selectOne(any())).thenReturn(inviteCode);
        when(teamMemberMapper.selectOne(any())).thenReturn(testTeamMember); // 已在团队中

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            teamService.joinTeam(1, "testuser", dto);
        });
        assertEquals(ResultCode.ALREADY_IN_TEAM, exception.getResultCode());
    }

    @Test
    void deactivateInviteCode_Success() {
        // Arrange
        TeamInviteCode inviteCode = new TeamInviteCode();
        inviteCode.setId(1);
        inviteCode.setTeamId(1);
        inviteCode.setStatus(InviteCodeStatus.ACTIVE.getValue());

        when(inviteCodeMapper.selectById(1)).thenReturn(inviteCode);
        when(inviteCodeMapper.updateById(any(TeamInviteCode.class))).thenReturn(1);

        // Act
        teamService.deactivateInviteCode(1, 1);

        // Assert
        verify(inviteCodeMapper).updateById((TeamInviteCode) argThat(code ->
                InviteCodeStatus.INACTIVE.getValue().equals(((TeamInviteCode) code).getStatus())));
    }

    @Test
    void deactivateInviteCode_NotFound() {
        // Arrange
        when(inviteCodeMapper.selectById(1)).thenReturn(null);

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            teamService.deactivateInviteCode(1, 1);
        });
        assertEquals(ResultCode.INVITE_CODE_NOT_FOUND, exception.getResultCode());
    }

    @Test
    void deactivateInviteCode_Forbidden() {
        // Arrange
        TeamInviteCode inviteCode = new TeamInviteCode();
        inviteCode.setId(1);
        inviteCode.setTeamId(2); // 不属于当前团队

        when(inviteCodeMapper.selectById(1)).thenReturn(inviteCode);

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            teamService.deactivateInviteCode(1, 1);
        });
        assertEquals(ResultCode.FORBIDDEN, exception.getResultCode());
    }

    @Test
    void getInviteCodesByTeamId_Success() {
        // Arrange
        TeamInviteCode code1 = new TeamInviteCode();
        code1.setId(1);
        code1.setCode("ABCD12");
        code1.setValidUntil(LocalDateTime.now().plusDays(7));
        code1.setMaxUses(10);
        code1.setUsesCount(2);
        code1.setStatus(InviteCodeStatus.ACTIVE.getValue());

        when(inviteCodeMapper.selectList(any())).thenReturn(Collections.singletonList(code1));
        when(teamMapper.selectById(1)).thenReturn(testTeam);

        // Act
        List<InviteCodeVO> result = teamService.getInviteCodesByTeamId(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ABCD12", result.get(0).getCode());
        assertEquals(2, result.get(0).getUsesCount());
        assertNotNull(result.get(0).getShareText());
    }

    @Test
    void leaveTeam_Success() {
        // Arrange
        TeamMember member = new TeamMember();
        member.setId(1);
        member.setTeamId(2); // 不是个人团队
        member.setUserId(1);
        member.setRole(TeamRole.MEMBER.getValue());

        Team currentTeam = new Team();
        currentTeam.setId(2);
        currentTeam.setLeaderId(2); // 队长是别人

        when(teamMemberMapper.selectOne(any())).thenReturn(member);
        when(teamMapper.selectById(2)).thenReturn(currentTeam);
        when(teamMemberMapper.deleteById(1)).thenReturn(1);
        when(teamMapper.insert(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(3);
            return 1;
        });
        when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

        // Act
        teamService.leaveTeam(1, "testuser");

        // Assert
        verify(teamMemberMapper).deleteById(1);
        verify(teamMapper).insert(any(Team.class));
        verify(teamMemberMapper).insert(any(TeamMember.class));
    }

    @Test
    void leaveTeam_CannotLeavePersonalTeam() {
        // Arrange
        TeamMember member = new TeamMember();
        member.setId(1);
        member.setTeamId(1);
        member.setUserId(1);
        member.setRole(TeamRole.LEADER.getValue());

        when(teamMemberMapper.selectOne(any())).thenReturn(member);
        when(teamMapper.selectById(1)).thenReturn(testTeam); // 队长是自己

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            teamService.leaveTeam(1, "testuser");
        });
        assertEquals(ResultCode.CANNOT_LEAVE_PERSONAL_TEAM, exception.getResultCode());
    }

    @Test
    void kickMember_Success() {
        // Arrange
        TeamMember targetMember = new TeamMember();
        targetMember.setId(2);
        targetMember.setTeamId(1);
        targetMember.setUserId(2);
        targetMember.setRole(TeamRole.MEMBER.getValue());

        TeamMember leaderMember = new TeamMember();
        leaderMember.setId(1);
        leaderMember.setTeamId(1);
        leaderMember.setUserId(1);
        leaderMember.setRole(TeamRole.LEADER.getValue());

        User targetUser = new User();
        targetUser.setId(2);
        targetUser.setUsername("targetuser");

        // 使用 thenAnswer 来根据调用次数返回不同的结果
        final int[] callCount = {0};
        when(teamMemberMapper.selectOne(any())).thenAnswer(invocation -> {
            callCount[0]++;
            // 第一次调用（查找目标成员）返回 targetMember
            // 第二次调用（查找队长）返回 leaderMember
            return callCount[0] == 1 ? targetMember : leaderMember;
        });
        when(teamMemberMapper.deleteById(2)).thenReturn(1);
        when(userMapper.selectById(2)).thenReturn(targetUser);
        when(teamMapper.insert(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(3);
            return 1;
        });
        when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);

        // Act
        teamService.kickMember(1, 2, "leader");

        // Assert
        verify(teamMemberMapper).deleteById(2);
        verify(teamMapper).insert(any(Team.class));
    }

    @Test
    void kickMember_CannotKickSelf() {
        // Arrange
        TeamMember leaderMember = new TeamMember();
        leaderMember.setId(1);
        leaderMember.setTeamId(1);
        leaderMember.setUserId(1);
        leaderMember.setRole(TeamRole.LEADER.getValue());

        // 两次调用都返回队长（表示要踢出的是队长自己）
        when(teamMemberMapper.selectOne(any())).thenReturn(leaderMember);

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            teamService.kickMember(1, 1, "leader");
        });
        assertEquals(ResultCode.CANNOT_KICK_SELF, exception.getResultCode());
    }

    @Test
    void getCurrentTeamInfo_Success() {
        // Arrange
        when(teamMemberMapper.selectOne(any())).thenReturn(testTeamMember);
        when(teamMapper.selectById(1)).thenReturn(testTeam);
        when(userMapper.selectById(1)).thenReturn(testUser);

        // Act
        TeamMemberVO result = teamService.getCurrentTeamInfo(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTeamId());
        assertEquals("测试团队", result.getTeamName());
        assertEquals(TeamRole.LEADER.getValue(), result.getRole());
        assertEquals("testuser", result.getUsername());
    }
}
