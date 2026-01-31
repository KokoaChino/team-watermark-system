package github.kokoachino.service;

import cn.hutool.crypto.digest.BCrypt;
import github.kokoachino.common.exception.BizException;
import github.kokoachino.common.util.JwtUtils;
import github.kokoachino.mapper.UserMapper;
import github.kokoachino.model.dto.LoginDTO;
import github.kokoachino.model.entity.User;
import github.kokoachino.model.vo.UserVO;
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
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

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
    void login_PasswordWrong() {
        // Arrange
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setAccount("testuser");
        loginDTO.setPassword("wrongpassword");
        loginDTO.setLoginType("password");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);

        // Act & Assert
        assertThrows(BizException.class, () -> userService.login(loginDTO));
    }

    @Test
    void login_CaptchaSuccess() {
        // Arrange
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setAccount("test@example.com");
        loginDTO.setCaptcha("123456");
        loginDTO.setLoginType("captcha");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha：test@example.com")).thenReturn("123456");
        when(jwtUtils.generateToken("testuser")).thenReturn("mock-token");

        // Act
        UserVO result = userService.login(loginDTO);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(redisTemplate).delete("captcha：test@example.com");
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setAccount("nonexistent");
        loginDTO.setLoginType("password");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        // Act & Assert
        assertThrows(BizException.class, () -> userService.login(loginDTO));
    }
}
