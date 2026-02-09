package com.github.kokoachino.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.kokoachino.model.dto.*;
import com.github.kokoachino.model.entity.User;
import com.github.kokoachino.model.vo.CaptchaVO;
import com.github.kokoachino.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;


/**
 * 用户服务接口
 *
 * @author kokoachino
 * @date 2026-01-31
 */
public interface UserService extends IService<User> {

    UserVO login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    void forgotPassword(ForgotPasswordDTO forgotPasswordDTO);

    void sendVerificationCode(SendCodeDTO sendCodeDTO);

    CaptchaVO getCaptcha();

    void logout();

    void unregister();

    UserVO getUserVOById(Integer userId);

    /**
     * 修改个人信息
     *
     * @param dto 修改信息DTO
     * @param request HTTP请求对象，用于获取当前Token
     * @return 更新后的用户信息
     */
    UserVO updateProfile(UpdateProfileDTO dto, HttpServletRequest request);
}