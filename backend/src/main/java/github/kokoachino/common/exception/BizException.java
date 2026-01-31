package github.kokoachino.common.exception;

import github.kokoachino.common.result.ResultCode;
import lombok.Getter;


/**
 * @author kokoachino
 * @date 2026-01-31
 */
@Getter
public class BizException extends RuntimeException {

    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BizException(String message) {
        super(message);
        this.resultCode = ResultCode.ERROR;
    }
}
