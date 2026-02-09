package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 团队邀请记录实体类
 * 记录谁通过哪个邀请码加入了团队
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@TableName("tw_team_invite_record")
public class TeamInviteRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 邀请码 ID
     */
    private Integer inviteCodeId;

    /**
     * 团队 ID
     */
    private Integer teamId;

    /**
     * 被邀请人用户 ID
     */
    private Integer userId;

    /**
     * 被邀请人用户名
     */
    private String username;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
