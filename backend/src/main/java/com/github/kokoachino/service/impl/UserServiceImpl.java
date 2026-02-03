package com.github.kokoachino.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.kokoachino.common.enums.BlackListType;
import com.github.kokoachino.common.enums.LoginType;
import com.github.kokoachino.common.enums.TeamRole;
import com.github.kokoachino.common.enums.VerificationCodeType;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.*;
import com.github.kokoachino.config.SystemProperties;
import com.github.kokoachino.model.dto.*;
import com.github.kokoachino.config.RabbitConfig;
import com.github.kokoachino.mapper.BlackListMapper;
import com.github.kokoachino.mapper.TeamMemberMapper;
import com.github.kokoachino.mapper.UserMapper;
import com.github.kokoachino.model.entity.BlackList;
import com.github.kokoachino.model.entity.Team;
import com.github.kokoachino.model.entity.TeamMember;
import com.github.kokoachino.model.entity.User;
import com.github.kokoachino.model.vo.CaptchaVO;
import com.github.kokoachino.model.vo.TokenVO;
import com.github.kokoachino.model.vo.UserVO;
import com.github.kokoachino.service.TeamService;
import com.github.kokoachino.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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
    private final CaptchaUtils captchaUtils;
    private final LockUtils lockUtils;
    private final SystemProperties systemProperties;
    private final AmqpTemplate amqpTemplate;

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
        LoginType loginType = LoginType.fromValue(loginDTO.getLoginType());
        if (loginType == null) {
            throw new BizException(ResultCode.UNSUPPORTED_LOGIN_TYPE);
        }
        if (loginType == LoginType.PASSWORD) {
            // 密码登录
            if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
                throw new BizException(ResultCode.PASSWORD_ERROR);
            }
        } else if (loginType == LoginType.CAPTCHA) {
            // 验证码登录
            String emailCode = loginDTO.getEmailCode();
            String redisKey = RedisKeyUtils.getEmailCodeKey(VerificationCodeType.LOGIN.getValue(), user.getEmail());
            String cachedCode = redisUtils.get(redisKey);
            if (cachedCode == null || !cachedCode.equals(emailCode)) {
                throw new BizException(ResultCode.EMAIL_CODE_ERROR);
            }
            // 验证通过后删除验证码
            redisUtils.delete(redisKey);
        }
        // 生成 Access Token 和 Refresh Token
        String accessToken = jwtUtils.generateAccessToken(user.getId());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());
        UserVO userVO = getUserVOById(user.getId());
        userVO.setToken(accessToken);
        userVO.setRefreshToken(refreshToken);
        return userVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        // 1. 校验人机验证码
        String captchaKey = registerDTO.getCaptchaKey();
        String redisCaptchaKey = RedisKeyUtils.getCaptchaKey(captchaKey);
        String cachedCaptcha = redisUtils.get(redisCaptchaKey);
        if (cachedCaptcha == null || !cachedCaptcha.equalsIgnoreCase(registerDTO.getCaptcha())) {
            throw new BizException(ResultCode.CAPTCHA_ERROR);
        }
        redisUtils.delete(redisCaptchaKey);
        // 2. 校验邮箱验证码
        String emailCodeKey = RedisKeyUtils.getEmailCodeKey(VerificationCodeType.REGISTER.getValue(), registerDTO.getEmail());
        String cachedEmailCode = redisUtils.get(emailCodeKey);
        if (cachedEmailCode == null || !cachedEmailCode.equals(registerDTO.getEmailCode())) {
            throw new BizException(ResultCode.EMAIL_CODE_ERROR);
        }
        redisUtils.delete(emailCodeKey);
        // 3. 校验用户是否存在
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername())
                .or()
                .eq(User::getEmail, registerDTO.getEmail()));
        if (count > 0) {
            throw new BizException(ResultCode.USER_EXIST);
        }
        // 4. 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        this.save(user);
        // 5. 检查黑名单以确定初始点数
        long isBlacklisted = blackListMapper.selectCount(new LambdaQueryWrapper<BlackList>()
                .eq(BlackList::getType, BlackListType.EMAIL.getValue())
                .eq(BlackList::getValue, registerDTO.getEmail()));
        int initialPoints = isBlacklisted > 0 ? 0 : systemProperties.getInitialPoints();
        // 6. 创建个人团队
        teamService.createPersonalTeam(user.getId(), user.getUsername(), initialPoints);
    }

    @Override
    public void forgotPassword(ForgotPasswordDTO forgotPasswordDTO) {
        // 1. 校验邮箱验证码
        String emailCodeKey = RedisKeyUtils.getEmailCodeKey(VerificationCodeType.FORGOT_PASSWORD.getValue(), forgotPasswordDTO.getEmail());
        String cachedEmailCode = redisUtils.get(emailCodeKey);
        if (cachedEmailCode == null || !cachedEmailCode.equals(forgotPasswordDTO.getCode())) {
            throw new BizException(ResultCode.EMAIL_CODE_ERROR);
        }
        redisUtils.delete(emailCodeKey);
        // 2. 更新密码
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
        VerificationCodeType type = VerificationCodeType.fromValue(typeValue);
        if (type == null) {
            throw new BizException(ResultCode.VALIDATE_FAILED);
        }
        String redisKey = RedisKeyUtils.getEmailCodeKey(typeValue, sendCodeDTO.getEmail());
        // 校验发送频率
        if (redisUtils.hasKey(redisKey)) {
            // 如果还剩60秒以上，说明才刚发过
            Long expire = redisUtils.getExpire(redisKey);
            if (expire != null && expire > (systemProperties.getEmailCode().getExpiration() * 60 - 60)) {
                throw new BizException(ResultCode.REQUEST_TOO_FREQUENT);
            }
        }
        // 存入 Redis
        redisUtils.set(redisKey, code, systemProperties.getEmailCode().getExpiration(), TimeUnit.MINUTES);
        // 发送邮件 (异步)
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
        MailMessage mailMessage = MailMessage.builder()
                .to(sendCodeDTO.getEmail())
                .subject(subject)
                .content(htmlContent)
                .isHtml(true)
                .build();
        amqpTemplate.convertAndSend(RabbitConfig.MAIL_QUEUE, mailMessage);
    }

    @Override
    public CaptchaVO getCaptcha() {
        CaptchaUtils.CaptchaResult result = captchaUtils.createCaptcha();
        // 将验证码存入 Redis
        redisUtils.set(
                RedisKeyUtils.getCaptchaKey(result.key()),
                result.code(),
                systemProperties.getCaptcha().getExpiration(),
                TimeUnit.MINUTES
        );
        // 返回前端需要的数据（不包含 code）
        return CaptchaVO.builder()
                .base64(result.base64())
                .key(result.key())
                .build();
    }

    @Override
    public TokenVO refreshToken(RefreshTokenDTO refreshTokenDTO) {
        String refreshToken = refreshTokenDTO.getRefreshToken();
        // 1. 验证 Refresh Token 有效性
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        // 2. 检查是否为 Refresh Token
        if (!jwtUtils.isRefreshToken(refreshToken)) {
            throw new BizException(ResultCode.VALIDATE_FAILED);
        }
        // 3. 检查 Token 是否在黑名单中
        long count = blackListMapper.selectCount(new LambdaQueryWrapper<BlackList>()
                .eq(BlackList::getType, BlackListType.TOKEN.getValue())
                .eq(BlackList::getValue, refreshToken));
        if (count > 0) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        // 4. 解析用户 ID 并生成新的 Access Token
        Integer userId = jwtUtils.getUserIdFromToken(refreshToken);
        String newAccessToken = jwtUtils.generateAccessToken(userId);
        return TokenVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtils.getAccessTokenExpiration())
                .build();
    }

    @Override
    public void logout() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            // 将 Token 加入黑名单
            BlackList blackList = new BlackList();
            blackList.setType(BlackListType.TOKEN.getValue());
            blackList.setValue(token);
            blackListMapper.insert(blackList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unregister() {
        Integer userId = UserContext.getUserId();
        String lockKey = LockUtils.getUserLockKey(userId);
        String lockValue = UUID.randomUUID().toString();
        // 使用分布式锁保证原子性
        lockUtils.executeWithLock(lockKey, lockValue, () -> {
            User user = this.getById(userId);
            if (user == null) return;
            // 1. 获取用户所属团队（用户同一时刻只能存在于一个团队）
            TeamMember member = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                    .eq(TeamMember::getUserId, userId));
            if (member != null) {
                Integer teamId = member.getTeamId();
                // 使用团队锁
                String teamLockKey = LockUtils.getTeamLockKey(teamId);
                String teamLockValue = UUID.randomUUID().toString();
                lockUtils.executeWithLock(teamLockKey, teamLockValue, () -> {
                    if (TeamRole.LEADER.getValue().equals(member.getRole())) {
                        // 队长逻辑：查找其他成员
                        List<TeamMember> others = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                                .eq(TeamMember::getTeamId, teamId)
                                .ne(TeamMember::getUserId, userId)
                                .orderByAsc(TeamMember::getCreatedAt));
                        if (others.isEmpty()) {
                            // 仅剩自己，解散团队
                            teamService.removeById(teamId);
                        } else {
                            // 转移队长身份给最早加入的成员
                            TeamMember nextLeader = others.get(0);
                            nextLeader.setRole(TeamRole.LEADER.getValue());
                            teamMemberMapper.updateById(nextLeader);

                            Team team = teamService.getById(teamId);
                            team.setLeaderId(nextLeader.getUserId());
                            teamService.updateById(team);
                        }
                    }
                    // 删除成员记录
                    teamMemberMapper.deleteById(member.getId());
                });
            }
            // 2. 删除用户
            this.removeById(userId);
            // 3. 邮箱加入黑名单防止刷点数
            BlackList blackList = new BlackList();
            blackList.setType(BlackListType.EMAIL.getValue());
            blackList.setValue(user.getEmail());
            blackListMapper.insert(blackList);
            // 4. 清除用户缓存
            redisUtils.delete(RedisKeyUtils.getUserKey(userId));
        });
    }

    @Override
    public UserVO getUserVOById(Integer userId) {
        // 先从 Redis 缓存中获取
        String cacheKey = RedisKeyUtils.getUserKey(userId);
        String cachedUserJson = redisUtils.get(cacheKey);
        if (cachedUserJson != null) {
            // 从缓存中返回
            return cn.hutool.json.JSONUtil.toBean(cachedUserJson, UserVO.class);
        }
        // 缓存中没有，从数据库查询
        User user = this.getById(userId);
        if (user == null) return null;
        // 获取用户所属团队（一个用户同一时刻只能存在于一个团队中）
        TeamMember member = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getUserId, userId)
                .last("limit 1"));
        UserVO userVO = UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .teamId(member != null ? member.getTeamId() : null)
                .teamRole(member != null ? member.getRole() : null)
                .build();
        // 存入 Redis 缓存，有效期 30 分钟
        redisUtils.set(cacheKey, cn.hutool.json.JSONUtil.toJsonStr(userVO), 30, TimeUnit.MINUTES);
        return userVO;
    }
}
