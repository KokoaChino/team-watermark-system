package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 点数流水日志实体
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@TableName("tw_point_change_log")
public class PointChangeLog {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer teamId;
    private String changeType;
    private Integer operatorUserId;
    private String operatorUsername;
    private String operatorUserStatus;
    private String sourceType;
    private String sourceId;
    private Integer points;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private String description;
    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
