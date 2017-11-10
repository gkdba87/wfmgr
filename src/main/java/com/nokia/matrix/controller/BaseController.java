package com.nokia.matrix.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.nokia.matrix.exception.WorkFlowExecuteException;
import com.nokia.matrix.model.exception.ErrorMessage;

/**
 * 
 * @author 1226211
 * Base Controller to handle all the exceptions
 *
 */
public abstract class BaseController {
	
	private static final Log log = LogFactory.getLog(BaseController.class);
	
	//private MappingJacksonJsonView  jsonView = new MappingJacksonJsonView();
	
	@ExceptionHandler(Exception.class)
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody ErrorMessage
	genericError(HttpServletRequest req, Exception ex, HttpServletResponse response){
		ErrorMessage error = buildErrorMessage(ex.getMessage(), 
				HttpStatus.INTERNAL_SERVER_ERROR, 500, ex.getStackTrace(), null);
		//log.error(error.getDevMessage());
		return error;
	}
	
	
	@ExceptionHandler(IllegalStateException.class)
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody ErrorMessage
	IllegalStateExceptionError(HttpServletRequest req, Exception ex, HttpServletResponse response){
		ErrorMessage error = buildErrorMessage(ex.getMessage(), 
				HttpStatus.INTERNAL_SERVER_ERROR, 504, ex.getStackTrace(), null);
		//log.error(error.getDevMessage());
		return error;
	}
	
	@ExceptionHandler(WorkFlowExecuteException.class)
	@ResponseBody ErrorMessage
	userExceptionHandler(HttpServletRequest request, HttpServletResponse response, WorkFlowExecuteException ex) {
		response.setStatus(ex.getHttpStatus().value());
		ErrorMessage error = buildErrorMessage(ex.getErrorMessage(), 
				ex.getHttpStatus(), ex.getHttpStatus().value(), null, ex.getDevErrorMessages());
		return error;
	}
		
		
	
	private ErrorMessage buildErrorMessage(String message, HttpStatus status, int errorCode, StackTraceElement[] stackTraceElements, List<String> devErrorMsgs) {
		ErrorMessage error = new ErrorMessage();
		error.setMessage(message);
		error.setStatusCode(status);
		error.setErrorCode(errorCode);
		if(!ArrayUtils.isEmpty(stackTraceElements)) {
			int length = stackTraceElements.length;
			List<String> devMessages = new ArrayList<String>(length);
			for(int i=0; i< stackTraceElements.length; i++) {
				devMessages.add(stackTraceElements[i].toString());
			}
			error.setDevErrorMessages(devMessages);
		} else {
			error.setDevErrorMessages(devErrorMsgs);
		}
		
		return error;
	}
}
