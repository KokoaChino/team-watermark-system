package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.model.dto.ExcelParseSettingsDTO;
import com.github.kokoachino.model.dto.ExcelTemplateBaseRequestDTO;
import com.github.kokoachino.model.vo.ExcelParseResultVO;
import com.github.kokoachino.service.ExcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


/**
 * Excel 控制器
 *
 * @author Kokoa_Chino
 * @date 2026-03-14
 */
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
@Tag(name = "Excel相关", description = "Excel配置文件解析与生成接口")
public class ExcelController {

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExcelService excelService;

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "解析Excel配置", description = """
            解析Excel配置文件，返回图片ID与水印配置的对应关系
            
            支持的表头：
            - id：图片ID（ID映射模式下必须）
            - 文字水印：文字水印内容，支持多列（表头后的空列也属于该区域）
            - 图片水印：图片水印URL，支持多列
            - 文件路径：输出路径，支持多列
            - 重命名：输出文件名
            - 拓展名：输出格式转换
            
            映射模式：
            - id：按图片ID映射，读取id列不为空的行（默认）
            - order：按顺序映射，跳过空行
            """)
    public Result<ExcelParseResultVO> parseExcel(
            @Parameter(description = "Excel文件(.xlsx或.xls)") @RequestParam MultipartFile excelFile,
            @Parameter(description = "映射模式：id-按图片ID映射，order-按顺序映射（默认：id）", example = "id") 
            @RequestParam(defaultValue = "id") String mappingMode,
            @Parameter(description = "重复ID处理策略：first-保留第一个，last-保留最后一个，error-报错终止（默认：first）", example = "first") 
            @RequestParam(required = false) String duplicateHandling,
            @Parameter(description = "异常字符处理策略：underscore-用下划线替代，error_folder-统一放到ERROR文件夹，error-报错终止（默认：underscore）", example = "underscore") 
            @RequestParam(required = false) String invalidCharHandling) {
        ExcelParseSettingsDTO settings = new ExcelParseSettingsDTO();
        settings.setDuplicateHandling(duplicateHandling);
        settings.setInvalidCharHandling(invalidCharHandling);
        ExcelParseResultVO result = excelService.parseExcel(excelFile, mappingMode, settings);
        return Result.success(result);
    }

    @PostMapping(value = "/template/base", consumes = MediaType.APPLICATION_JSON_VALUE, produces = EXCEL_CONTENT_TYPE)
    @Operation(summary = "下载Excel基座模板", description = """
            根据映射模式和水印数量，生成用于“导入 Excel 配置”的基座模板。

            表头规则：
            - id（仅 id 映射模式）
            - 文字水印（按数量生成区域）
            - 图片水印（按数量生成区域）
            - 文件路径（固定 3 列）
            - 重命名（1 列）
            - 拓展名（1 列）
            """)
    public ResponseEntity<byte[]> downloadTemplateBase(@Valid @RequestBody ExcelTemplateBaseRequestDTO request) {
        byte[] excelData = excelService.generateTemplateBase(request);
        String fileName = UUID.randomUUID().toString().replace("-", "") + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}
