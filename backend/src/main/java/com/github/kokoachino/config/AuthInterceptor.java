package com.github.kokoachino.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.kokoachino.common.enums.BlackListTypeEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.JwtUtils;
import com.github.kokoachino.common.util.TeamContext;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.mapper.BlackListMapper;
import com.github.kokoachino.model.entity.BlackList;
import com.github.kokoachino.model.vo.TeamMemberVO;
import com.github.kokoachino.model.vo.UserVO;
import com.github.kokoachino.service.TeamService;
import com.github.kokoachino.service.UserService;
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
    private final TeamService teamService;
    private final BlackListMapper blackListMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        token = token.substring(7);
        long count = blackListMapper.selectCount(new LambdaQueryWrapper<BlackList>()
                .eq(BlackList::getType, BlackListTypeEnum.TOKEN.getValue())
                .eq(BlackList::getValue, token));
        if (count > 0) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (!jwtUtils.validateToken(token)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        Integer userId = jwtUtils.getUserIdFromToken(token);
        UserVO userVO = userService.getUserVOById(userId);
        if (userVO == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        UserContext.setUser(userVO);
        TeamMemberVO teamMemberVO = teamService.getCurrentTeamInfo(userId);
        if (teamMemberVO != null) {
            TeamContext.setTeam(teamMemberVO);
        }
        if (jwtUtils.shouldRenewToken(token)) {
            String newToken = jwtUtils.generateToken(userId);
            response.setHeader("X-New-Token", newToken);
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        UserContext.clear();
        TeamContext.clear();
    }
}
