package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.model.dto.ForgotPasswordDTO;
import com.github.kokoachino.model.dto.LoginDTO;
import com.github.kokoachino.model.dto.RegisterDTO;
import com.github.kokoachino.model.dto.SendCodeDTO;
import com.github.kokoachino.model.vo.CaptchaVO;
import com.github.kokoachino.model.vo.UserVO;
import com.github.kokoachino.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持密码登录和邮箱验证码登录")
    public Result<UserVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        UserVO userVO = userService.login(loginDTO);
        return Result.success(userVO, "登录成功");
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "通过邮箱注册新用户")
    public Result<Object> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success(null, "注册成功");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "忘记密码", description = "通过邮箱验证码重置密码")
    public Result<Object> forgotPassword(@Valid @RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        userService.forgotPassword(forgotPasswordDTO);
        return Result.success(null, "密码重置成功");
    }

    @PostMapping("/send-code")
    @Operation(summary = "发送验证码", description = "发送邮箱验证码（注册、登录、找回密码、修改邮箱）")
    public Result<Object> sendCode(@Valid @RequestBody SendCodeDTO sendCodeDTO) {
        userService.sendVerificationCode(sendCodeDTO);
        return Result.success(null, "验证码已发送");
    }

    @GetMapping("/captcha")
    @Operation(summary = "获取图形验证码", description = "生成一个新的图形验证码")
    public Result<CaptchaVO> getCaptcha() {
        CaptchaVO captchaVO = userService.getCaptcha();
        return Result.success(captchaVO);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户退出", description = "退出登录，使当前 Token 失效")
    public Result<Object> logout() {
        userService.logout();
        return Result.success(null, "已退出登录");
    }

    @DeleteMapping("/unregister")
    @Operation(summary = "注销账户", description = "永久删除用户账户和相关数据")
    public Result<Object> unregister() {
        userService.unregister();
        return Result.success(null, "账户已注销");
    }
}
