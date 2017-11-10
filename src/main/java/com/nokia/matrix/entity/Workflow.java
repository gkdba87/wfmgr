/**
 * 
 */
package com.nokia.matrix.entity;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * @author 1226211
 *
 */
@Entity
@Table(name = "WORKFLOW")
public class Workflow {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "WORKFLOW_SEQ")
	@SequenceGenerator(sequenceName = "WORKFLOW_SEQ", allocationSize = 1, name = "WORKFLOW_SEQ")
	@Column(name = "ID")
	private Long id;

	@Column(name = "NAME")
	private String name;

	@Column(name = "PROJECT_ID")
	private Integer projectId;
	
	@Column(name = "PROJECT_NAME")
	private String projectName;

	@Column(name = "USER_NAME")
	private String userName;
	
	@Column(name = "JIRA_TASK_ID")
	private String jiraTaskId;
	
	@Column(name = "STATUS")
	private String status;
	
	@Column(name = "FILE")
	@Lob
	@Type(type="org.hibernate.type.BinaryType")
    private byte[] file;
	
	@OneToMany(mappedBy="workflow", cascade = CascadeType.ALL,fetch=FetchType.LAZY, orphanRemoval= true)
    private Set<Task> tasks;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getProjectId() {
		return projectId;
	}

	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getJiraTaskId() {
		return jiraTaskId;
	}

	public void setJiraTaskId(String jiraTaskId) {
		this.jiraTaskId = jiraTaskId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public byte[] getFile() {
		return file;
	}

	public void setFile(byte[] file) {
		this.file = file;
	}

	public Set<Task> getTasks() {
		return tasks;
	}

	public void setTasks(Set<Task> tasks) {
		this.tasks = tasks;
	}
	
}
