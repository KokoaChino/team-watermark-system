package github.kokoachino.controller;

import github.kokoachino.common.result.Result;
import github.kokoachino.model.dto.LoginDTO;
import github.kokoachino.model.vo.UserVO;
import github.kokoachino.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author kokoachino
 * @date 2026-01-31
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<UserVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        UserVO userVO = userService.login(loginDTO);
        return Result.success(userVO, "登录成功");
    }
}
