package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.Font;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;


/**
 * 字体 Mapper 接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Mapper
public interface FontMapper extends BaseMapper<Font> {

    /**
     * 查询系统字体和指定团队的字体
     *
     * @param teamId 团队ID
     * @return 字体列表
     */
    @Select("SELECT * FROM tw_font WHERE team_id IS NULL OR team_id = #{teamId} ORDER BY created_at DESC")
    List<Font> selectAvailableFonts(@Param("teamId") Integer teamId);

    /**
     * 根据名称和团队查询字体
     *
     * @param name   字体名称
     * @param teamId 团队ID
     * @return 字体
     */
    @Select("SELECT * FROM tw_font WHERE name = #{name} AND (team_id IS NULL OR team_id = #{teamId}) LIMIT 1")
    Font selectByNameAndTeam(@Param("name") String name, @Param("teamId") Integer teamId);
}
