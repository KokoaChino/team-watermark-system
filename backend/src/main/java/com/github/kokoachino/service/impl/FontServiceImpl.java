package com.github.kokoachino.service.impl;

import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.mapper.FontMapper;
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
    public List<FontVO> getAvailableFonts(Integer teamId) {
        List<Font> fonts = fontMapper.selectAvailableFonts(teamId);
        return fonts.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FontVO uploadFont(Integer teamId, Integer userId, String name, MultipartFile fontFile) {
        // 校验文件格式
        String originalFilename = fontFile.getOriginalFilename();
        if (originalFilename == null || 
                !(originalFilename.toLowerCase().endsWith(".ttf") || originalFilename.toLowerCase().endsWith(".otf"))) {
            throw new BizException(ResultCode.FONT_FILE_INVALID);
        }
        // 检查字体名称是否已存在
        if (fontExists(name, teamId)) {
            throw new BizException(ResultCode.FONT_NAME_EXIST);
        }
        try {
            // 生成ObjectKey并上传
            String objectKey = ((MinioServiceImpl) minioService).generateFontObjectKey(teamId, originalFilename);
            String fontUrl = minioService.uploadFile(fontFile, objectKey);
            // 保存字体记录
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
        // 系统字体不能删除
        if (font.getTeamId() == null) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        // 只能删除自己团队的字体
        if (!font.getTeamId().equals(teamId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        // 只有队长可以删除
        if (!isLeader) {
            throw new BizException(ResultCode.NOT_TEAM_LEADER);
        }
        // 删除MinIO文件
        try {
            minioService.deleteFile(font.getFontKey());
        } catch (Exception e) {
            log.warn("删除MinIO字体文件失败，继续删除数据库记录", e);
        }
        // 删除数据库记录
        fontMapper.deleteById(fontId);
    }

    @Override
    public boolean fontExists(String name, Integer teamId) {
        Font font = fontMapper.selectByNameAndTeam(name, teamId);
        return font != null;
    }

    /**
     * 转换为VO
     */
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
