package com.github.kokoachino.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.kokoachino.common.enums.BlackListTypeEnum;
import com.github.kokoachino.common.enums.LoginTypeEnum;
import com.github.kokoachino.common.enums.TeamRoleEnum;
import com.github.kokoachino.common.enums.VerificationCodeTypeEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.*;
import com.github.kokoachino.config.SystemProperties;
import com.github.kokoachino.model.dto.*;
import com.github.kokoachino.mapper.BlackListMapper;
import com.github.kokoachino.mapper.TeamMemberMapper;
import com.github.kokoachino.mapper.UserMapper;
import com.github.kokoachino.model.entity.BlackList;
import com.github.kokoachino.model.entity.Team;
import com.github.kokoachino.model.entity.TeamMember;
import com.github.kokoachino.model.entity.User;
import com.github.kokoachino.model.vo.CaptchaVO;
import com.github.kokoachino.model.vo.UserVO;
import com.github.kokoachino.service.TeamService;
import com.github.kokoachino.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.List;
import java.util.Objects;
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
    private final CaptchaUtils captchaUtils;
    private final LockUtils lockUtils;
    private final AsyncTaskUtils asyncTaskUtils;
    private final MailUtils mailUtils;
    private final SystemProperties systemProperties;

    @Override
    public UserVO login(LoginDTO loginDTO) {
        String account = loginDTO.getAccount();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, account)
                .or()
                .eq(User::getEmail, account));
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        LoginTypeEnum loginTypeEnum = LoginTypeEnum.fromValue(loginDTO.getLoginType());
        if (loginTypeEnum == null) {
            throw new BizException(ResultCode.UNSUPPORTED_LOGIN_TYPE);
        }
        if (loginTypeEnum == LoginTypeEnum.PASSWORD) {
            if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
                throw new BizException(ResultCode.PASSWORD_ERROR);
            }
        } else if (loginTypeEnum == LoginTypeEnum.CAPTCHA) {
            String emailCode = loginDTO.getEmailCode();
            String redisKey = RedisKeyUtils.getEmailCodeKey(VerificationCodeTypeEnum.LOGIN.getValue(), user.getEmail());
            String cachedCode = redisUtils.get(redisKey);
            if (cachedCode == null || !cachedCode.equals(emailCode)) {
                throw new BizException(ResultCode.EMAIL_CODE_ERROR);
            }
            redisUtils.delete(redisKey);
        }
        String token = jwtUtils.generateToken(user.getId());
        UserVO userVO = getUserVOById(user.getId());
        userVO.setToken(token);
        return userVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        String captchaKey = registerDTO.getCaptchaKey();
        String redisCaptchaKey = RedisKeyUtils.getCaptchaKey(captchaKey);
        String cachedCaptcha = redisUtils.get(redisCaptchaKey);
        if (cachedCaptcha == null || !cachedCaptcha.equalsIgnoreCase(registerDTO.getCaptcha())) {
            throw new BizException(ResultCode.CAPTCHA_ERROR);
        }
        redisUtils.delete(redisCaptchaKey);
        String emailCodeKey = RedisKeyUtils.getEmailCodeKey(VerificationCodeTypeEnum.REGISTER.getValue(), registerDTO.getEmail());
        String cachedEmailCode = redisUtils.get(emailCodeKey);
        if (cachedEmailCode == null || !cachedEmailCode.equals(registerDTO.getEmailCode())) {
            throw new BizException(ResultCode.EMAIL_CODE_ERROR);
        }
        redisUtils.delete(emailCodeKey);
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername())
                .or()
                .eq(User::getEmail, registerDTO.getEmail()));
        if (count > 0) {
            throw new BizException(ResultCode.USER_EXIST);
        }
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        this.save(user);
        long isBlacklisted = blackListMapper.selectCount(new LambdaQueryWrapper<BlackList>()
                .eq(BlackList::getType, BlackListTypeEnum.EMAIL.getValue())
                .eq(BlackList::getValue, registerDTO.getEmail()));
        int initialPoints = isBlacklisted > 0 ? 0 : systemProperties.getInitialPoints();
        teamService.createPersonalTeam(user.getId(), user.getUsername(), initialPoints);
    }

    @Override
    public void forgotPassword(ForgotPasswordDTO forgotPasswordDTO) {
        String emailCodeKey = RedisKeyUtils.getEmailCodeKey(VerificationCodeTypeEnum.FORGOT_PASSWORD.getValue(), forgotPasswordDTO.getEmail());
        String cachedEmailCode = redisUtils.get(emailCodeKey);
        if (cachedEmailCode == null || !cachedEmailCode.equals(forgotPasswordDTO.getCode())) {
            throw new BizException(ResultCode.EMAIL_CODE_ERROR);
        }
        redisUtils.delete(emailCodeKey);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, forgotPasswordDTO.getEmail()));
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(BCrypt.hashpw(forgotPasswordDTO.getNewPassword()));
        this.updateById(user);
    }

    @Override
    public void sendVerificationCode(SendCodeDTO sendCodeDTO) {
        String code = RandomUtil.randomNumbers(6);
        String typeValue = sendCodeDTO.getType();
        VerificationCodeTypeEnum type = VerificationCodeTypeEnum.fromValue(typeValue);
        if (type == null) {
            throw new BizException(ResultCode.VALIDATE_FAILED);
        }
        String redisKey = RedisKeyUtils.getEmailCodeKey(typeValue, sendCodeDTO.getEmail());
        if (redisUtils.hasKey(redisKey)) {
            Long expire = redisUtils.getExpire(redisKey);
            if (expire != null && expire > (systemProperties.getEmailCode().getExpiration() * 60 - 60)) {
                throw new BizException(ResultCode.REQUEST_TOO_FREQUENT);
            }
        }
        redisUtils.set(redisKey, code, systemProperties.getEmailCode().getExpiration(), TimeUnit.MINUTES);
        String subject = "【协作式批量图片水印处理系统】验证码（请勿泄露）";
        String htmlContent = """
            <html>
            <body style='font-family: Microsoft YaHei, sans-serif;'>
                <p>尊敬的用户：</p>
                <p>您好！</p>
                <p>您正在进行【%s】操作，本次验证码为：<br>
                <strong style='color: #1890ff; font-size: 18px;'>%s</strong></p>
            
                <div style='color: #666; margin-top: 20px;'>
                    <h4>【温馨提示】</h4>
                    <ol>
                        <li>本验证码%d分钟内有效，请尽快完成操作</li>
                        <li>切勿将验证码泄露给他人</li>
                        <li>如非本人操作，请忽略此邮件</li>
                    </ol>
                </div>
            
                <p>祝您使用愉快(´･ω･`)/<br>
                <hr style='border: 0; border-top: 1px solid #eee; margin: 20px 0;'>
                <p style='color: #999; font-size: 12px;'>此邮件由系统自动发送，请勿直接回复</p>
            </body>
            </html>
            """.formatted(type.getDesc(), code, systemProperties.getEmailCode().getExpiration());
        asyncTaskUtils.execute(() -> {
            try {
                mailUtils.sendHtmlMail(sendCodeDTO.getEmail(), subject, htmlContent);
            } catch (Exception e) {
                log.error("邮件发送失败", e);
            }
        });
    }

    @Override
    public CaptchaVO getCaptcha() {
        CaptchaUtils.CaptchaResult result = captchaUtils.createCaptcha();
        redisUtils.set(
                RedisKeyUtils.getCaptchaKey(result.key()),
                result.code(),
                systemProperties.getCaptcha().getExpiration(),
                TimeUnit.MINUTES
        );
        return CaptchaVO.builder()
                .base64(result.base64())
                .key(result.key())
                .build();
    }

    @Override
    public void logout() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            BlackList blackList = new BlackList();
            blackList.setType(BlackListTypeEnum.TOKEN.getValue());
            blackList.setValue(token);
            blackListMapper.insert(blackList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unregister() {
        Integer userId = UserContext.getUserId();
        String lockKey = LockUtils.getUserLockKey(userId);
        lockUtils.executeWithLock(lockKey, () -> {
            User user = this.getById(userId);
            if (user == null) return;
            TeamMember member = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                    .eq(TeamMember::getUserId, userId));
            if (member != null) {
                Integer teamId = member.getTeamId();
                String teamLockKey = LockUtils.getTeamLockKey(teamId);
                lockUtils.executeWithLock(teamLockKey, () -> {
                    if (TeamRoleEnum.LEADER.getValue().equals(member.getRole())) {
                        List<TeamMember> others = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                                .eq(TeamMember::getTeamId, teamId)
                                .ne(TeamMember::getUserId, userId)
                                .orderByAsc(TeamMember::getCreatedAt));
                        if (!others.isEmpty()) {
                            TeamMember nextLeader = others.getFirst();
                            nextLeader.setRole(TeamRoleEnum.LEADER.getValue());
                            teamMemberMapper.updateById(nextLeader);
                            Team team = teamService.getById(teamId);
                            team.setLeaderId(nextLeader.getUserId());
                            teamService.updateById(team);
                        }
                    }
                    teamMemberMapper.deleteById(member.getId());
                });
            }
            this.removeById(userId);
            BlackList blackList = new BlackList();
            blackList.setType(BlackListTypeEnum.EMAIL.getValue());
            blackList.setValue(user.getEmail());
            blackListMapper.insert(blackList);
            redisUtils.delete(RedisKeyUtils.getUserKey(userId));
        });
    }

    @Override
    public UserVO getUserVOById(Integer userId) {
        String cacheKey = RedisKeyUtils.getUserKey(userId);
        String cachedUserJson = redisUtils.get(cacheKey);
        if (cachedUserJson != null) {
            return JSONUtil.toBean(cachedUserJson, UserVO.class);
        }
        User user = this.getById(userId);
        if (user == null) return null;
        UserVO userVO = UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
        redisUtils.set(cacheKey, cn.hutool.json.JSONUtil.toJsonStr(userVO), 30, TimeUnit.MINUTES);
        return userVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(UpdateProfileDTO dto, HttpServletRequest request) {
        Integer userId = UserContext.getUserId();
        User user = this.getById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        boolean needLogout = false;
        if (dto.getUsername() != null) {
            if (dto.getUsername().equals(user.getUsername())) {
                throw new BizException(ResultCode.SAME_USER);
            }
            long count = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, dto.getUsername())
                    .ne(User::getId, userId));
            if (count > 0) {
                throw new BizException(ResultCode.USER_EXIST);
            }
            user.setUsername(dto.getUsername());
        }
        if (dto.getNewPassword() != null) {
            user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
            needLogout = true;
        }
        if (dto.getNewEmail() != null) {
            if (dto.getNewEmail().equals(user.getEmail())) {
                throw new BizException(ResultCode.SAME_EMAIL);
            }
            String emailCodeKey = RedisKeyUtils.getEmailCodeKey(VerificationCodeTypeEnum.UPDATE_EMAIL.getValue(), dto.getNewEmail());
            String cachedCode = redisUtils.get(emailCodeKey);
            if (cachedCode == null || !cachedCode.equals(dto.getEmailCode())) {
                throw new BizException(ResultCode.EMAIL_CODE_ERROR);
            }
            redisUtils.delete(emailCodeKey);
            long count = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, dto.getNewEmail())
                    .ne(User::getId, userId));
            if (count > 0) {
                throw new BizException(ResultCode.EMAIL_ALREADY_BOUND);
            }
            user.setEmail(dto.getNewEmail());
            needLogout = true;
        }
        this.updateById(user);
        redisUtils.delete(RedisKeyUtils.getUserKey(userId));
        if (needLogout) {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                BlackList blackList = new BlackList();
                blackList.setType(BlackListTypeEnum.TOKEN.getValue());
                blackList.setValue(token);
                blackListMapper.insert(blackList);
            }
        }
        return getUserVOById(userId);
    }
}
