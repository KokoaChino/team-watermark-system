package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 字体 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Builder
@Schema(description = "字体信息")
public class FontVO {

    @Schema(description = "字体ID")
    private Integer id;

    @Schema(description = "字体名称")
    private String name;

    @Schema(description = "字体文件URL")
    private String fontUrl;

    @Schema(description = "是否系统字体")
    private Boolean isSystemFont;

    @Schema(description = "团队ID（系统字体为null）")
    private Integer teamId;

    @Schema(description = "上传人ID")
    private Integer uploadedBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
