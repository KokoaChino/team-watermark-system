package github.kokoachino.service;

import com.baomidou.mybatisplus.extension.service.IService;
import github.kokoachino.model.dto.LoginDTO;
import github.kokoachino.model.entity.User;
import github.kokoachino.model.vo.UserVO;


/**
 * @author kokoachino
 * @date 2026-01-31
 */
public interface UserService extends IService<User> {
    UserVO login(LoginDTO loginDTO);
}