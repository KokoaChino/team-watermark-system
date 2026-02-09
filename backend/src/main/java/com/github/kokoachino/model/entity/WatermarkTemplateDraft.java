package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 水印模板草稿实体类
 * 用户工作区，每个用户同时只能有一个草稿
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@TableName("tw_watermark_template_draft")
public class WatermarkTemplateDraft {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 源模板ID（为空表示新建）
     */
    private Integer sourceTemplateId;

    /**
     * 源模板版本号（用于冲突检测）
     */
    private Integer sourceVersion;

    /**
     * 草稿配置（JSON格式）
     */
    private String config;

    /**
     * 草稿名称
     */
    private String name;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
