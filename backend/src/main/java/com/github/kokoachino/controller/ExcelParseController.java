package com.github.kokoachino.controller;

import com.github.kokoachino.common.result.Result;
import com.github.kokoachino.model.vo.ExcelParseResultVO;
import com.github.kokoachino.service.ExcelParseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


/**
 * Excel 解析控制器
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
@Tag(name = "Excel解析", description = "Excel配置文件解析接口")
public class ExcelParseController {

    private final ExcelParseService excelParseService;

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "解析Excel配置", description = "解析Excel配置文件，返回图片ID与水印配置的对应关系")
    public Result<ExcelParseResultVO> parseExcel(
            @RequestParam @Parameter(description = "Excel文件") MultipartFile excelFile,
            @RequestParam @Parameter(description = "映射模式：id-按图片ID映射，order-按顺序映射") String mappingMode,
            @RequestParam(required = false) @Parameter(description = "图片ID列表（按顺序映射时不需要）") List<String> imageIds) {
        ExcelParseResultVO result = excelParseService.parseExcel(excelFile, mappingMode, imageIds);
        return Result.success(result);
    }
}
