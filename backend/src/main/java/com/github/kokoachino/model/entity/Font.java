package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 字体实体类
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@TableName("tw_font")
public class Font {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 字体名称
     */
    private String name;

    /**
     * 字体文件 MinIO Key
     */
    private String fontKey;

    /**
     * 所属团队ID（NULL为系统字体）
     */
    private Integer teamId;

    /**
     * 上传人ID
     */
    private Integer uploadedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
