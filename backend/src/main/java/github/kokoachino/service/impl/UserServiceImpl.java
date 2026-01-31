package github.kokoachino.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import github.kokoachino.common.enums.LoginType;
import github.kokoachino.common.enums.VerificationCodeType;
import github.kokoachino.common.exception.BizException;
import github.kokoachino.common.result.ResultCode;
import github.kokoachino.common.util.CaptchaUtils;
import github.kokoachino.common.util.JwtUtils;
import github.kokoachino.common.util.MailUtils;
import github.kokoachino.common.util.RedisUtils;
import github.kokoachino.mapper.BlackListMapper;
import github.kokoachino.mapper.TeamMemberMapper;
import github.kokoachino.mapper.UserMapper;
import github.kokoachino.model.dto.ForgotPasswordDTO;
import github.kokoachino.model.dto.LoginDTO;
import github.kokoachino.model.dto.RegisterDTO;
import github.kokoachino.model.dto.SendCodeDTO;
import github.kokoachino.model.entity.BlackList;
import github.kokoachino.model.entity.Team;
import github.kokoachino.model.entity.TeamMember;
import github.kokoachino.model.entity.User;
import github.kokoachino.model.vo.CaptchaVO;
import github.kokoachino.model.vo.UserVO;
import github.kokoachino.service.TeamService;
import github.kokoachino.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * 用户服务实现类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final TeamService teamService;
    private final TeamMemberMapper teamMemberMapper;
    private final BlackListMapper blackListMapper;
    private final MailUtils mailUtils;
    private final CaptchaUtils captchaUtils;

    private static final String CAPTCHA_PREFIX = "captcha：";
    private static final String EMAIL_CODE_PREFIX = "email_code：";

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

        LoginType loginType = LoginType.fromValue(loginDTO.getLoginType());
        if (loginType == LoginType.PASSWORD) {
            // 密码登录
            if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
                throw new BizException(ResultCode.PASSWORD_ERROR);
            }
        } else if (loginType == LoginType.CAPTCHA) {
            // 验证码登录
            String captcha = loginDTO.getCaptcha();
            String redisKey = EMAIL_CODE_PREFIX + VerificationCodeType.LOGIN.getValue() + "：" + user.getEmail();
            String cachedCaptcha = redisUtils.get(redisKey);
            if (cachedCaptcha == null || !cachedCaptcha.equals(captcha)) {
                throw new BizException(ResultCode.CAPTCHA_ERROR);
            }
            // 验证通过后删除验证码
            redisUtils.delete(redisKey);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        // 1. 校验人机验证码
        String captchaKey = registerDTO.getCaptchaKey();
        String cachedCaptcha = redisUtils.get(CAPTCHA_PREFIX + captchaKey);
        if (cachedCaptcha == null || !cachedCaptcha.equalsIgnoreCase(registerDTO.getCaptcha())) {
            throw new BizException(ResultCode.CAPTCHA_ERROR);
        }
        redisUtils.delete(CAPTCHA_PREFIX + captchaKey);

        // 2. 校验邮箱验证码
        String emailCodeKey = EMAIL_CODE_PREFIX + VerificationCodeType.REGISTER.getValue() + "：" + registerDTO.getEmail();
        String cachedEmailCode = redisUtils.get(emailCodeKey);
        if (cachedEmailCode == null || !cachedEmailCode.equals(registerDTO.getEmailCode())) {
            throw new BizException("邮箱验证码错误");
        }
        redisUtils.delete(emailCodeKey);

        // 3. 校验用户是否存在
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername())
                .or()
                .eq(User::getEmail, registerDTO.getEmail()));
        if (count > 0) {
            throw new BizException("用户名或邮箱已存在");
        }

        // 4. 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        this.save(user);

        // 5. 检查黑名单以确定初始点数
        long isBlacklisted = blackListMapper.selectCount(new LambdaQueryWrapper<BlackList>()
                .eq(BlackList::getType, "email")
                .eq(BlackList::getValue, registerDTO.getEmail()));
        int initialPoints = isBlacklisted > 0 ? 0 : 10000;

        // 6. 创建个人团队
        teamService.createPersonalTeam(user.getId(), user.getUsername(), initialPoints);
    }

    @Override
    public void forgotPassword(ForgotPasswordDTO forgotPasswordDTO) {
        // 1. 校验邮箱验证码
        String emailCodeKey = EMAIL_CODE_PREFIX + VerificationCodeType.FORGOT_PASSWORD.getValue() + "：" + forgotPasswordDTO.getEmail();
        String cachedEmailCode = redisUtils.get(emailCodeKey);
        if (cachedEmailCode == null || !cachedEmailCode.equals(forgotPasswordDTO.getCode())) {
            throw new BizException("邮箱验证码错误");
        }
        redisUtils.delete(emailCodeKey);

        // 2. 更新密码
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, forgotPasswordDTO.getEmail()));
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(BCrypt.hashpw(forgotPasswordDTO.getNewPassword()));
        this.updateById(user);
    }

    @Override
    public void sendVerificationCode(SendCodeDTO sendCodeDTO) {
        String code = RandomUtil.randomNumbers(6);
        String type = sendCodeDTO.getType();
        String redisKey = EMAIL_CODE_PREFIX + type + "：" + sendCodeDTO.getEmail();
        
        // 存入 Redis，有效期 5 分钟
        redisUtils.set(redisKey, code, 5, TimeUnit.MINUTES);
        
        // 发送邮件
        String subject = "验证码";
        String content = "您的验证码为：" + code + "，有效期5分钟。";
        mailUtils.sendSimpleMail(sendCodeDTO.getEmail(), subject, content);
    }

    @Override
    public CaptchaVO getCaptcha() {
        CaptchaVO captchaVO = captchaUtils.createCaptcha();
        String key = UUID.randomUUID().toString();
        captchaVO.setKey(key);
        // 存入 Redis，有效期 2 分钟
        redisUtils.set(CAPTCHA_PREFIX + key, captchaVO.getCode(), 2, TimeUnit.MINUTES);
        return captchaVO;
    }

    @Override
    public void logout(String token) {
        // 将 Token 加入黑名单
        BlackList blackList = new BlackList();
        blackList.setType("token");
        blackList.setValue(token);
        blackListMapper.insert(blackList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unregister(Integer userId) {
        User user = this.getById(userId);
        if (user == null) return;

        // 1. 获取用户所属的所有团队及角色
        List<TeamMember> memberships = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getUserId, userId));

        for (TeamMember member : memberships) {
            if ("leader".equals(member.getRole())) {
                // 队长逻辑
                List<TeamMember> others = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, member.getTeamId())
                        .ne(TeamMember::getUserId, userId)
                        .orderByAsc(TeamMember::getCreatedAt));
                
                if (others.isEmpty()) {
                    // 仅剩自己，解散团队
                    teamService.removeById(member.getTeamId());
                } else {
                    // 转移队长身份给最早加入的成员
                    TeamMember nextLeader = others.get(0);
                    nextLeader.setRole("leader");
                    teamMemberMapper.updateById(nextLeader);
                    
                    Team team = teamService.getById(member.getTeamId());
                    team.setLeaderId(nextLeader.getUserId());
                    teamService.updateById(team);
                }
            }
            // 删除成员记录
            teamMemberMapper.deleteById(member.getId());
        }

        // 2. 删除用户
        this.removeById(userId);
        
        // 3. 邮箱加入黑名单防止刷点数
        BlackList blackList = new BlackList();
        blackList.setType("email");
        blackList.setValue(user.getEmail());
        blackListMapper.insert(blackList);
    }
}
