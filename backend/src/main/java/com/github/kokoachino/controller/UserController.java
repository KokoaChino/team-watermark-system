package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.model.dto.UpdateProfileDTO;
import com.github.kokoachino.model.vo.UserVO;
import com.github.kokoachino.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * 用户控制层
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息相关接口")
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/profile")
    @Operation(summary = "获取个人信息", description = "获取当前登录用户的详细信息")
    public Result<UserVO> getProfile() {
        UserVO userVO = userService.getUserVOById(UserContext.getUserId());
        return Result.success(userVO);
    }

    /**
     * 修改个人信息
     *
     * @param dto 修改信息DTO
     * @param request HTTP请求对象
     * @return 更新后的用户信息
     */
    @PutMapping("/profile")
    @Operation(summary = "修改个人信息", description = "修改当前登录用户的个人信息（用户名、密码、邮箱），修改密码或邮箱后会自动退出登录")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto, HttpServletRequest request) {
        UserVO userVO = userService.updateProfile(dto, request);
        return Result.success(userVO, "个人信息修改成功");
    }
}
