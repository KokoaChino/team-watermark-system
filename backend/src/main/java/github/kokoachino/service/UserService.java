package github.kokoachino.service;

import com.baomidou.mybatisplus.extension.service.IService;
import github.kokoachino.model.dto.LoginDTO;
import github.kokoachino.model.entity.User;
import github.kokoachino.model.vo.UserVO;
import github.kokoachino.model.dto.RegisterDTO;
import github.kokoachino.model.dto.ForgotPasswordDTO;
import github.kokoachino.model.dto.SendCodeDTO;
import github.kokoachino.model.vo.CaptchaVO;


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
}