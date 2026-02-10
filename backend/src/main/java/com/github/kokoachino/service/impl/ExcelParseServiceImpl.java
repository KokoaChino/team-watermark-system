package com.github.kokoachino.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.model.vo.ExcelParseResultVO;
import com.github.kokoachino.service.ExcelParseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;


/**
 * Excel 解析服务实现
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Slf4j
@Service
public class ExcelParseServiceImpl implements ExcelParseService {

    @Override
    public ExcelParseResultVO parseExcel(MultipartFile excelFile, String mappingMode, List<String> imageIds) {
        List<List<String>> dataList = new ArrayList<>();
        List<String> headers = new ArrayList<>();

        try {
            // 读取所有数据
            EasyExcel.read(excelFile.getInputStream(), new ReadListener<Map<Integer, String>>() {
                private boolean isFirstRow = true;

                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    if (isFirstRow) {
                        // 第一行是表头
                        data.forEach((key, value) -> headers.add(value != null ? value : ""));
                        isFirstRow = false;
                    } else {
                        // 数据行
                        List<String> row = new ArrayList<>();
                        data.forEach((key, value) -> row.add(value));
                        dataList.add(row);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("Excel解析完成，共{}行数据", dataList.size());
                }
            }).sheet().doRead();
        } catch (IOException e) {
            log.error("Excel文件读取失败", e);
            throw new BizException(ResultCode.EXCEL_PARSE_ERROR);
        }

        if (headers.isEmpty()) {
            throw new BizException(ResultCode.EXCEL_STRUCTURE_INVALID);
        }

        // 解析数据
        ExcelParseResultVO.ExcelParseResultVOBuilder resultBuilder = ExcelParseResultVO.builder()
                .headers(headers)
                .totalRows(dataList.size());

        if ("order".equals(mappingMode)) {
            // 按顺序映射
            List<ExcelParseResultVO.ImageConfigVO> configByOrder = parseByOrder(dataList, headers, imageIds);
            resultBuilder.configByOrder(configByOrder);
        } else {
            // 按ID映射（默认）
            Map<String, ExcelParseResultVO.ImageConfigVO> configById = parseById(dataList, headers);
            resultBuilder.configById(configById);
        }

        return resultBuilder.build();
    }

    /**
     * 按ID映射解析
     */
    private Map<String, ExcelParseResultVO.ImageConfigVO> parseById(List<List<String>> dataList, List<String> headers) {
        Map<String, ExcelParseResultVO.ImageConfigVO> result = new LinkedHashMap<>();

        for (List<String> row : dataList) {
            if (row.isEmpty() || (row.get(0) == null || row.get(0).trim().isEmpty())) {
                continue; // 跳过空行
            }

            String imageId = row.get(0).trim();
            ExcelParseResultVO.ImageConfigVO config = parseRowToConfig(row, headers, imageId);
            result.put(imageId, config);
        }

        return result;
    }

    /**
     * 按顺序映射解析
     */
    private List<ExcelParseResultVO.ImageConfigVO> parseByOrder(List<List<String>> dataList, List<String> headers, List<String> imageIds) {
        List<ExcelParseResultVO.ImageConfigVO> result = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            List<String> row = dataList.get(i);
            if (row.isEmpty() || (row.get(0) == null || row.get(0).trim().isEmpty())) {
                continue; // 跳过空行
            }

            // 按顺序分配图片ID
            String imageId = (imageIds != null && i < imageIds.size()) ? imageIds.get(i) : String.valueOf(i);
            ExcelParseResultVO.ImageConfigVO config = parseRowToConfig(row, headers, imageId);
            result.add(config);
        }

        return result;
    }

    /**
     * 将一行数据解析为配置
     */
    private ExcelParseResultVO.ImageConfigVO parseRowToConfig(List<String> row, List<String> headers, String imageId) {
        Map<String, String> textReplacements = new LinkedHashMap<>();
        Map<String, String> imageReplacements = new LinkedHashMap<>();
        String outputFilename = null;
        String outputPath = null;

        // 解析后续列
        for (int i = 1; i < headers.size() && i < row.size(); i++) {
            String header = headers.get(i);
            String value = row.get(i);

            if (value == null || value.trim().isEmpty()) {
                continue;
            }

            value = value.trim();

            // 根据表头判断类型
            if (header.contains("文字") || header.contains("text") || header.contains("Text")) {
                // 提取索引或标识
                String key = extractKey(header, "text");
                textReplacements.put(key, value);
            } else if (header.contains("图片") || header.contains("image") || header.contains("Image")) {
                String key = extractKey(header, "image");
                imageReplacements.put(key, value);
            } else if (header.contains("输出文件名") || header.contains("filename")) {
                outputFilename = value;
            } else if (header.contains("输出路径") || header.contains("path")) {
                outputPath = value;
            } else {
                // 默认按顺序作为文字水印
                textReplacements.put(String.valueOf(i - 1), value);
            }
        }

        return ExcelParseResultVO.ImageConfigVO.builder()
                .imageId(imageId)
                .textReplacements(textReplacements)
                .imageReplacements(imageReplacements)
                .outputFilename(outputFilename)
                .outputPath(outputPath)
                .rawData(row)
                .build();
    }

    /**
     * 从表头提取key
     */
    private String extractKey(String header, String type) {
        // 尝试提取数字索引，如 "文字1" -> "1", "text_0" -> "0"
        String number = header.replaceAll("[^0-9]", "");
        if (!number.isEmpty()) {
            return number;
        }
        // 如果没有数字，使用表头本身作为key
        return header;
    }
}
