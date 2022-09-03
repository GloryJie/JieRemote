package top.gloryjie.remote.serializer;

import lombok.Getter;

/**
 * @author jie-r
 * @since 2022/9/3
 */
@Getter
public enum InnerSerializer {
    JDK(1),
    HESSIAN2(2);

    private final int code;

    InnerSerializer(int code) {
        this.code = code;
    }
}
