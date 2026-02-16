package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 黑名单实体类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Data
@TableName("tw_black_list")
public class BlackList {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String type;

    private String value;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
