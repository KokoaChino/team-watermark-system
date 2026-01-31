package github.kokoachino.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import github.kokoachino.common.exception.BizException;
import github.kokoachino.common.result.ResultCode;
import github.kokoachino.common.util.JwtUtils;
import github.kokoachino.mapper.UserMapper;
import github.kokoachino.model.dto.LoginDTO;
import github.kokoachino.model.entity.User;
import github.kokoachino.model.vo.UserVO;
import github.kokoachino.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


/**
 * @author kokoachino
 * @date 2026-01-31
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    @Override
    public UserVO login(LoginDTO loginDTO) {
        String account = loginDTO.getAccount();
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, account)
                .or()
                .eq(User::getEmail, account));

        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }

        if ("password".equals(loginDTO.getLoginType())) {
            // 密码登录
            if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
                throw new BizException(ResultCode.PASSWORD_ERROR);
            }
        } else if ("captcha".equals(loginDTO.getLoginType())) {
            // 验证码登录
            String captcha = loginDTO.getCaptcha();
            String cachedCaptcha = redisTemplate.opsForValue().get("captcha：" + user.getEmail());
            if (cachedCaptcha == null || !cachedCaptcha.equals(captcha)) {
                throw new BizException(ResultCode.CAPTCHA_ERROR);
            }
            // 验证通过后删除验证码
            redisTemplate.delete("captcha：" + user.getEmail());
        } else {
            throw new BizException("不支持的登录方式");
        }

        // 生成 Token
        String token = jwtUtils.generateToken(user.getUsername());

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .build();
    }
}
