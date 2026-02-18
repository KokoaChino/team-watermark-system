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

    private String currentErrorFolderId;

    @Override
    public ExcelParseResultVO parseExcel(MultipartFile excelFile, String mappingMode, ExcelParseSettingsDTO settings) {
        if (settings == null) {
            settings = new ExcelParseSettingsDTO();
        }
        currentErrorFolderId = null;
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
            }).sheet(0).headRowNumber(0).doRead();
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
        Map<HeaderEnum, Integer> headerCount = new HashMap<>();
        int colCount = headerRow.size();
        int i = 0;
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
            headerCount.merge(headerEnum, 1, Integer::sum);
            if (headerCount.get(headerEnum) > 1) {
                throw new BizException(ResultCode.EXCEL_DUPLICATE_HEADER, 
                    String.format("表头 '%s' 重复出现（第%d列）", headerEnum.getValue(), i + 1));
            }
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
            // 6. 统一处理文件路径、重命名、拓展名的异常字符
            ProcessResult processResult = processFilePathRenameExtension(row, headerInfo, invalidCharHandling, rowNumber);
            // 7. 构建配置对象
            ExcelParseResultVO.ImageConfigVO config = ExcelParseResultVO.ImageConfigVO.builder()
                    .imageId(imageId)
                    .textWatermarks(textWatermarks)
                    .imageWatermarks(imageWatermarks)
                    .filePaths(processResult.filePaths)
                    .rename(processResult.rename)
                    .extension(processResult.extension)
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
                throw new BizException(ResultCode.EXCEL_DUPLICATE_ID, String.format("第%s行存在重复的图片ID：%s", rowNumber, imageId));
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
            if (value != null && !value.isEmpty()) {
                values.add(value);
            }
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
     * 统一处理文件路径、重命名、拓展名的异常字符
     */
    private ProcessResult processFilePathRenameExtension(List<String> row, HeaderInfo headerInfo,
                                                         InvalidCharHandlingEnum handling, int rowNumber) {
        List<String> filePaths = extractRangeValues(row, headerInfo.getFilePathStart(), headerInfo.getFilePathEnd());
        String rename = extractSingleValue(row, headerInfo.getRenameColumnIndex());
        String extension = extractSingleValue(row, headerInfo.getExtensionColumnIndex());
        ProcessResult result = new ProcessResult();
        result.filePaths = new ArrayList<>(filePaths);
        result.rename = rename;
        result.extension = extension;
        boolean anyInvalid = false;
        boolean pathInvalid = false;
        boolean renameInvalid = false;
        boolean extensionInvalid = false;
        int invalidPathCol = -1;
        String invalidPathValue = null;
        for (int i = 0; i < result.filePaths.size(); i++) {
            String path = result.filePaths.get(i);
            if (path != null && !path.isEmpty() && INVALID_CHAR_PATTERN.matcher(path).find()) {
                pathInvalid = anyInvalid = true;
                invalidPathCol = headerInfo.getFilePathStart() + i;
                invalidPathValue = path;
                break;
            }
        }
        if (!pathInvalid && rename != null && !rename.isEmpty() && INVALID_CHAR_PATTERN.matcher(rename).find()) {
            renameInvalid = anyInvalid = true;
        }
        if (!pathInvalid && !renameInvalid && extension != null && !extension.isEmpty()) {
            String cleaned = extension.trim().toLowerCase();
            if (cleaned.startsWith(".")) {
                cleaned = cleaned.substring(1);
            }
            if (cleaned.isEmpty() || cleaned.contains(".") || INVALID_EXTENSION_PATTERN.matcher(cleaned).find()) {
                extensionInvalid = true;
                anyInvalid = true;
            } else {
                result.extension = cleaned;
            }
        }
        switch (handling) {
            case ERROR -> {
                if (pathInvalid) {
                    throw new BizException(ResultCode.EXCEL_INVALID_CHAR_IN_PATH, 
                        String.format("第%d行第%d列 '%s' 包含非法字符", rowNumber, invalidPathCol + 1, invalidPathValue));
                }
                if (renameInvalid) {
                    throw new BizException(ResultCode.EXCEL_INVALID_CHAR_IN_RENAME, 
                        String.format("第%d行第%d列 '%s' 包含非法字符", rowNumber, headerInfo.getRenameColumnIndex() + 1, rename));
                }
                if (extensionInvalid) {
                    throw new BizException(ResultCode.EXCEL_INVALID_EXTENSION, 
                        String.format("第%d行第%d列 '%s' 包含非法字符", rowNumber, headerInfo.getExtensionColumnIndex() + 1, extension));
                }
            }
            case ERROR_FOLDER -> {
                if (anyInvalid) {
                    result.filePaths = List.of(generateErrorId());
                    result.rename = renameInvalid ? null : rename;
                    result.extension = extensionInvalid ? null : extension;
                }
            }
            case UNDERSCORE -> {
                List<String> processedPaths = new ArrayList<>();
                for (String path : result.filePaths) {
                    if (path != null && !path.isEmpty()) {
                        processedPaths.add(INVALID_CHAR_PATTERN.matcher(path).replaceAll("_"));
                    } else {
                        processedPaths.add(path);
                    }
                }
                result.filePaths = processedPaths;
                if (renameInvalid && result.rename != null) {
                    result.rename = INVALID_CHAR_PATTERN.matcher(result.rename).replaceAll("_");
                }
                if (extensionInvalid && result.extension != null) {
                    String cleaned = result.extension.trim().toLowerCase();
                    if (cleaned.startsWith(".")) {
                        cleaned = cleaned.substring(1);
                    }
                    if (cleaned.contains(".")) {
                        cleaned = cleaned.replace(".", "");
                    }
                    result.extension = INVALID_EXTENSION_PATTERN.matcher(cleaned).replaceAll("");
                }
            }
        }
        if (result.extension != null) {
            String cleaned = result.extension.trim().toLowerCase();
            if (cleaned.startsWith(".")) {
                cleaned = cleaned.substring(1);
            }
            result.extension = cleaned;
        }
        return result;
    }

    /**
     * 生成错误ID（单次解析中唯一）
     */
    private String generateErrorId() {
        if (currentErrorFolderId == null) {
            currentErrorFolderId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return "ERROR-" + currentErrorFolderId;
    }

    /**
     * 处理结果
     */
    @Data
    private static class ProcessResult {
        private List<String> filePaths;
        private String rename;
        private String extension;
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
