package com.github.kokoachino.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kokoachino.common.enums.LockActionEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.LockUtils;
import com.github.kokoachino.config.SystemProperties;
import com.github.kokoachino.mapper.UserMapper;
import com.github.kokoachino.mapper.WatermarkTemplateDraftMapper;
import com.github.kokoachino.mapper.WatermarkTemplateMapper;
import com.github.kokoachino.model.dto.SaveDraftDTO;
import com.github.kokoachino.model.dto.SubmitDraftDTO;
import com.github.kokoachino.model.dto.BaseConfigDTO;
import com.github.kokoachino.model.dto.WatermarkConfigDTO;
import com.github.kokoachino.model.entity.User;
import com.github.kokoachino.model.entity.WatermarkTemplate;
import com.github.kokoachino.model.entity.WatermarkTemplateDraft;
import com.github.kokoachino.model.vo.DraftVO;
import com.github.kokoachino.model.vo.WatermarkTemplateVO;
import com.github.kokoachino.common.enums.EventTypeEnum;
import com.github.kokoachino.service.OperationLogService;
import com.github.kokoachino.service.WatermarkTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 水印模板服务实现类
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatermarkTemplateServiceImpl extends ServiceImpl<WatermarkTemplateMapper, WatermarkTemplate>
        implements WatermarkTemplateService {

    private final WatermarkTemplateMapper templateMapper;
    private final WatermarkTemplateDraftMapper draftMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final LockUtils lockUtils;
    private final OperationLogService operationLogService;
    private final SystemProperties systemProperties;

    @Override
    public List<WatermarkTemplateVO> getTemplateList(Integer teamId) {
        List<WatermarkTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<WatermarkTemplate>()
                        .eq(WatermarkTemplate::getTeamId, teamId)
                        .orderByDesc(WatermarkTemplate::getCreatedAt)
        );
        return templates.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Integer templateId, Integer userId, Integer teamId, boolean isLeader) {
        WatermarkTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        if (!template.getCreatedById().equals(userId) && !isLeader) {
            throw new BizException(ResultCode.NOT_TEMPLATE_CREATOR);
        }
        String templateName = template.getName();
        templateMapper.deleteById(templateId);
        operationLogService.log(EventTypeEnum.TEMPLATE_DELETE, templateId, templateName,
                Map.of("deletedBy", isLeader ? "leader" : "creator"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DraftVO createDraftFromTemplate(Integer templateId, Integer userId) {
        WatermarkTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        deleteExistingDraft(userId);
        WatermarkTemplateDraft draft = new WatermarkTemplateDraft();
        draft.setUserId(userId);
        draft.setSourceTemplateId(templateId);
        draft.setSourceVersion(template.getVersion());
        draft.setName(template.getName());
        draft.setConfig(template.getConfig());
        draftMapper.insert(draft);
        return convertToDraftVO(draft, false, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DraftVO createEmptyDraft(Integer userId) {
        deleteExistingDraft(userId);
        WatermarkConfigDTO defaultConfig = createDefaultConfig();
        WatermarkTemplateDraft draft = new WatermarkTemplateDraft();
        draft.setUserId(userId);
        draft.setName(systemProperties.getTemplate().getDefaultName());
        draft.setConfig(convertConfigToJson(defaultConfig));
        draftMapper.insert(draft);
        return convertToDraftVO(draft, false, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DraftVO saveDraft(Integer userId, SaveDraftDTO dto) {
        return lockUtils.executeWithLock(
                LockUtils.getLockKey(LockActionEnum.DRAFT_SAVE, userId),
                () -> {
                    WatermarkTemplateDraft draft = draftMapper.selectByUserId(userId);
                    if (draft == null) {
                        draft = new WatermarkTemplateDraft();
                        draft.setUserId(userId);
                    }
                    draft.setSourceTemplateId(dto.getSourceTemplateId());
                    draft.setSourceVersion(dto.getSourceVersion());
                    draft.setName(dto.getName());
                    draft.setConfig(convertConfigToJson(dto.getConfig()));
                    if (draft.getId() == null) {
                        draftMapper.insert(draft);
                    } else {
                        draftMapper.updateById(draft);
                    }
                    boolean hasConflict = checkConflict(draft);
                    String conflictMessage = hasConflict ? "该模板已被他人修改或删除" : null;
                    return convertToDraftVO(draft, hasConflict, conflictMessage);
                }
        );
    }

    @Override
    public DraftVO getCurrentDraft(Integer userId) {
        WatermarkTemplateDraft draft = draftMapper.selectByUserId(userId);
        if (draft == null) {
            return null;
        }
        boolean hasConflict = checkConflict(draft);
        String conflictMessage = hasConflict ? "该模板已被他人修改或删除" : null;
        return convertToDraftVO(draft, hasConflict, conflictMessage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WatermarkTemplateVO submitDraft(Integer userId, String username, Integer teamId, SubmitDraftDTO dto) {
        return lockUtils.executeWithLock(
                LockUtils.getLockKey(LockActionEnum.TEMPLATE_SUBMIT, userId),
                () -> {
                    WatermarkTemplateDraft draft = draftMapper.selectByUserId(userId);
                    if (draft == null) {
                        throw new BizException(ResultCode.NO_WORKING_DRAFT);
                    }
                    WatermarkConfigDTO config = parseConfig(draft.getConfig());
                    if (draft.getSourceTemplateId() == null || Boolean.TRUE.equals(dto.getForceCreateNew())) {
                        WatermarkTemplateVO result = doCreateTemplate(teamId, userId, draft.getName(), config);
                        clearDraft(userId);
                        return result;
                    } else {
                        WatermarkTemplate sourceTemplate = templateMapper.selectById(draft.getSourceTemplateId());
                        if (sourceTemplate == null) {
                            throw new BizException(ResultCode.TEMPLATE_DELETED);
                        }
                        if (!sourceTemplate.getVersion().equals(draft.getSourceVersion())) {
                            throw new BizException(ResultCode.TEMPLATE_VERSION_CONFLICT);
                        }
                        WatermarkTemplateVO result = doUpdateTemplate(draft.getSourceTemplateId(), draft.getName(), config, draft.getSourceVersion());
                        clearDraft(userId);
                        return result;
                    }
                }
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearDraft(Integer userId) {
        WatermarkTemplateDraft draft = draftMapper.selectByUserId(userId);
        if (draft != null) {
            draftMapper.deleteById(draft.getId());
        }
    }

    private WatermarkTemplateVO doCreateTemplate(Integer teamId, Integer userId, String name, WatermarkConfigDTO config) {
        checkTemplateNameExists(teamId, name, null);
        WatermarkTemplate template = new WatermarkTemplate();
        template.setTeamId(teamId);
        template.setName(name);
        template.setConfig(convertConfigToJson(config));
        template.setCreatedById(userId);
        templateMapper.insert(template);
        operationLogService.log(EventTypeEnum.TEMPLATE_CREATE, template.getId(), name,
                Map.of("config", config));
        return convertToVO(template);
    }

    private WatermarkTemplateVO doUpdateTemplate(Integer templateId, String name, WatermarkConfigDTO config, Integer version) {
        WatermarkTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        checkTemplateNameExists(template.getTeamId(), name, templateId);
        if (!version.equals(template.getVersion())) {
            throw new BizException(ResultCode.TEMPLATE_VERSION_CONFLICT);
        }
        template.setName(name);
        template.setConfig(convertConfigToJson(config));
        int affected = templateMapper.updateById(template);
        if (affected == 0) {
            throw new BizException(ResultCode.TEMPLATE_VERSION_CONFLICT);
        }
        operationLogService.log(EventTypeEnum.TEMPLATE_UPDATE, templateId, name,
                Map.of("beforeVersion", version, "afterVersion", template.getVersion()));
        return convertToVO(template);
    }

    private void checkTemplateNameExists(Integer teamId, String name, Integer excludeId) {
        LambdaQueryWrapper<WatermarkTemplate> wrapper = new LambdaQueryWrapper<WatermarkTemplate>()
                .eq(WatermarkTemplate::getTeamId, teamId)
                .eq(WatermarkTemplate::getName, name);
        if (excludeId != null) {
            wrapper.ne(WatermarkTemplate::getId, excludeId);
        }
        long count = templateMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BizException(ResultCode.TEMPLATE_NAME_EXIST);
        }
    }

    private void deleteExistingDraft(Integer userId) {
        WatermarkTemplateDraft existingDraft = draftMapper.selectByUserId(userId);
        if (existingDraft != null) {
            draftMapper.deleteById(existingDraft.getId());
        }
    }

    private boolean checkConflict(WatermarkTemplateDraft draft) {
        if (draft.getSourceTemplateId() == null) {
            return false;
        }
        WatermarkTemplate sourceTemplate = templateMapper.selectById(draft.getSourceTemplateId());
        if (sourceTemplate == null) {
            return true;
        }
        return !sourceTemplate.getVersion().equals(draft.getSourceVersion());
    }

    private WatermarkConfigDTO createDefaultConfig() {
        SystemProperties.TemplateConfig templateConfig = systemProperties.getTemplate();
        WatermarkConfigDTO config = new WatermarkConfigDTO();
        BaseConfigDTO baseConfig = new BaseConfigDTO();
        baseConfig.setWidth(templateConfig.getDefaultWidth());
        baseConfig.setHeight(templateConfig.getDefaultHeight());
        baseConfig.setBackgroundColor(templateConfig.getDefaultBackgroundColor());
        config.setBaseConfig(baseConfig);
        return config;
    }

    private String convertConfigToJson(WatermarkConfigDTO config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("转换水印配置为JSON失败", e);
            throw new RuntimeException("配置格式错误", e);
        }
    }

    private WatermarkConfigDTO parseConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, WatermarkConfigDTO.class);
        } catch (JsonProcessingException e) {
            log.error("解析水印配置JSON失败", e);
            throw new RuntimeException("配置格式错误", e);
        }
    }

    private WatermarkTemplateVO convertToVO(WatermarkTemplate template) {
        User creator = userMapper.selectById(template.getCreatedById());
        return WatermarkTemplateVO.builder()
                .id(template.getId())
                .name(template.getName())
                .teamId(template.getTeamId())
                .config(parseConfig(template.getConfig()))
                .createdById(template.getCreatedById())
                .createdByUsername(creator != null ? creator.getUsername() : null)
                .version(template.getVersion())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private DraftVO convertToDraftVO(WatermarkTemplateDraft draft, boolean hasConflict, String conflictMessage) {
        return DraftVO.builder()
                .id(draft.getId())
                .sourceTemplateId(draft.getSourceTemplateId())
                .sourceVersion(draft.getSourceVersion())
                .name(draft.getName())
                .config(parseConfig(draft.getConfig()))
                .updatedAt(draft.getUpdatedAt())
                .hasConflict(hasConflict)
                .conflictMessage(conflictMessage)
                .build();
    }
}
