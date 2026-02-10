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

    @Schema(description = "字体ID", example = "1")
    private Integer id;

    @Schema(description = "字体名称", example = "阿里巴巴普惠体")
    private String name;

    @Schema(description = "字体文件URL", example = "https://minio.example.com/fonts/abc123.ttf")
    private String fontUrl;

    @Schema(description = "是否系统字体", example = "true")
    private Boolean isSystemFont;

    @Schema(description = "团队ID", example = "1")
    private Integer teamId;

    @Schema(description = "上传人ID", example = "2")
    private Integer uploadedBy;

    @Schema(description = "创建时间", example = "2026-02-10T14:30:00")
    private LocalDateTime createdAt;
}
