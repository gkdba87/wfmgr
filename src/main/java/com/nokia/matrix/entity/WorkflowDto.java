/**
 * 
 */
package com.nokia.matrix.entity;

import java.io.Serializable;

/**
 * @author 1226211
 *
 */
public class WorkflowDto implements Serializable {

	private static final long serialVersionUID = 3956410947629390525L;
	private Long workflowId;
	private String workflowName;
	private String jiraTaskId;
	private Integer projectId;
	private String projectName;
	private String userName;
	private String file;

	public WorkflowDto() {

	}

	/**
	 * 
	 * @param workflowId
	 * @param workflowName
	 * @param taskId
	 * @param projectId
	 * @param userName
	 * @param uid
	 */
	public WorkflowDto(Long workflowId, String workflowName, String taskId, Integer projectId, String projectName, String userName) {
		super();
		this.workflowId = workflowId;
		this.workflowName = workflowName;
		this.jiraTaskId = taskId;
		this.projectId = projectId;
		this.projectName = projectName;
		this.userName = userName;
	}

	public Long getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(Long workflowId) {
		this.workflowId = workflowId;
	}

	public String getWorkflowName() {
		return workflowName;
	}

	public void setWorkflowName(String workflowName) {
		this.workflowName = workflowName;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getJiraTaskId() {
		return jiraTaskId;
	}

	public void setJiraTaskId(String jiraTaskId) {
		this.jiraTaskId = jiraTaskId;
	}

	public Integer getProjectId() {
		return projectId;
	}

	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public String toString() {
		return "WorkflowDto [workflowId=" + workflowId + ", workflowName=" + workflowName + ", jiraTaskId=" + jiraTaskId
				+ ", projectId=" + projectId + ", projectName=" + projectName + ", userName=" + userName + "]";
	}


}
