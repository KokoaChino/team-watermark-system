package github.kokoachino.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author kokoachino
 * @date 2026-02-02
 */
@Getter
@AllArgsConstructor
public enum TeamRole {

    LEADER("leader", "队长"),
    MEMBER("member", "成员");

    private final String value;
    private final String desc;

    public static TeamRole fromValue(String value) {
        for (TeamRole role : TeamRole.values()) {
            if (role.getValue().equals(value)) {
                return role;
            }
        }
        return null;
    }
}
