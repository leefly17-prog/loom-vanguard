package io.github.leefly17_prog.loomvanguard.exception;

/**
 * Vanguard 组件自定义异常
 *
 * @author Fly
 * @since 1.0.0
 */
public class VanguardException extends RuntimeException {
    public VanguardException(String message) {
        super(message);
    }

    public VanguardException(String message, Throwable cause) {
        super(message, cause);
    }

    public VanguardException(Throwable cause) {
        super(cause);
    }
}

