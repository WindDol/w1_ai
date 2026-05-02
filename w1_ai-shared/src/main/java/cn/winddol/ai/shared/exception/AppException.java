package cn.winddol.ai.shared.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 5317680961212299217L;

    private final String code;
    private String info;

    public AppException(String code) {
        this.code = code;
    }

    public AppException(String code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public AppException(String code, String message) {
        super(message);
        this.code = code;
        this.info = message;
    }

    public AppException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.info = message;
    }
}
