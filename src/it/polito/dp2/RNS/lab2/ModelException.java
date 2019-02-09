package it.polito.dp2.RNS.lab2;

public class ModelException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ModelException() {
	}

	public ModelException(String message) {
		super(message);
	}

	public ModelException(Throwable cause) {
		super(cause);
	}

	public ModelException(String message, Throwable cause) {
		super(message, cause);
	}

	public ModelException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
