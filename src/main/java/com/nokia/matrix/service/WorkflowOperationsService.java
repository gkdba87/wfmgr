package com.nokia.matrix.service;

import java.util.List;

import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import com.nokia.matrix.entity.WorkflowDto;
import com.nokia.matrix.exception.WorkFlowExecuteException;

public interface WorkflowOperationsService {

	/**
	 * Creates a workflow ind DB and returns the id 
	 * @param workflowData
	 * @return workflow id
	 */
	public String create(WorkflowDto workflowData) throws WorkFlowExecuteException;

	/**
	 * @param workflowId
	 * @param file
	 * @param isExecute
	 * @return
	 * @throws WorkFlowExecuteException
	 */
	public String saveAndExecute(Long workflowId, MultipartFile file, boolean isExecute) throws WorkFlowExecuteException;

	/**
	 * @param id
	 * @return
	 * @throws WorkFlowExecuteException
	 */
	public WorkflowDto getWorkflowImage(Long id) throws WorkFlowExecuteException;

	/**
	 * @param userName
	 * @param projectId
	 * @return
	 * @throws WorkFlowExecuteException
	 */
	public List<JSONObject> getSavedWorkflowNames(String userName, Integer projectId) throws WorkFlowExecuteException;
	
	/**
	 * @param userName
	 * @param projectId
	 * @return
	 * @throws WorkFlowExecuteException
	 */
	public List<JSONObject> getSavedWorkflowNamesByJiraId(String userName, Integer projectId, String jiraTaskId) throws WorkFlowExecuteException;

	/**
	 * @param workflowId
	 * @return
	 * @throws WorkFlowExecuteException
	 */
	public JSONObject getStatus(Long workflowId) throws WorkFlowExecuteException;
	
	
	/**
	 * @param userName
	 * @return
	 * @throws WorkFlowExecuteException
	 */
	public String findUserProjects(String userName)  throws WorkFlowExecuteException;
	
	void getTaskSuccessFailureCount(String userName, Integer projectId, Long workflowId, String taskName);

	public List<Object[]> getTaskReports();
}
