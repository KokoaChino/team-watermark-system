package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 团队成员实体类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Data
@TableName("tw_team_member")
public class TeamMember {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer teamId;

    private Integer userId;

    private String role;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
