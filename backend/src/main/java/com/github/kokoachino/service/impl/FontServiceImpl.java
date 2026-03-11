package com.github.kokoachino.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.kokoachino.common.enums.LogUserStatusEnum;
import com.github.kokoachino.common.enums.WatermarkResourceEventTypeEnum;
import com.github.kokoachino.common.enums.WatermarkResourceScopeEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.mapper.FontMapper;
import com.github.kokoachino.model.dto.WatermarkResourceLogRecordDTO;
import com.github.kokoachino.model.entity.Font;
import com.github.kokoachino.model.vo.FontVO;
import com.github.kokoachino.service.FontService;
import com.github.kokoachino.service.MinioService;
import com.github.kokoachino.service.WatermarkResourceLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 字体服务实现类
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FontServiceImpl implements FontService {

    private final FontMapper fontMapper;
    private final MinioService minioService;
    private final WatermarkResourceLogService watermarkResourceLogService;

    @Override
    public List<FontVO> getAvailableFonts(Integer teamId, String name) {
        LambdaQueryWrapper<Font> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(Font::getTeamId).or().eq(Font::getTeamId, teamId));
        if (name != null && !name.isEmpty()) {
            wrapper.like(Font::getName, name);
        }
        wrapper.orderByAsc(Font::getTeamId).orderByAsc(Font::getName);
        return fontMapper.selectList(wrapper).stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FontVO uploadFont(Integer teamId, Integer userId, String username, String name, MultipartFile fontFile) {
        String originalFilename = fontFile.getOriginalFilename();
        if (originalFilename == null || !(originalFilename.toLowerCase().endsWith(".ttf") || originalFilename.toLowerCase().endsWith(".otf"))) {
            throw new BizException(ResultCode.FONT_FILE_INVALID);
        }
        if (fontExists(name, teamId)) {
            throw new BizException(ResultCode.FONT_NAME_EXIST);
        }
        try {
            String objectKey = ((MinioServiceImpl) minioService).generateFontObjectKey(teamId, originalFilename);
            minioService.uploadFile(fontFile, objectKey);
            Font font = new Font();
            font.setName(name);
            font.setFontKey(objectKey);
            font.setTeamId(teamId);
            font.setUploadedBy(userId);
            fontMapper.insert(font);
            Map<String, Object> afterData = new HashMap<>();
            afterData.put("fontName", font.getName());
            afterData.put("fontKey", font.getFontKey());
            watermarkResourceLogService.record(WatermarkResourceLogRecordDTO.builder()
                    .teamId(teamId)
                    .resourceScope(WatermarkResourceScopeEnum.FONT)
                    .eventType(WatermarkResourceEventTypeEnum.FONT_UPLOAD)
                    .operatorUserId(userId)
                    .operatorUsername(username)
                    .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                    .resourceId(font.getId())
                    .resourceName(font.getName())
                    .afterData(afterData)
                    .build());
            return convertToVO(font);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传字体失败", e);
            throw new BizException(ResultCode.FONT_UPLOAD_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFont(Integer fontId, Integer teamId, Integer operatorUserId, String operatorUsername, boolean isLeader) {
        Font font = fontMapper.selectById(fontId);
        if (font == null) {
            throw new BizException(ResultCode.FONT_NOT_FOUND);
        }
        if (font.getTeamId() == null || !font.getTeamId().equals(teamId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (!isLeader) {
            throw new BizException(ResultCode.NOT_TEAM_LEADER);
        }
        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("fontName", font.getName());
        beforeData.put("fontKey", font.getFontKey());
        try {
            minioService.deleteFile(font.getFontKey());
        } catch (Exception e) {
            log.warn("删除MinIO字体文件失败，继续删除数据库记录", e);
        }
        fontMapper.deleteById(fontId);
        watermarkResourceLogService.record(WatermarkResourceLogRecordDTO.builder()
                .teamId(teamId)
                .resourceScope(WatermarkResourceScopeEnum.FONT)
                .eventType(WatermarkResourceEventTypeEnum.FONT_DELETE)
                .operatorUserId(operatorUserId)
                .operatorUsername(operatorUsername)
                .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .resourceId(fontId)
                .resourceName(font.getName())
                .beforeData(beforeData)
                .build());
    }

    @Override
    public boolean fontExists(String name, Integer teamId) {
        return fontMapper.selectByNameAndTeam(name, teamId) != null;
    }

    private FontVO convertToVO(Font font) {
        return FontVO.builder()
                .id(font.getId())
                .name(font.getName())
                .fontUrl(minioService.getFileUrl(font.getFontKey()))
                .isSystemFont(font.getTeamId() == null)
                .teamId(font.getTeamId())
                .uploadedBy(font.getUploadedBy())
                .createdAt(font.getCreatedAt())
                .build();
    }
}
