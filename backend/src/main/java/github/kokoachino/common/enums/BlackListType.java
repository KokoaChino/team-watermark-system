package github.kokoachino.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author kokoachino
 * @date 2026-02-02
 */
@Getter
@AllArgsConstructor
public enum BlackListType {

    TOKEN("token", "JWT黑名单"),
    EMAIL("email", "邮箱黑名单");

    private final String value;
    private final String desc;

    public static BlackListType fromValue(String value) {
        for (BlackListType type : BlackListType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
