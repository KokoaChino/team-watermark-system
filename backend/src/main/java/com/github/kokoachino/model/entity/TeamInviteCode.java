package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.github.kokoachino.common.enums.InviteCodeStatusEnum;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 团队邀请码实体类
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@TableName("tw_team_invite_code")
public class TeamInviteCode {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 团队 ID
     */
    private Integer teamId;

    /**
     * 邀请码
     */
    private String code;

    /**
     * 有效期截止时间
     */
    private LocalDateTime validUntil;

    /**
     * 最大使用次数
     */
    private Integer maxUses;

    /**
     * 已使用次数
     */
    private Integer usesCount;

    /**
     * @see InviteCodeStatusEnum
     * 状态
     */
    private String status;

    /**
     * 创建人 ID（队长）
     */
    private Integer createdById;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
