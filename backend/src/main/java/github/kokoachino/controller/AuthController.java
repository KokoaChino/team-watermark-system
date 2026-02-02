package github.kokoachino.controller;

import github.kokoachino.common.result.Result;
import github.kokoachino.model.dto.ForgotPasswordDTO;
import github.kokoachino.model.dto.LoginDTO;
import github.kokoachino.model.dto.RegisterDTO;
import github.kokoachino.model.dto.SendCodeDTO;
import github.kokoachino.model.vo.CaptchaVO;
import github.kokoachino.model.vo.UserVO;
import github.kokoachino.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * 认证控制层
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<UserVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        UserVO userVO = userService.login(loginDTO);
        return Result.success(userVO, "登录成功");
    }

    @PostMapping("/register")
    public Result<Object> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success(null, "注册成功");
    }

    @PostMapping("/forgot-password")
    public Result<Object> forgotPassword(@Valid @RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        userService.forgotPassword(forgotPasswordDTO);
        return Result.success(null, "密码重置成功");
    }

    @PostMapping("/send-code")
    public Result<Object> sendCode(@Valid @RequestBody SendCodeDTO sendCodeDTO) {
        userService.sendVerificationCode(sendCodeDTO);
        return Result.success(null, "验证码已发送");
    }

    @GetMapping("/captcha")
    public Result<CaptchaVO> getCaptcha() {
        CaptchaVO captchaVO = userService.getCaptcha();
        return Result.success(captchaVO);
    }

    @PostMapping("/logout")
    public Result<Object> logout() {
        userService.logout();
        return Result.success(null, "已退出登录");
    }

    @DeleteMapping("/unregister")
    public Result<Object> unregister() {
        userService.unregister();
        return Result.success(null, "账户已注销");
    }
}
