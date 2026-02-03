package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.BlackList;
import org.apache.ibatis.annotations.Mapper;


/**
 * 黑名单 Mapper 接口
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Mapper
public interface BlackListMapper extends BaseMapper<BlackList> {

}
