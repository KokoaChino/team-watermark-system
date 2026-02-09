package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 水印模板实体类
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@TableName("tw_watermark_template")
public class WatermarkTemplate {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 团队ID
     */
    private Integer teamId;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 水印配置（JSON格式）
     */
    private String config;

    /**
     * 创建人ID
     */
    private Integer createdById;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
