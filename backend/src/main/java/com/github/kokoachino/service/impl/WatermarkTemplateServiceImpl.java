package com.github.kokoachino.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kokoachino.common.enums.LockActionEnum;
import com.github.kokoachino.common.enums.LogUserStatusEnum;
import com.github.kokoachino.common.enums.WatermarkResourceEventTypeEnum;
import com.github.kokoachino.common.enums.WatermarkResourceScopeEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.LockUtils;
import com.github.kokoachino.config.SystemProperties;
import com.github.kokoachino.mapper.UserMapper;
import com.github.kokoachino.mapper.WatermarkTemplateDraftMapper;
import com.github.kokoachino.mapper.WatermarkTemplateMapper;
import com.github.kokoachino.model.dto.BaseConfigDTO;
import com.github.kokoachino.model.dto.SaveDraftDTO;
import com.github.kokoachino.model.dto.SubmitDraftDTO;
import com.github.kokoachino.model.dto.WatermarkConfigDTO;
import com.github.kokoachino.model.dto.WatermarkResourceLogRecordDTO;
import com.github.kokoachino.model.entity.User;
import com.github.kokoachino.model.entity.WatermarkTemplate;
import com.github.kokoachino.model.entity.WatermarkTemplateDraft;
import com.github.kokoachino.model.vo.DraftVO;
import com.github.kokoachino.model.vo.WatermarkTemplateVO;
import com.github.kokoachino.service.WatermarkResourceLogService;
import com.github.kokoachino.service.WatermarkTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 水印模板服务实现类
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatermarkTemplateServiceImpl extends ServiceImpl<WatermarkTemplateMapper, WatermarkTemplate>
        implements WatermarkTemplateService {

    private static final int DRAFT_DEFAULT_STATE = -1;

    private final WatermarkTemplateMapper templateMapper;
    private final WatermarkTemplateDraftMapper draftMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final LockUtils lockUtils;
    private final WatermarkResourceLogService watermarkResourceLogService;
    private final SystemProperties systemProperties;

    @Override
    public List<WatermarkTemplateVO> getTemplateList(Integer teamId) {
        return templateMapper.selectList(new LambdaQueryWrapper<WatermarkTemplate>()
                        .eq(WatermarkTemplate::getTeamId, teamId)
                        .orderByDesc(WatermarkTemplate::getCreatedAt))
                .stream()
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
        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("templateName", template.getName());
        beforeData.put("version", template.getVersion());
        beforeData.put("config", parseConfig(template.getConfig()));
        templateMapper.deleteById(templateId);
        watermarkResourceLogService.record(WatermarkResourceLogRecordDTO.builder()
                .teamId(teamId)
                .resourceScope(WatermarkResourceScopeEnum.TEMPLATE)
                .eventType(WatermarkResourceEventTypeEnum.TEMPLATE_DELETE)
                .operatorUserId(userId)
                .operatorUsername(resolveUsername(userId))
                .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .resourceId(templateId)
                .resourceName(template.getName())
                .beforeData(beforeData)
                .details(Map.of("deletedByLeader", isLeader))
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DraftVO createDraftFromTemplate(Integer templateId, Integer userId, boolean force) {
        WatermarkTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        WatermarkTemplateDraft existingDraft = draftMapper.selectByUserId(userId);
        if (!force && existingDraft != null && !isDraftInDefaultState(existingDraft)) {
            if (templateId.equals(existingDraft.getSourceTemplateId())) {
                return convertToDraftVO(existingDraft, false, null);
            }
            return convertToDraftVO(existingDraft, true, "当前存在未提交的草稿「" + existingDraft.getName() + "」，继续将覆盖之前的编辑内容");
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
    public DraftVO createEmptyDraft(Integer userId, boolean force) {
        WatermarkTemplateDraft existingDraft = draftMapper.selectByUserId(userId);
        if (!force && existingDraft != null && !isDraftInDefaultState(existingDraft)) {
            return convertToDraftVO(existingDraft, true, "当前存在未提交的草稿「" + existingDraft.getName() + "」，继续将覆盖之前的编辑内容");
        }
        deleteExistingDraft(userId);
        WatermarkConfigDTO defaultConfig = createDefaultConfig();
        WatermarkTemplateDraft draft = new WatermarkTemplateDraft();
        draft.setUserId(userId);
        draft.setName(systemProperties.getTemplate().getDefaultName());
        draft.setSourceVersion(DRAFT_DEFAULT_STATE);
        draft.setConfig(convertConfigToJson(defaultConfig));
        draftMapper.insert(draft);
        return convertToDraftVO(draft, false, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DraftVO saveDraft(Integer userId, SaveDraftDTO dto) {
        return lockUtils.executeWithLock(LockUtils.getLockKey(LockActionEnum.DRAFT_SAVE, userId), () -> {
            WatermarkTemplateDraft draft = draftMapper.selectByUserId(userId);
            if (draft == null) {
                draft = new WatermarkTemplateDraft();
                draft.setUserId(userId);
            }
            draft.setSourceTemplateId(dto.getSourceTemplateId());
            draft.setSourceVersion(dto.getSourceVersion());
            draft.setName(dto.getName());
            draft.setConfig(convertConfigToJson(dto.getConfig()));
            if (draft.getSourceVersion() != null && draft.getSourceVersion().equals(DRAFT_DEFAULT_STATE)) {
                draft.setSourceVersion(0);
            }
            if (draft.getId() == null) {
                draftMapper.insert(draft);
            } else {
                draftMapper.updateById(draft);
            }
            return convertToDraftVO(draft, false, null);
        });
    }

    @Override
    public DraftVO getCurrentDraft(Integer userId) {
        WatermarkTemplateDraft draft = draftMapper.selectByUserId(userId);
        if (draft == null) {
            return null;
        }
        return convertToDraftVO(draft, false, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WatermarkTemplateVO submitDraft(Integer userId, String username, Integer teamId, SubmitDraftDTO dto) {
        return lockUtils.executeWithLock(LockUtils.getLockKey(LockActionEnum.TEMPLATE_SUBMIT, userId), () -> {
            WatermarkTemplateDraft draft = draftMapper.selectByUserId(userId);
            if (draft == null) {
                throw new BizException(ResultCode.NO_WORKING_DRAFT);
            }
            WatermarkConfigDTO config = parseConfig(draft.getConfig());
            WatermarkTemplateVO result;
            if (draft.getSourceTemplateId() == null || Boolean.TRUE.equals(dto.getForceCreateNew())) {
                result = doCreateTemplate(teamId, userId, username, draft.getName(), config);
            } else {
                WatermarkTemplate sourceTemplate = templateMapper.selectById(draft.getSourceTemplateId());
                if (sourceTemplate == null) {
                    throw new BizException(ResultCode.TEMPLATE_DELETED);
                }
                if (!sourceTemplate.getVersion().equals(draft.getSourceVersion())) {
                    throw new BizException(ResultCode.TEMPLATE_VERSION_CONFLICT);
                }
                result = doUpdateTemplate(draft.getSourceTemplateId(), userId, username, draft.getName(), config, draft.getSourceVersion());
            }
            deleteExistingDraft(userId);
            return result;
        });
    }

    private boolean isDraftInDefaultState(WatermarkTemplateDraft draft) {
        return draft != null && draft.getSourceVersion() != null && draft.getSourceVersion().equals(DRAFT_DEFAULT_STATE);
    }

    private WatermarkTemplateVO doCreateTemplate(Integer teamId, Integer userId, String username, String name, WatermarkConfigDTO config) {
        WatermarkTemplate template = new WatermarkTemplate();
        template.setTeamId(teamId);
        template.setName(name);
        template.setConfig(convertConfigToJson(config));
        template.setCreatedById(userId);
        templateMapper.insert(template);
        Map<String, Object> afterData = new HashMap<>();
        afterData.put("templateName", template.getName());
        afterData.put("version", template.getVersion());
        afterData.put("config", config);
        watermarkResourceLogService.record(WatermarkResourceLogRecordDTO.builder()
                .teamId(teamId)
                .resourceScope(WatermarkResourceScopeEnum.TEMPLATE)
                .eventType(WatermarkResourceEventTypeEnum.TEMPLATE_CREATE)
                .operatorUserId(userId)
                .operatorUsername(username)
                .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .resourceId(template.getId())
                .resourceName(name)
                .afterData(afterData)
                .build());
        return convertToVO(template);
    }

    private WatermarkTemplateVO doUpdateTemplate(Integer templateId, Integer userId, String username, String name, WatermarkConfigDTO config, Integer version) {
        WatermarkTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException(ResultCode.TEMPLATE_NOT_FOUND);
        }
        if (!version.equals(template.getVersion())) {
            throw new BizException(ResultCode.TEMPLATE_VERSION_CONFLICT);
        }
        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("templateName", template.getName());
        beforeData.put("version", template.getVersion());
        beforeData.put("config", parseConfig(template.getConfig()));
        template.setName(name);
        template.setConfig(convertConfigToJson(config));
        template.setUpdatedAt(LocalDateTime.now());
        int affected = templateMapper.updateById(template);
        if (affected == 0) {
            throw new BizException(ResultCode.TEMPLATE_VERSION_CONFLICT);
        }
        Map<String, Object> afterData = new HashMap<>();
        afterData.put("templateName", template.getName());
        afterData.put("version", template.getVersion());
        afterData.put("config", config);
        watermarkResourceLogService.record(WatermarkResourceLogRecordDTO.builder()
                .teamId(template.getTeamId())
                .resourceScope(WatermarkResourceScopeEnum.TEMPLATE)
                .eventType(WatermarkResourceEventTypeEnum.TEMPLATE_UPDATE)
                .operatorUserId(userId)
                .operatorUsername(username)
                .operatorUserStatus(LogUserStatusEnum.ACTIVE.getValue())
                .resourceId(templateId)
                .resourceName(name)
                .beforeData(beforeData)
                .afterData(afterData)
                .build());
        return convertToVO(template);
    }

    private void deleteExistingDraft(Integer userId) {
        WatermarkTemplateDraft existingDraft = draftMapper.selectByUserId(userId);
        if (existingDraft != null) {
            draftMapper.deleteById(existingDraft.getId());
        }
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

    private String resolveUsername(Integer userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : null;
    }
}
