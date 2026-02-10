package com.github.kokoachino.config;

import com.github.kokoachino.common.enums.BlackListTypeEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.JwtUtils;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.mapper.BlackListMapper;
import com.github.kokoachino.model.entity.BlackList;
import com.github.kokoachino.model.vo.UserVO;
import com.github.kokoachino.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * 认证拦截器
 *
 * @author kokoachino
 * @date 2026-02-02
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final BlackListMapper blackListMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        token = token.substring(7);
        // 1. 校验 Token 是否在黑名单
        long count = blackListMapper.selectCount(new LambdaQueryWrapper<BlackList>()
                .eq(BlackList::getType, BlackListTypeEnum.TOKEN.getValue())
                .eq(BlackList::getValue, token));
        if (count > 0) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        // 2. 校验 Token 有效性
        if (!jwtUtils.validateToken(token)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        // 3. 解析用户并存入上下文
        Integer userId = jwtUtils.getUserIdFromToken(token);
        UserVO userVO = userService.getUserVOById(userId);
        if (userVO == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        UserContext.setUser(userVO);
        // 4. 检查是否需要续期（剩余有效期 < 7 天）
        if (jwtUtils.shouldRenewToken(token)) {
            String newToken = jwtUtils.generateToken(userId);
            // 通过响应头返回新 Token，前端需要监听此头并替换本地 Token
            response.setHeader("X-New-Token", newToken);
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        UserContext.clear();
    }
}
