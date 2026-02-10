package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.WatermarkTemplateDraft;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 水印模板草稿 Mapper 接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Mapper
public interface WatermarkTemplateDraftMapper extends BaseMapper<WatermarkTemplateDraft> {

    /**
     * 根据用户ID查询草稿
     *
     * @param userId 用户ID
     * @return 草稿
     */
    WatermarkTemplateDraft selectByUserId(@Param("userId") Integer userId);
}
