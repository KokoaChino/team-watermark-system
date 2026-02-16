package com.github.kokoachino.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.mapper.FontMapper;
import com.github.kokoachino.model.dto.FontQueryDTO;
import com.github.kokoachino.model.entity.Font;
import com.github.kokoachino.model.vo.FontVO;
import com.github.kokoachino.service.FontService;
import com.github.kokoachino.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 字体服务实现类
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FontServiceImpl implements FontService {

    private final FontMapper fontMapper;
    private final MinioService minioService;

    @Override
    public List<FontVO> getAvailableFonts(Integer teamId, FontQueryDTO dto) {
        LambdaQueryWrapper<Font> wrapper = new LambdaQueryWrapper<>();
        if (dto != null) {
            if (Boolean.TRUE.equals(dto.getSystemFontOnly())) {
                wrapper.isNull(Font::getTeamId);
            } else if (Boolean.TRUE.equals(dto.getTeamFontOnly())) {
                wrapper.eq(Font::getTeamId, teamId);
            } else {
                wrapper.and(w -> w.isNull(Font::getTeamId).or().eq(Font::getTeamId, teamId));
            }
            if (dto.getName() != null && !dto.getName().isEmpty()) {
                wrapper.like(Font::getName, dto.getName());
            }
        } else {
            wrapper.and(w -> w.isNull(Font::getTeamId).or().eq(Font::getTeamId, teamId));
        }
        wrapper.orderByAsc(Font::getTeamId).orderByAsc(Font::getName);
        List<Font> fonts = fontMapper.selectList(wrapper);
        return fonts.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FontVO uploadFont(Integer teamId, Integer userId, String name, MultipartFile fontFile) {
        String originalFilename = fontFile.getOriginalFilename();
        if (originalFilename == null ||
                !(originalFilename.toLowerCase().endsWith(".ttf") || originalFilename.toLowerCase().endsWith(".otf"))) {
            throw new BizException(ResultCode.FONT_FILE_INVALID);
        }
        if (fontExists(name, teamId)) {
            throw new BizException(ResultCode.FONT_NAME_EXIST);
        }
        try {
            String objectKey = ((MinioServiceImpl) minioService).generateFontObjectKey(teamId, originalFilename);
            String fontUrl = minioService.uploadFile(fontFile, objectKey);
            Font font = new Font();
            font.setName(name);
            font.setFontKey(objectKey);
            font.setTeamId(teamId);
            font.setUploadedBy(userId);
            fontMapper.insert(font);
            return convertToVO(font);
        } catch (Exception e) {
            log.error("上传字体失败", e);
            throw new BizException(ResultCode.FONT_UPLOAD_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFont(Integer fontId, Integer teamId, boolean isLeader) {
        Font font = fontMapper.selectById(fontId);
        if (font == null) {
            throw new BizException(ResultCode.FONT_NOT_FOUND);
        }
        if (font.getTeamId() == null) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (!font.getTeamId().equals(teamId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (!isLeader) {
            throw new BizException(ResultCode.NOT_TEAM_LEADER);
        }
        try {
            minioService.deleteFile(font.getFontKey());
        } catch (Exception e) {
            log.warn("删除MinIO字体文件失败，继续删除数据库记录", e);
        }
        fontMapper.deleteById(fontId);
    }

    @Override
    public boolean fontExists(String name, Integer teamId) {
        Font font = fontMapper.selectByNameAndTeam(name, teamId);
        return font != null;
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
