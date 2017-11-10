/**
 * 
 */
package com.nokia.matrix.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author 1226211
 *
 */
@Entity
@Table(name = "TASK")
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TASK_SEQ")
	@SequenceGenerator(sequenceName = "TASK_SEQ", allocationSize = 1, name = "TASK_SEQ")
	@Column(name = "ID")
	private Long id;

	@Column(name = "TASK_ID")
	private String taskId;

	@Column(name = "STATUS")
	private String status;
	
	@Column(name = "TASK_NAME")
	private String taskName;

	@Column(name = "EXECUTED_BY")
	private String executedBy;

	@Column(name="EXECUTED_ON", insertable=false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date executedOn;
	
	@Column(name = "SUCCESS_COUNT")
	private Integer successCount = 0;
	
	@Column(name = "FAILURE_COUNT")
	private Integer failureCount = 0;
	
	@Column(name = "DURATION")
	private Long duration;
	
	@ManyToOne
	@JoinColumn(name = "WORKFLOW_ID")
	private Workflow workflow;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getExecutedBy() {
		return executedBy;
	}

	public void setExecutedBy(String executedBy) {
		this.executedBy = executedBy;
	}

	public Date getExecutedOn() {
		return executedOn;
	}

	public void setExecutedOn(Date executedOn) {
		this.executedOn = executedOn;
	}

	public Integer getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(Integer successCount) {
		this.successCount = successCount;
	}

	public Integer getFailureCount() {
		return failureCount;
	}

	public void setFailureCount(Integer failureCount) {
		this.failureCount = failureCount;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public Workflow getWorkflow() {
		return workflow;
	}

	public void setWorkflow(Workflow workflow) {
		this.workflow = workflow;
	}
	
	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	@Override
	public String toString() {
		return "Task [id=" + id + ", taskId=" + taskId + ", status=" + status + ", taskName=" + taskName + "]";
	}

}
