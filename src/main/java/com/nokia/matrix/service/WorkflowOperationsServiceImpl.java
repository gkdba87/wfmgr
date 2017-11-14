package com.nokia.matrix.service;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nokia.matrix.dao.TaskRepository;
import com.nokia.matrix.dao.WorkflowRepository;
import com.nokia.matrix.entity.Task;
import com.nokia.matrix.entity.TaskStatus;
import com.nokia.matrix.entity.Workflow;
import com.nokia.matrix.entity.WorkflowDto;
import com.nokia.matrix.entity.WorkflowStatus;
import com.nokia.matrix.exception.WorkFlowExecuteException;
import com.nokia.matrix.model.exception.ErrorMessage;
import com.nokia.matrix.util.WorkflowUtil;

/**
 * 
 *
 */
@Service("WorkflowOperationsService")
public class WorkflowOperationsServiceImpl implements WorkflowOperationsService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	WorkflowRepository workflowRepo;

	@Autowired
	TaskRepository taskRepo;

	@Autowired
	ExecutionEngine executionEngine;

	private static final Log log = LogFactory.getLog(WorkflowOperationsServiceImpl.class);

	@Override
	public String create(WorkflowDto workflowData) {

		JSONObject createResponse = new JSONObject();

		try {
			log.debug("Creating a workflow with data :" + workflowData);
			// validate input
			validateInput(workflowData);

			Workflow workflow = new Workflow();
			workflow.setName(workflowData.getWorkflowName());
			workflow.setProjectId(new Integer(workflowData.getProjectId()));
			workflow.setProjectName(workflowData.getProjectName());
			workflow.setJiraTaskId(workflowData.getJiraTaskId());
			workflow.setUserName(workflowData.getUserName());
			workflow.setStatus(WorkflowStatus.CREATED.name());

			Long workflowId = workflowRepo.save(workflow).getId();

			log.info("Workflow created with id :" + workflowId);
			createResponse.put("workflowId", workflowId);

		} catch (WorkFlowExecuteException validationExe) {
			throw validationExe;
		} catch (Exception exe) {
			log.error(
					"Exception while creating workflow with data : " + workflowData.toString() + " : Stack trace is :",
					exe);
			throw new WorkFlowExecuteException(exe.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return createResponse.toString();

	}

	private void validateInput(WorkflowDto workflowData) throws WorkFlowExecuteException {

		List<String> errorMsgs = new ArrayList<String>();
		if (WorkflowUtil.isNullOrEmpty(workflowData.getWorkflowName())) {
			errorMsgs.add("WorkFlow Name not supplied");
		}
		if (WorkflowUtil.isNullOrEmpty(workflowData.getUserName())) {
			errorMsgs.add("User Name not supplied");
		}
		if (WorkflowUtil.isNull(workflowData.getProjectId())) {
			errorMsgs.add("Project id not supplied");
		}
		if (WorkflowUtil.isNull(workflowData.getJiraTaskId())) {
			errorMsgs.add("JIRA or OpenProject id not supplied");
		}
		if (WorkflowUtil.isNullOrEmpty(workflowData.getProjectName())) {
			errorMsgs.add("Project Name not supplied");
		}
		if (WorkflowUtil.isNeitherNullNorEmpty(errorMsgs)) {
			throw new WorkFlowExecuteException("Bad Request", HttpStatus.BAD_REQUEST, errorMsgs);
		}

	}

	@Override
	public String saveAndExecute(Long workflowId, MultipartFile file, boolean isExecute)
			throws WorkFlowExecuteException {

		List<Task> tasks = null;
		JSONObject result = new JSONObject();

		try {
			Workflow workflow = workflowRepo.findOne(workflowId);
			tasks = executionEngine.parseAndGetTasks(workflow.getProjectId().toString(), workflow.getId().toString(),
					file);
			workflow.setFile(file.getBytes());
			Set<Task> dbTasks = workflow.getTasks();
			Set<Task> mergedTasks = new HashSet<Task>();
			for (Task task : tasks) {
				boolean isExists = false;
				for (Task dbTask : dbTasks) {
					if (dbTask.getTaskName().equals(task.getTaskName())) {
						mergedTasks.add(dbTask);
						isExists = true;
						break;
					}
				}
				if (!isExists) {
					task.setStatus(TaskStatus.NOT_STARTED.name());
					task.setWorkflow(workflow);
					mergedTasks.add(task);
				}
			}
			workflow.getTasks().clear();
			workflow.getTasks().addAll(mergedTasks);
			workflowRepo.save(workflow);
			System.out.println("workflow saved.. " );
			if (isExecute) {
				log.info("Execution started for workflow with name : " + workflow.getName() + ", and user name is :"
						+ workflow.getUserName());
				boolean resumeWorkflow = false;
				for (Task task : mergedTasks) {
					if(task.getStatus().equals(TaskStatus.FAILED.name())  || task.getStatus().equals(TaskStatus.TIME_OUT.name())) {
						resumeWorkflow = true;
						break;
					}
				}
				
				executionEngine.executeWorkFlow(workflowId, resumeWorkflow);
			} else {
				log.debug("Saved workflow with name : " + workflow.getName() + ", and user name is :"
						+ workflow.getUserName());
			}
			System.out.println(" Flow executed successfully for workflow Id ::" + workflowId);
			result.put("status", HttpStatus.OK.value());
		} catch (WorkFlowExecuteException | FileNotFoundException fe) {
			System.out.println("Error in executing/saving workflow... " );
			throw new WorkFlowExecuteException(fe.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			log.error("Exception while saving/executing workflow." + e);
			System.out.println("Error in executing/saving workflow... ");
			e.printStackTrace();
			throw new WorkFlowExecuteException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		System.out.println("Retuning result for workfowid . "+ workflowId );
		return result.toString();

	} 
	
	private void checkForResumeOrReplay() {
		
	}
	

	@Override
	public WorkflowDto getWorkflowImage(Long id) {

		WorkflowDto dto = new WorkflowDto();
		try {
			log.debug("Retriving workflow from DB for Id :" + id);
			Workflow workflow = workflowRepo.findOne(id);
			dto.setWorkflowId(workflow.getId());
			dto.setWorkflowName(workflow.getName());
			dto.setProjectId(workflow.getProjectId());
			dto.setJiraTaskId(workflow.getJiraTaskId());
			dto.setUserName(workflow.getUserName());

			byte[] demBytes = workflow.getFile();
			if (null != demBytes) {
				String fileString = new String(demBytes, "UTF-8");
				dto.setFile(fileString);
			}

		} catch (Exception e) {
			log.error("Exception while getting workflow image with id " + id, e);
			throw new WorkFlowExecuteException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return dto;

	}

	@Override
	public List<JSONObject> getSavedWorkflowNames(String userName, Integer projectId) {

		List<JSONObject> workflowsJson = new ArrayList<JSONObject>();
		try {
			List<Object[]> workflows = workflowRepo.findUserWorkflows(userName, projectId);

			for (Object[] workflow : workflows) {
				JSONObject record = new JSONObject();
				record.put("workflowId", (Long) workflow[0]);
				record.put("workflowName", (String) workflow[1]);
				workflowsJson.add(record);
			}
		} catch (Exception e) {
			log.error("Exception while getting saved workflow names for userName = " + userName + ", and project id = "
					+ projectId, e);
			throw new WorkFlowExecuteException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return workflowsJson;

	}
	
	@Override
	public List<JSONObject> getSavedWorkflowNamesByJiraId(String userName, Integer projectId, String jiraTaskId) {

		List<JSONObject> workflowsJson = new ArrayList<JSONObject>();
		try {
			List<Object[]> workflows = workflowRepo.findUserWorkflowsByJiraId(userName, projectId, jiraTaskId);

			for (Object[] workflow : workflows) {
				JSONObject record = new JSONObject();
				record.put("workflowId", (Long) workflow[0]);
				record.put("workflowName", (String) workflow[1]);
				workflowsJson.add(record);
			}
		} catch (Exception e) {
			log.error("Exception while getting saved workflow names for userName = " + userName + ", and project id = "
					+ projectId, e);
			throw new WorkFlowExecuteException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return workflowsJson;

	}

	@Override
	public JSONObject getStatus(Long workflowId) {

		JSONObject statusResp = new JSONObject();
		List<JSONObject> taskList = new ArrayList<JSONObject>();
		try {
			statusResp.put("WorkflowId", workflowId);
			List<Task> tasks = taskRepo.findAllTasksByWorkflowId(workflowId);

			for (Task task : tasks) {
				JSONObject taskStatus = new JSONObject();
				taskStatus.put("taskId", task.getTaskId());
				taskStatus.put("taskName", task.getTaskName());
				taskStatus.put("status", task.getStatus());
				taskList.add(taskStatus);
			}
			statusResp.put("tasks", taskList);

		} catch (Exception e) {
			log.error("Exception while getting task status for workflow Id :" + workflowId, e);
			e.printStackTrace();
			throw new WorkFlowExecuteException(ErrorMessage.GENERIC_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return statusResp;

	}

	@Override
	public String findUserProjects(String userName) throws WorkFlowExecuteException {
		
		
		JSONObject userProjects = new JSONObject();
		List<JSONObject> projectsList = new ArrayList<JSONObject>();
		try {
			List<Object[]> projects = workflowRepo.findUserProjects(userName);
			for (Object[] project : projects) {
				JSONObject projectJson = new JSONObject();
				projectJson.put("projectId", (Integer) project[0]);
				projectJson.put("projectName", (String) project[1]);
				projectsList.add(projectJson);
			}
			userProjects.put("projectlist", projectsList);
		} catch (Exception e) {
			log.error("Exception while getting project list for userName :" + userName, e);
			e.printStackTrace();
			throw new WorkFlowExecuteException(ErrorMessage.GENERIC_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return userProjects.toString();
	}

	@Override
	public void getTaskSuccessFailureCount(String userName, Integer projectId, Long workflowId, String taskName) {
		
		if(!WorkflowUtil.isNeitherNullNorEmpty(taskName)) {
			
			log.info("Operation Name can not be empty");
			throw new WorkFlowExecuteException("Operation Name can not be empty", HttpStatus.BAD_REQUEST);
					
		}
		
		try{
			Criteria crit = entityManager.unwrap(Session.class).createCriteria(Task.class);
			crit.createAlias("workflow", "w");
			
			crit.add(Restrictions.eq("taskName", taskName));
			if(!WorkflowUtil.isNull(projectId)) {
				crit.add(Restrictions.eq("w.projectId", projectId));
			}
			if(!WorkflowUtil.isNull(workflowId)) {
				 crit.add(Restrictions.eq("w.id", workflowId));		
			}
			if(WorkflowUtil.isNeitherNullNorEmpty(userName)) {
				 crit.add(Restrictions.eq("w.userName", userName));		
			}
	        List<Task> tasks = crit.list();
	        System.out.println(tasks);
		}catch(Exception exe) {
			log.error("Exception while fetching success failure count fo taskName :" + taskName + " , Exception stack trace is : ", exe);
			exe.printStackTrace();
			throw new WorkFlowExecuteException(ErrorMessage.GENERIC_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		
	}
	
	@Override
	public List<Object[]> getTaskReports() {
		
		return taskRepo.getTaskReports();
		//System.out.println(tasks);
		
	}

}
