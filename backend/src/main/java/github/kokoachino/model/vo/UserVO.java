package github.kokoachino.model.vo;

import lombok.Builder;
import lombok.Data;


/**
 * @author kokoachino
 * @date 2026-01-31
 */
@Data
@Builder
public class UserVO {
    private Integer id;
    private String username;
    private String email;
    private String token;
}