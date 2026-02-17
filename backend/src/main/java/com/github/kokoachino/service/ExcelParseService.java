package com.github.kokoachino.service;

import com.github.kokoachino.model.dto.ExcelParseSettingsDTO;
import com.github.kokoachino.model.vo.ExcelParseResultVO;
import org.springframework.web.multipart.MultipartFile;


/**
 * Excel解析服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-17
 */
public interface ExcelParseService {

    /**
     * 解析Excel配置文件
     *
     * @param excelFile Excel文件
     * @param mappingMode 映射模式：id-按图片ID映射，order-按顺序映射
     * @param settings 解析设置
     * @return 解析结果
     */
    ExcelParseResultVO parseExcel(MultipartFile excelFile, String mappingMode, ExcelParseSettingsDTO settings);
}
