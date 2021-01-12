package exceptions;

public class InvalidWikiTextException extends Exception {
	public InvalidWikiTextException(String message, Throwable cause) {
		super(message, cause);
	}
}
