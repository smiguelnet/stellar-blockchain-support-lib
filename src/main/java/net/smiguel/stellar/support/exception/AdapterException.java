package net.smiguel.stellar.support.exception;

public class AdapterException extends RuntimeException {

    private String code;
    private String message;

    public AdapterException() {
        super();
    }

    public AdapterException(String message) {
        super(message);
    }

    public AdapterException(String message, Throwable cause) {
        super(message, cause);
    }

    public AdapterException(Throwable cause) {
        super(cause);
    }

    protected AdapterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
