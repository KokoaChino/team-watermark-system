package github.kokoachino.common.util;

import github.kokoachino.model.vo.UserVO;


/**
 * @author kokoachino
 * @date 2026-02-02
 */
public class UserContext {

    private static final ThreadLocal<UserVO> CONTEXT = new ThreadLocal<>();

    public static void setUser(UserVO user) {
        CONTEXT.set(user);
    }

    public static UserVO getUser() {
        return CONTEXT.get();
    }

    public static Integer getUserId() {
        UserVO user = CONTEXT.get();
        return user != null ? user.getId() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
