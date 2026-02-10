package com.github.kokoachino.model.dto.batch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


/**
 * Excel 解析请求 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Schema(description = "Excel解析请求")
public class ExcelParseRequestDTO {

    @NotNull(message = "Excel文件不能为空")
    @Schema(description = "Excel配置文件")
    private MultipartFile excelFile;

    @NotBlank(message = "映射模式不能为空")
    @Schema(description = "映射模式：id-按图片ID映射，order-按顺序映射", example = "id")
    private String mappingMode;

    @Schema(description = "图片ID列表（按顺序映射时不需要）")
    private List<String> imageIds;
}
