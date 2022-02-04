package pubsher.talexsoultech.talex.electricity.exception;

public class ElectricityException extends Exception {

    public ElectricityException() {

        super();
    }

    public ElectricityException(String message) {

        super(message);
    }

    public ElectricityException(String message, Throwable cause) {

        super(message, cause);
    }

    public ElectricityException(Throwable cause) {

        super(cause);
    }

    protected ElectricityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {

        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public String getMessage() {

        return "Electricity # " + super.getMessage();

    }

}
