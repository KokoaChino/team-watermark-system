package com.github.kokoachino.service;

import cn.hutool.crypto.digest.BCrypt;
import com.github.kokoachino.common.enums.VerificationCodeType;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.*;
import com.github.kokoachino.config.SystemProperties;
import com.github.kokoachino.mapper.BlackListMapper;
import com.github.kokoachino.mapper.TeamMemberMapper;
import com.github.kokoachino.mapper.UserMapper;
import com.github.kokoachino.model.dto.ForgotPasswordDTO;
import com.github.kokoachino.model.dto.LoginDTO;
import com.github.kokoachino.model.dto.RegisterDTO;
import com.github.kokoachino.model.dto.UpdateProfileDTO;
import com.github.kokoachino.model.entity.BlackList;
import com.github.kokoachino.model.entity.TeamMember;
import com.github.kokoachino.model.entity.User;
import com.github.kokoachino.model.vo.UserVO;
import com.github.kokoachino.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务测试类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private TeamService teamService;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private BlackListMapper blackListMapper;

    @Mock
    private MailUtils mailUtils;

    @Mock
    private CaptchaUtils captchaUtils;

    @Mock
    private AmqpTemplate amqpTemplate;

    @Mock
    private SystemProperties systemProperties;

    @Mock
    private LockUtils lockUtils;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserVO testUserVO;

    @BeforeEach
    void setUp() {
        // Manually set the baseMapper field in ServiceImpl
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(BCrypt.hashpw("password123"));

        testUserVO = UserVO.builder()
                .id(1)
                .username("testuser")
                .email("test@example.com")
                .teamId(1)
                .teamRole("leader")
                .build();

        when(systemProperties.getInitialPoints()).thenReturn(10000);
        when(systemProperties.getEmailCode()).thenReturn(new SystemProperties.EmailCodeConfig());
        when(systemProperties.getCaptcha()).thenReturn(new SystemProperties.CaptchaConfig());

        // 设置用户上下文
        UserContext.setUser(testUserVO);
    }

    @Test
    void login_PasswordSuccess() {
        // Arrange
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setAccount("testuser");
        loginDTO.setPassword("password123");
        loginDTO.setLoginType("password");

        // Use lenient to avoid unnecessary stubbing errors and try to match any possible call
        lenient().when(userMapper.selectOne(any())).thenReturn(testUser);
        lenient().when(userMapper.selectList(any())).thenReturn(java.util.Collections.singletonList(testUser));
        
        lenient().when(jwtUtils.generateToken(anyInt())).thenReturn("mock-token");
        lenient().when(userMapper.selectById(anyInt())).thenReturn(testUser);
        
        lenient().when(teamMemberMapper.selectOne(any())).thenReturn(null);

        // Act
        UserVO result = userService.login(loginDTO);

        // Assert
        assertNotNull(result, "Login result should not be null");
        assertEquals("testuser", result.getUsername());
        assertEquals("mock-token", result.getToken());
    }

    @Test
    void register_Success() {
        // Arrange
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("password123");
        registerDTO.setEmail("new@example.com");
        registerDTO.setCaptcha("1234");
        registerDTO.setCaptchaKey("captcha-key");
        registerDTO.setEmailCode("654321");

        when(redisUtils.get(RedisKeyUtils.getCaptchaKey("captcha-key"))).thenReturn("1234");
        when(redisUtils.get(RedisKeyUtils.getEmailCodeKey("register", "new@example.com"))).thenReturn("654321");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(blackListMapper.selectCount(any())).thenReturn(0L);

        // Act
        userService.register(registerDTO);

        // Assert
        verify(userMapper).insert(any(User.class));
        verify(teamService).createPersonalTeam(any(), eq("newuser"), eq(10000));
    }

    @Test
    void forgotPassword_Success() {
        // Arrange
        ForgotPasswordDTO forgotPasswordDTO = new ForgotPasswordDTO();
        forgotPasswordDTO.setEmail("test@example.com");
        forgotPasswordDTO.setCode("111222");
        forgotPasswordDTO.setNewPassword("newpassword");

        when(redisUtils.get(RedisKeyUtils.getEmailCodeKey("forgot_password", "test@example.com"))).thenReturn("111222");
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);

        // Act
        userService.forgotPassword(forgotPasswordDTO);

        // Assert
        verify(userMapper).updateById(any(User.class));
    }

    // ==================== 修改个人信息测试 ====================

    @Test
    void updateProfile_UsernameOnly_Success() {
        // Arrange
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setUsername("newUsername");

        when(userMapper.selectById(1)).thenReturn(testUser);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        TeamMember member = new TeamMember();
        member.setTeamId(1);
        member.setRole("leader");
        when(teamMemberMapper.selectOne(any())).thenReturn(member);

        // Act
        UserVO result = userService.updateProfile(dto, httpServletRequest);

        // Assert
        assertNotNull(result);
        verify(userMapper).updateById(any(User.class));
        verify(redisUtils).delete(RedisKeyUtils.getUserKey(1));
    }

    @Test
    void updateProfile_PasswordChange_Success() {
        // Arrange
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setOriginalPassword("password123");
        dto.setNewPassword("newPassword123");

        when(userMapper.selectById(1)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer mock-token");

        TeamMember member = new TeamMember();
        member.setTeamId(1);
        member.setRole("leader");
        when(teamMemberMapper.selectOne(any())).thenReturn(member);

        // Act
        UserVO result = userService.updateProfile(dto, httpServletRequest);

        // Assert
        assertNotNull(result);
        verify(userMapper).updateById(any(User.class));
        verify(redisUtils).delete(RedisKeyUtils.getUserKey(1));
        verify(blackListMapper).insert(any(BlackList.class)); // Token 加入黑名单
    }

    @Test
    void updateProfile_PasswordChange_InvalidOriginalPassword() {
        // Arrange
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setOriginalPassword("wrongPassword");
        dto.setNewPassword("newPassword123");

        when(userMapper.selectById(1)).thenReturn(testUser);

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            userService.updateProfile(dto, httpServletRequest);
        });
        assertEquals(ResultCode.INVALID_PASSWORD, exception.getResultCode());
    }

    @Test
    void updateProfile_EmailChange_Success() {
        // Arrange
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNewEmail("newemail@example.com");
        dto.setEmailCode("123456");

        when(userMapper.selectById(1)).thenReturn(testUser);
        when(redisUtils.get(RedisKeyUtils.getEmailCodeKey(VerificationCodeType.UPDATE_EMAIL.getValue(), "newemail@example.com")))
                .thenReturn("123456");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer mock-token");

        TeamMember member = new TeamMember();
        member.setTeamId(1);
        member.setRole("leader");
        when(teamMemberMapper.selectOne(any())).thenReturn(member);

        // Act
        UserVO result = userService.updateProfile(dto, httpServletRequest);

        // Assert
        assertNotNull(result);
        verify(userMapper).updateById(any(User.class));
        verify(redisUtils).delete(RedisKeyUtils.getUserKey(1));
        verify(blackListMapper).insert(any(BlackList.class)); // Token 加入黑名单
    }

    @Test
    void updateProfile_EmailChange_InvalidCode() {
        // Arrange
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNewEmail("newemail@example.com");
        dto.setEmailCode("wrongcode");

        when(userMapper.selectById(1)).thenReturn(testUser);
        when(redisUtils.get(RedisKeyUtils.getEmailCodeKey(VerificationCodeType.UPDATE_EMAIL.getValue(), "newemail@example.com")))
                .thenReturn("123456"); // 正确的验证码

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            userService.updateProfile(dto, httpServletRequest);
        });
        assertEquals(ResultCode.EMAIL_CODE_ERROR, exception.getResultCode());
    }

    @Test
    void updateProfile_EmailChange_EmailAlreadyBound() {
        // Arrange
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNewEmail("existing@example.com");
        dto.setEmailCode("123456");

        when(userMapper.selectById(1)).thenReturn(testUser);
        when(redisUtils.get(RedisKeyUtils.getEmailCodeKey(VerificationCodeType.UPDATE_EMAIL.getValue(), "existing@example.com")))
                .thenReturn("123456");
        when(userMapper.selectCount(any())).thenReturn(1L); // 邮箱已被其他用户使用

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            userService.updateProfile(dto, httpServletRequest);
        });
        assertEquals(ResultCode.EMAIL_ALREADY_BOUND, exception.getResultCode());
    }

    @Test
    void updateProfile_UsernameAlreadyExists() {
        // Arrange
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setUsername("existinguser");

        when(userMapper.selectById(1)).thenReturn(testUser);
        when(userMapper.selectCount(any())).thenReturn(1L); // 用户名已被使用

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            userService.updateProfile(dto, httpServletRequest);
        });
        assertEquals(ResultCode.USER_EXIST, exception.getResultCode());
    }

    @Test
    void updateProfile_CombinedChanges_Success() {
        // Arrange - 同时修改用户名和密码
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setUsername("newUsername");
        dto.setOriginalPassword("password123");
        dto.setNewPassword("newPassword123");

        when(userMapper.selectById(1)).thenReturn(testUser);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer mock-token");

        TeamMember member = new TeamMember();
        member.setTeamId(1);
        member.setRole("leader");
        when(teamMemberMapper.selectOne(any())).thenReturn(member);

        // Act
        UserVO result = userService.updateProfile(dto, httpServletRequest);

        // Assert
        assertNotNull(result);
        verify(userMapper).updateById(any(User.class));
        verify(redisUtils).delete(RedisKeyUtils.getUserKey(1));
        verify(blackListMapper).insert(any(BlackList.class));
    }

    @Test
    void updateProfile_UserNotFound() {
        // Arrange
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setUsername("newUsername");

        when(userMapper.selectById(1)).thenReturn(null);

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> {
            userService.updateProfile(dto, httpServletRequest);
        });
        assertEquals(ResultCode.USER_NOT_FOUND, exception.getResultCode());
    }
}
