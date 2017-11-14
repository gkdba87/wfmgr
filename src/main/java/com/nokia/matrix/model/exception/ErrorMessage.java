package com.nokia.matrix.model.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * 
 * @author 1226211
 *
 *         Generic Error Message for handling all exceptions
 */

public class ErrorMessage {

	public static final String USER_NAME_NOT_EMPTY = "User already exists with email, please register with another email id";
	public static final String GENERIC_MESSAGE = "Internal server error, please contact workflow manager";
	public static final String BAD_REQUEST = "Internal server error, please contact workflow manager";

	private int statusCode;
	private int errorCode;
	private String message;
	private List<String> devErrorMessages;

	/**
	 * @return the statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * @param statusCode
	 *            the statusCode to set
	 */
	public void setStatusCode(HttpStatus statusCode) {
		this.statusCode = statusCode.value();
	}

	/**
	 * @return the errorCode
	 */
	public int getErrorCode() {
		return errorCode;
	}

	/**
	 * @param errorCode
	 *            the errorCode to set
	 */
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message
	 *            the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	public List<String> getDevErrorMessages() {
		return devErrorMessages;
	}

	public void setDevErrorMessages(List<String> devErrorMessages) {
		this.devErrorMessages = devErrorMessages;
	}


}
