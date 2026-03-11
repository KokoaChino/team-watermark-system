package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 批量任务实体
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@TableName("tw_batch_task")
public class BatchTask {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String taskNo;
    private Integer teamId;
    private Integer createdById;
    private String createdByUsername;
    private String userStatus;
    private Integer templateId;
    private String templateName;
    private Integer templateVersion;
    private String templateSnapshot;
    private String description;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private Long totalDurationMs;
    private Long totalSize;
    private String resultZipKey;
    private String report;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}