package github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import github.kokoachino.model.entity.User;
import org.apache.ibatis.annotations.Mapper;


/**
 * @author kokoachino
 * @date 2026-01-31
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}