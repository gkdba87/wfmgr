package com.nokia.matrix.entity;

/**
 * BuildInfo
 */

public class BuildInfo   {
	
  private boolean result = false;

private String message = null;

private long duration;
private boolean buildTimeOut = false;

public boolean isResult() {
	return result;
}

public void setResult(boolean result) {
	this.result = result;
}
  

public String getMessage() {
	return message;
}

public void setMessage(String message) {
	this.message = message;
}

public long getDuration() {
	return duration;
}

public void setDuration(long duration) {
	this.duration = duration;
}

public boolean isBuildTimeOut() {
	return buildTimeOut;
}

public void setBuildTimeOut(boolean buildTimeOut) {
	this.buildTimeOut = buildTimeOut;
}






  
}

