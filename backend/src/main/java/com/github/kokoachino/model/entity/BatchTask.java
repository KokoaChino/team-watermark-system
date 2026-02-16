package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 批量任务实体（简化版）
 * 只记录点数相关信息，不维护任务状态
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@TableName("tw_batch_task")
public class BatchTask {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 任务编号
     */
    private String taskNo;

    /**
     * 团队ID
     */
    private Integer teamId;

    /**
     * 创建人ID
     */
    private Integer createdById;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 图片总数
     */
    private Integer imageCount;

    /**
     * 成功处理数量
     */
    private Integer successCount;

    /**
     * 预扣点数
     */
    private Integer deductedPoints;

    /**
     * 实际消耗点数
     */
    private Integer consumedPoints;

    /**
     * 返还点数
     */
    private Integer refundedPoints;

    /**
     * 结果 ZIP 文件的 MinIO Key
     */
    private String resultZipKey;

    /**
     * 处理报表（JSON格式）
     */
    private String report;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
