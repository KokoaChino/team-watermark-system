package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.BatchTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


/**
 * 批量任务 Mapper 接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Mapper
public interface BatchTaskMapper extends BaseMapper<BatchTask> {

    /**
     * 查询用户是否有未完成的任务
     */
    @Select("SELECT COUNT(*) FROM tw_batch_task WHERE created_by_id = #{userId} AND completed_at IS NULL")
    int countUncompletedByUserId(@Param("userId") Integer userId);
}
