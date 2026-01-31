package github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import github.kokoachino.model.entity.Team;
import org.apache.ibatis.annotations.Mapper;


/**
 * 团队 Mapper 接口
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Mapper
public interface TeamMapper extends BaseMapper<Team> {

}
