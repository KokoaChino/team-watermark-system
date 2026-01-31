package github.kokoachino.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import github.kokoachino.model.entity.TeamMember;
import org.apache.ibatis.annotations.Mapper;


/**
 * 团队成员 Mapper 接口
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {

}
