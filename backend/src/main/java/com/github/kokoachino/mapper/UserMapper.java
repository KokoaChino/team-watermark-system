package com.github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.kokoachino.model.entity.User;
import org.apache.ibatis.annotations.Mapper;


/**
 * 用户 Mapper 接口
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}