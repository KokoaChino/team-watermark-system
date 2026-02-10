package com.github.kokoachino.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 邀请码状态枚举
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Getter
@AllArgsConstructor
public enum InviteCodeStatusEnum {

    ACTIVE("active", "有效"),
    INACTIVE("inactive", "已失效");

    private final String value;
    private final String desc;

    public static InviteCodeStatusEnum fromValue(String value) {
        for (InviteCodeStatusEnum status : InviteCodeStatusEnum.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return null;
    }
}