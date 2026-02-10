package com.github.kokoachino.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author kokoachino
 * @date 2026-02-02
 */
@Getter
@AllArgsConstructor
public enum TeamRoleEnum {

    LEADER("leader", "队长"),
    MEMBER("member", "成员");

    private final String value;
    private final String desc;

    public static TeamRoleEnum fromValue(String value) {
        for (TeamRoleEnum role : TeamRoleEnum.values()) {
            if (role.getValue().equals(value)) {
                return role;
            }
        }
        return null;
    }
}
