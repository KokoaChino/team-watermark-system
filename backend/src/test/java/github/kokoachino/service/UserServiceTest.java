package github.kokoachino.service;

import cn.hutool.crypto.digest.BCrypt;
import github.kokoachino.common.enums.LoginType;
import github.kokoachino.common.enums.VerificationCodeType;
import github.kokoachino.common.exception.BizException;
import github.kokoachino.common.util.CaptchaUtils;
import github.kokoachino.common.util.JwtUtils;
import github.kokoachino.common.util.MailUtils;
import github.kokoachino.common.util.RedisUtils;
import github.kokoachino.common.util.RedisKeyUtils;
import github.kokoachino.config.SystemProperties;
import github.kokoachino.mapper.BlackListMapper;
import github.kokoachino.mapper.TeamMemberMapper;
import github.kokoachino.mapper.UserMapper;
import github.kokoachino.model.dto.ForgotPasswordDTO;
import github.kokoachino.model.dto.LoginDTO;
import github.kokoachino.model.dto.RegisterDTO;
import github.kokoachino.model.entity.User;
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

        when(systemProperties.getInitialPoints()).thenReturn(10000);
        when(systemProperties.getEmailCodeExpiration()).thenReturn(5);
        when(systemProperties.getCaptchaExpiration()).thenReturn(2);
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
}
