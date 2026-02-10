package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.List;


/**
 * 分页结果 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Builder
@Schema(description = "分页结果")
public class PageVO<T> {

    @Schema(description = "数据列表")
    private List<T> list;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "当前页码", example = "1")
    private Integer page;

    @Schema(description = "每页大小", example = "20")
    private Integer size;

    @Schema(description = "总页数", example = "5")
    public Integer getTotalPages() {
        if (size == null || size == 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / size);
    }
}
