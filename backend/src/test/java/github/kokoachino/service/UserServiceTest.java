package github.kokoachino.service;

import cn.hutool.crypto.digest.BCrypt;
import github.kokoachino.common.enums.LoginType;
import github.kokoachino.common.enums.VerificationCodeType;
import github.kokoachino.common.exception.BizException;
import github.kokoachino.common.util.CaptchaUtils;
import github.kokoachino.common.util.JwtUtils;
import github.kokoachino.common.util.MailUtils;
import github.kokoachino.common.util.RedisUtils;
import github.kokoachino.mapper.BlackListMapper;
import github.kokoachino.mapper.TeamMemberMapper;
import github.kokoachino.mapper.UserMapper;
import github.kokoachino.model.dto.ForgotPasswordDTO;
import github.kokoachino.model.dto.LoginDTO;
import github.kokoachino.model.dto.RegisterDTO;
import github.kokoachino.model.dto.SendCodeDTO;
import github.kokoachino.model.entity.User;
import github.kokoachino.model.vo.CaptchaVO;
import github.kokoachino.model.vo.UserVO;
import github.kokoachino.service.TeamService;
import github.kokoachino.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Manually set the baseMapper field in ServiceImpl
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(BCrypt.hashpw("password123"));
    }

    @Test
    void login_PasswordSuccess() {
        // Arrange
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setAccount("testuser");
        loginDTO.setPassword("password123");
        loginDTO.setLoginType("password");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);
        when(jwtUtils.generateToken("testuser")).thenReturn("mock-token");

        // Act
        UserVO result = userService.login(loginDTO);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("mock-token", result.getToken());
        verify(userMapper).selectOne(any(), anyBoolean());
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

        when(redisUtils.get("captcha：captcha-key")).thenReturn("1234");
        when(redisUtils.get("email_code：register：new@example.com")).thenReturn("654321");
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

        when(redisUtils.get("email_code：forgot_password：test@example.com")).thenReturn("111222");
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);

        // Act
        userService.forgotPassword(forgotPasswordDTO);

        // Assert
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void logout_Success() {
        // Act
        userService.logout("test-token");

        // Assert
        verify(blackListMapper).insert(any(github.kokoachino.model.entity.BlackList.class));
    }
}
