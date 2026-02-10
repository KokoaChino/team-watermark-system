package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.PointTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 点数流水 Mapper 接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Mapper
public interface PointTransactionMapper extends BaseMapper<PointTransaction> {

    /**
     * 查询团队的点数流水
     */
    List<PointTransaction> selectByTeamId(@Param("teamId") Integer teamId, @Param("limit") Integer limit);
}
