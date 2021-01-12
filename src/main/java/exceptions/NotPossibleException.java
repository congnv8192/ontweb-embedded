package exceptions;

public class NotPossibleException extends RuntimeException {
	public NotPossibleException(String message) {
		super(message);
	}
	
	public NotPossibleException(String message, Throwable exception) {
		super(message, exception);
	}
}
