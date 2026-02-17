package com.github.kokoachino.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.github.kokoachino.common.enums.DuplicateHandlingEnum;
import com.github.kokoachino.common.enums.HeaderEnum;
import com.github.kokoachino.common.enums.InvalidCharHandlingEnum;
import com.github.kokoachino.common.enums.MappingModeEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.model.dto.ExcelParseSettingsDTO;
import com.github.kokoachino.model.vo.ExcelParseResultVO;
import com.github.kokoachino.service.ExcelParseService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;


/**
 * Excel解析服务实现
 *
 * @author Kokoa_Chino
 * @date 2026-02-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelParseServiceImpl implements ExcelParseService {

    private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile("[*^\\\\/:|\"<>?]");
    private static final Pattern INVALID_EXTENSION_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

    @Override
    public ExcelParseResultVO parseExcel(MultipartFile excelFile, String mappingMode, ExcelParseSettingsDTO settings) {
        if (settings == null) {
            settings = new ExcelParseSettingsDTO();
        }
        // 1. 读取Excel原始数据
        List<List<String>> allRows = readExcelData(excelFile);
        if (allRows.isEmpty()) {
            throw new BizException(ResultCode.EXCEL_EMPTY);
        }
        // 2. 解析表头，识别各列位置
        List<String> headerRow = allRows.getFirst();
        HeaderInfo headerInfo = parseHeader(headerRow);
        // 3. 校验映射模式与表头匹配
        MappingModeEnum mode = MappingModeEnum.fromValue(mappingMode);
        if (mode == MappingModeEnum.ID && headerInfo.getIdColumnIndex() == -1) {
            throw new BizException(ResultCode.EXCEL_MISSING_ID_COLUMN);
        }
        // 4. 解析数据行
        List<List<String>> dataRows = allRows.size() > 1 ? allRows.subList(1, allRows.size()) : new ArrayList<>();
        List<ExcelParseResultVO.ImageConfigVO> configs = parseDataRows(dataRows, headerInfo, mode, settings);
        // 5. 构建返回结果
        return ExcelParseResultVO.builder()
                .configs(configs)
                .validRowCount(configs.size())
                .build();
    }

    /**
     * 读取Excel原始数据
     */
    private List<List<String>> readExcelData(MultipartFile excelFile) {
        List<List<String>> allRows = new ArrayList<>();
        try {
            EasyExcel.read(excelFile.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    int maxCol = data.keySet().stream().max(Integer::compareTo).orElse(-1);
                    List<String> row = new ArrayList<>();
                    for (int i = 0; i <= maxCol; i++) {
                        String value = data.get(i);
                        row.add(value != null ? value.trim() : "");
                    }
                    allRows.add(row);
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("Excel读取完成，共{}行", allRows.size());
                }
            }).sheet(0).doRead();
        } catch (IOException e) {
            log.error("Excel文件读取失败", e);
            throw new BizException(ResultCode.EXCEL_READ_FAILED);
        }
        return allRows;
    }

    /**
     * 解析表头，识别各列位置
     */
    private HeaderInfo parseHeader(List<String> headerRow) {
        HeaderInfo info = new HeaderInfo();
        int colCount = headerRow.size();
        int i = 0;
        // 遍历表头，识别各列
        while (i < colCount) {
            String header = headerRow.get(i).toLowerCase().trim();
            if (header.isEmpty()) {
                i++;
                continue;
            }
            HeaderEnum headerEnum = HeaderEnum.fromValue(header);
            if (headerEnum == null) {
                i++;
                continue;
            }
            // 根据表头类型处理
            switch (headerEnum) {
                case ID:
                    info.setIdColumnIndex(i);
                    i++;
                    break;
                case TEXT_WATERMARK:
                    info.setTextWatermarkStart(i);
                    i = findRangeEnd(headerRow, i + 1);
                    info.setTextWatermarkEnd(i);
                    i++;
                    break;
                case IMAGE_WATERMARK:
                    info.setImageWatermarkStart(i);
                    i = findRangeEnd(headerRow, i + 1);
                    info.setImageWatermarkEnd(i);
                    i++;
                    break;
                case FILE_PATH:
                    info.setFilePathStart(i);
                    i = findRangeEnd(headerRow, i + 1);
                    info.setFilePathEnd(i);
                    i++;
                    break;
                case RENAME:
                    info.setRenameColumnIndex(i);
                    i++;
                    break;
                case EXTENSION:
                    info.setExtensionColumnIndex(i);
                    i++;
                    break;
            }
        }
        return info;
    }

    /**
     * 查找多列区域的结束位置
     */
    private int findRangeEnd(List<String> headerRow, int startIndex) {
        for (int i = startIndex; i < headerRow.size(); i++) {
            String header = headerRow.get(i).trim();
            if (!header.isEmpty()) {
                return i - 1;
            }
        }
        return headerRow.size() - 1;
    }

    /**
     * 解析数据行
     */
    private List<ExcelParseResultVO.ImageConfigVO> parseDataRows(
            List<List<String>> dataRows,
            HeaderInfo headerInfo,
            MappingModeEnum mode,
            ExcelParseSettingsDTO settings) {
        List<ExcelParseResultVO.ImageConfigVO> configs = new ArrayList<>();
        Map<String, Integer> idToConfigIndex = new HashMap<>();
        DuplicateHandlingEnum duplicateHandling = settings.getDuplicateHandlingEnum();
        InvalidCharHandlingEnum invalidCharHandling = settings.getInvalidCharHandlingEnum();
        int rowNumber = 2;
        for (List<String> row : dataRows) {
            // 1. 确保行数据长度足够
            ensureRowSize(row, headerInfo);
            // 2. 检查是否为空行
            if (isRowEmpty(row, headerInfo)) {
                rowNumber++;
                continue;
            }
            // 3. 提取图片ID
            String imageId = null;
            if (headerInfo.getIdColumnIndex() >= 0 && headerInfo.getIdColumnIndex() < row.size()) {
                imageId = row.get(headerInfo.getIdColumnIndex());
            }
            // 4. ID映射模式下的处理
            if (mode == MappingModeEnum.ID) {
                if (imageId == null || imageId.isEmpty()) {
                    rowNumber++;
                    continue;
                }
                if (idToConfigIndex.containsKey(imageId)) {
                    handleDuplicate(imageId, rowNumber, idToConfigIndex, duplicateHandling, configs);
                }
                idToConfigIndex.put(imageId, configs.size());
            }
            // 5. 提取各区域数据
            List<String> textWatermarks = extractRangeValues(row, headerInfo.getTextWatermarkStart(), headerInfo.getTextWatermarkEnd());
            List<String> imageWatermarks = extractRangeValues(row, headerInfo.getImageWatermarkStart(), headerInfo.getImageWatermarkEnd());
            List<String> filePaths = extractRangeValues(row, headerInfo.getFilePathStart(), headerInfo.getFilePathEnd());
            String rename = extractSingleValue(row, headerInfo.getRenameColumnIndex());
            String extension = extractSingleValue(row, headerInfo.getExtensionColumnIndex());
            // 6. 处理异常字符
            rename = processInvalidChars(rename, invalidCharHandling, ResultCode.EXCEL_INVALID_CHAR_IN_RENAME, rowNumber);
            extension = processExtension(extension, invalidCharHandling, rowNumber);
            filePaths = processFilePaths(filePaths, invalidCharHandling, rowNumber);
            // 7. 构建配置对象
            ExcelParseResultVO.ImageConfigVO config = ExcelParseResultVO.ImageConfigVO.builder()
                    .imageId(imageId)
                    .textWatermarks(textWatermarks)
                    .imageWatermarks(imageWatermarks)
                    .filePaths(filePaths)
                    .rename(rename)
                    .extension(extension)
                    .build();
            configs.add(config);
            rowNumber++;
        }
        return configs;
    }

    /**
     * 确保行数据长度足够
     */
    private void ensureRowSize(List<String> row, HeaderInfo headerInfo) {
        int maxSize = Math.max(
                Math.max(
                        headerInfo.getTextWatermarkEnd() + 1,
                        headerInfo.getImageWatermarkEnd() + 1
                ),
                Math.max(
                        headerInfo.getFilePathEnd() + 1,
                        Math.max(
                                headerInfo.getRenameColumnIndex() + 1,
                                headerInfo.getExtensionColumnIndex() + 1
                        )
                )
        );
        while (row.size() < maxSize) {
            row.add("");
        }
    }

    /**
     * 检查是否为空行
     */
    private boolean isRowEmpty(List<String> row, HeaderInfo headerInfo) {
        boolean hasId = headerInfo.getIdColumnIndex() >= 0 &&
                        headerInfo.getIdColumnIndex() < row.size() &&
                        !row.get(headerInfo.getIdColumnIndex()).isEmpty();
        boolean hasTextWatermark = hasRangeData(row, headerInfo.getTextWatermarkStart(), headerInfo.getTextWatermarkEnd());
        boolean hasImageWatermark = hasRangeData(row, headerInfo.getImageWatermarkStart(), headerInfo.getImageWatermarkEnd());
        boolean hasFilePath = hasRangeData(row, headerInfo.getFilePathStart(), headerInfo.getFilePathEnd());
        boolean hasRename = headerInfo.getRenameColumnIndex() >= 0 &&
                           headerInfo.getRenameColumnIndex() < row.size() &&
                           !row.get(headerInfo.getRenameColumnIndex()).isEmpty();
        boolean hasExtension = headerInfo.getExtensionColumnIndex() >= 0 &&
                              headerInfo.getExtensionColumnIndex() < row.size() &&
                              !row.get(headerInfo.getExtensionColumnIndex()).isEmpty();
        return !hasId && !hasTextWatermark && !hasImageWatermark && !hasFilePath && !hasRename && !hasExtension;
    }

    /**
     * 检查区域是否有数据
     */
    private boolean hasRangeData(List<String> row, int start, int end) {
        if (start < 0) return false;
        for (int i = start; i <= end && i < row.size(); i++) {
            if (!row.get(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理重复ID
     */
    private void handleDuplicate(String imageId, int rowNumber, Map<String, Integer> idToConfigIndex,
                                 DuplicateHandlingEnum handling, List<ExcelParseResultVO.ImageConfigVO> configs) {
        switch (handling) {
            case ERROR:
                log.error("第{}行存在重复的图片ID: {}", rowNumber, imageId);
                throw new BizException(ResultCode.EXCEL_DUPLICATE_ID);
            case LAST:
                int existingIndex = idToConfigIndex.get(imageId);
                configs.set(existingIndex, null);
                log.info("第{}行重复ID '{}' 将覆盖之前的记录", rowNumber, imageId);
                break;
            case FIRST:
            default:
                log.info("第{}行重复ID '{}' 已跳过，保留第一条记录", rowNumber, imageId);
                break;
        }
    }

    /**
     * 提取区域值
     */
    private List<String> extractRangeValues(List<String> row, int start, int end) {
        List<String> values = new ArrayList<>();
        if (start < 0) return values;
        for (int i = start; i <= end && i < row.size(); i++) {
            String value = row.get(i);
            values.add(value != null && !value.isEmpty() ? value : "");
        }
        return values;
    }

    /**
     * 提取单列值
     */
    private String extractSingleValue(List<String> row, int index) {
        if (index < 0 || index >= row.size()) {
            return null;
        }
        String value = row.get(index);
        return (value != null && !value.isEmpty()) ? value : null;
    }

    /**
     * 处理异常字符
     */
    private String processInvalidChars(String value, InvalidCharHandlingEnum handling,
                                       ResultCode errorCode, int rowNumber) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (!INVALID_CHAR_PATTERN.matcher(value).find()) {
            return value;
        }
        switch (handling) {
            case ERROR:
                log.error("第{}行包含非法字符: {}", rowNumber, value);
                throw new BizException(errorCode);
            case ERROR_FOLDER:
                return "ERROR/" + generateErrorId();
            case UNDERSCORE:
            default:
                return INVALID_CHAR_PATTERN.matcher(value).replaceAll("_");
        }
    }

    /**
     * 处理扩展名
     */
    private String processExtension(String extension, InvalidCharHandlingEnum handling, int rowNumber) {
        if (extension == null || extension.isEmpty()) {
            return extension;
        }
        String cleaned = extension.toLowerCase().replaceAll("^\\.+", "");
        if (INVALID_EXTENSION_PATTERN.matcher(cleaned).find()) {
            switch (handling) {
                case ERROR:
                    log.error("第{}行的扩展名包含非法字符: {}", rowNumber, extension);
                    throw new BizException(ResultCode.EXCEL_INVALID_EXTENSION);
                case ERROR_FOLDER:
                    return null;
                case UNDERSCORE:
                default:
                    return INVALID_EXTENSION_PATTERN.matcher(cleaned).replaceAll("");
            }
        }
        return cleaned;
    }

    /**
     * 处理文件路径列表
     */
    private List<String> processFilePaths(List<String> filePaths, InvalidCharHandlingEnum handling, int rowNumber) {
        List<String> processed = new ArrayList<>();
        for (String path : filePaths) {
            if (path == null || path.isEmpty()) {
                processed.add("");
                continue;
            }
            String processedPath = processInvalidChars(path, handling, ResultCode.EXCEL_INVALID_CHAR_IN_PATH, rowNumber);
            processed.add(processedPath);
        }
        return processed;
    }

    /**
     * 生成错误ID
     */
    private String generateErrorId() {
        return "ERROR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 表头信息
     */
    @Data
    private static class HeaderInfo {
        private int idColumnIndex = -1;
        private int textWatermarkStart = -1;
        private int textWatermarkEnd = -1;
        private int imageWatermarkStart = -1;
        private int imageWatermarkEnd = -1;
        private int filePathStart = -1;
        private int filePathEnd = -1;
        private int renameColumnIndex = -1;
        private int extensionColumnIndex = -1;
    }
}
