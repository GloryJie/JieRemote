package top.gloryjie.remote.exception;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * desc remote Exception
 * @author jie-r
 * @since 2022/9/2
 */
@Getter
@Setter
public class ExceptionProtocol implements Serializable {

    private int errorCode;

    private String errorMsg;

}
