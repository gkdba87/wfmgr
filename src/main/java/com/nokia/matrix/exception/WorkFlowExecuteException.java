package com.nokia.matrix.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * @author 1226211
 * 
 * An Exception class to handle work flow operations exceptions
 *
 */
public class WorkFlowExecuteException extends RuntimeException{
	
	private static final long serialVersionUID = 1L;
	
	private String errorMessage;
	
	private HttpStatus status;
	
	private List<String> devErrorMessages;
	
	public WorkFlowExecuteException(String errorMessage) {
		super(errorMessage);
	}
	
	public WorkFlowExecuteException(String errorMessage, HttpStatus httpStatus) {
		super(errorMessage);
		this.status = httpStatus;
		this.errorMessage = errorMessage;
	}
	
	public WorkFlowExecuteException(String errorMessage, HttpStatus httpStatus, List<String> devErrorMessages) {
		super(errorMessage);
		this.status = httpStatus;
		this.errorMessage = errorMessage;
		this.devErrorMessages = devErrorMessages;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public HttpStatus getHttpStatus() {
		return status;
	}
	
	public List<String> getDevErrorMessages() {
		return devErrorMessages;
	}
}
