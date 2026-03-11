package com.github.kokoachino.common.enums;

import lombok.Getter;


/**
 * 团队变更事件类型枚举
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Getter
public enum TeamEventTypeEnum {

    INVITE_CODE_CREATE("invite_code_create", "邀请码创建"),
    INVITE_CODE_DEACTIVATE("invite_code_deactivate", "邀请码停用"),
    MEMBER_JOIN("member_join", "成员加入"),
    MEMBER_LEAVE("member_leave", "成员退出"),
    MEMBER_KICK("member_kick", "成员移出"),
    LEADER_TRANSFER("leader_transfer", "队长转移"),
    TEAM_RENAME("team_rename", "团队更名");

    private final String value;
    private final String description;

    TeamEventTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static TeamEventTypeEnum fromValue(String value) {
        for (TeamEventTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
