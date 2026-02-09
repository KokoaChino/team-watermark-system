package com.github.kokoachino.service;

import com.github.kokoachino.model.dto.CreateTemplateDTO;
import com.github.kokoachino.model.dto.SaveDraftDTO;
import com.github.kokoachino.model.dto.SubmitDraftDTO;
import com.github.kokoachino.model.dto.UpdateTemplateDTO;
import com.github.kokoachino.model.vo.DraftVO;
import com.github.kokoachino.model.vo.WatermarkTemplateVO;
import java.util.List;


/**
 * 水印模板服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
public interface WatermarkTemplateService {

    /**
     * 获取团队模板列表
     *
     * @param teamId 团队ID
     * @return 模板列表
     */
    List<WatermarkTemplateVO> getTemplateList(Integer teamId);

    /**
     * 获取模板详情
     *
     * @param templateId 模板ID
     * @return 模板详情
     */
    WatermarkTemplateVO getTemplateDetail(Integer templateId);

    /**
     * 创建模板（直接创建，不走草稿）
     *
     * @param teamId     团队ID
     * @param userId     用户ID
     * @param username   用户名
     * @param dto        创建参数
     * @return 创建的模板
     */
    WatermarkTemplateVO createTemplate(Integer teamId, Integer userId, String username, CreateTemplateDTO dto);

    /**
     * 更新模板
     *
     * @param templateId 模板ID
     * @param dto        更新参数
     * @return 更新后的模板
     */
    WatermarkTemplateVO updateTemplate(Integer templateId, UpdateTemplateDTO dto);

    /**
     * 删除模板
     *
     * @param templateId 模板ID
     * @param userId     用户ID
     * @param teamId     团队ID
     * @param isLeader   是否是队长
     */
    void deleteTemplate(Integer templateId, Integer userId, Integer teamId, boolean isLeader);

    /**
     * 基于现有模板创建工作区草稿
     *
     * @param templateId 源模板ID
     * @param userId     用户ID
     * @return 草稿信息
     */
    DraftVO createDraftFromTemplate(Integer templateId, Integer userId);

    /**
     * 创建空的工作区草稿
     *
     * @param userId 用户ID
     * @return 草稿信息
     */
    DraftVO createEmptyDraft(Integer userId);

    /**
     * 保存草稿
     *
     * @param userId   用户ID
     * @param dto      保存参数
     * @return 草稿信息
     */
    DraftVO saveDraft(Integer userId, SaveDraftDTO dto);

    /**
     * 获取当前用户的草稿
     *
     * @param userId 用户ID
     * @return 草稿信息
     */
    DraftVO getCurrentDraft(Integer userId);

    /**
     * 提交草稿
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param teamId   团队ID
     * @param dto      提交参数
     * @return 提交后的模板
     */
    WatermarkTemplateVO submitDraft(Integer userId, String username, Integer teamId, SubmitDraftDTO dto);

    /**
     * 清空草稿
     *
     * @param userId 用户ID
     */
    void clearDraft(Integer userId);
}
