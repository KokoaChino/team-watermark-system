package com.github.kokoachino.service;

import com.github.kokoachino.model.vo.ExcelParseResultVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


/**
 * Excel 解析服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
public interface ExcelParseService {

    /**
     * 解析 Excel 配置文件
     *
     * @param excelFile   Excel 文件
     * @param mappingMode 映射模式：id-按图片ID映射，order-按顺序映射
     * @param imageIds    图片ID列表（用于验证和按顺序映射）
     * @return 解析结果
     */
    ExcelParseResultVO parseExcel(MultipartFile excelFile, String mappingMode, List<String> imageIds);
}
