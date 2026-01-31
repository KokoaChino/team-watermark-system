package github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 团队实体类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Data
@TableName("tw_team")
public class Team {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private Integer pointBalance;

    private Integer leaderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
