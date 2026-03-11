package com.github.kokoachino.model.dto;

import com.github.kokoachino.common.enums.PointChangeTypeEnum;
import com.github.kokoachino.common.enums.PointSourceTypeEnum;
import lombok.Builder;
import lombok.Data;


/**
 * 点数流水日志写入命令
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Builder
public class PointChangeLogRecordDTO {

    private Integer teamId;
    private PointChangeTypeEnum changeType;
    private Integer operatorUserId;
    private String operatorUsername;
    private String operatorUserStatus;
    private PointSourceTypeEnum sourceType;
    private String sourceId;
    private Integer points;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private String description;
}
