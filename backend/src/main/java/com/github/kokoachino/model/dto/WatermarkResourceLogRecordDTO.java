package com.github.kokoachino.model.dto;

import com.github.kokoachino.common.enums.WatermarkResourceEventTypeEnum;
import com.github.kokoachino.common.enums.WatermarkResourceScopeEnum;
import lombok.Builder;
import lombok.Data;
import java.util.Map;


/**
 * 水印资源日志写入命令
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Builder
public class WatermarkResourceLogRecordDTO {

    private Integer teamId;
    private WatermarkResourceScopeEnum resourceScope;
    private WatermarkResourceEventTypeEnum eventType;
    private Integer operatorUserId;
    private String operatorUsername;
    private String operatorUserStatus;
    private Integer resourceId;
    private String resourceName;
    private Object beforeData;
    private Object afterData;
    private Map<String, Object> details;
}
