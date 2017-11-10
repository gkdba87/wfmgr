package com.nokia.matrix.controller;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nokia.matrix.entity.WorkflowDto;
import com.nokia.matrix.exception.WorkFlowExecuteException;
import com.nokia.matrix.service.WorkflowOperationsService;
import com.nokia.matrix.util.WorkflowConstants;

/**
 * Controller to handle service portal workflow operations
 *
 */
@RestController
@RequestMapping(WorkflowConstants.WORKFLOW)
public class WorkflowOperationsController extends BaseController {

	private static final Log log = LogFactory.getLog(WorkflowOperationsController.class);

	@Autowired
	WorkflowOperationsService service;

	/**
	 *  Creates a workflow ind DB and returns the id 
	 * @param request
	 * @param workflowData
	 * @return workflowId
	 * @throws WorkFlowExecuteException
	 */
	@RequestMapping(value = WorkflowConstants.CREATE, method = RequestMethod.POST)
	public String create(HttpServletRequest request, @RequestBody WorkflowDto workflowData)
			throws WorkFlowExecuteException {

		return service.create(workflowData);

	}

	/**
	 * Save the workflow file and 
	 * @param request
	 * @param docDetails
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = WorkflowConstants.SAVE, method = RequestMethod.POST)
	public String save(HttpServletRequest request, @RequestParam(value = "workflowId") Long workflowId,
			@RequestParam("file") MultipartFile file) {

		return service.saveAndExecute(workflowId, file, false);

	}

	/**
	 * @param request
	 * @param docDetails
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = WorkflowConstants.EXECUTE, method = RequestMethod.POST)
	public String execute(HttpServletRequest request, @RequestParam(value = "workflowId") Long workflowId,
			@RequestParam("file") MultipartFile file) {

		return service.saveAndExecute(workflowId, file, true);

	}

	@RequestMapping(value = "/testFileupload", method = RequestMethod.GET)
	public String saveAndExecuteWorkflow1(HttpServletRequest request) throws Exception {

		File file = new File("E:/Matrix/bpmn/sampleBpmns/sampleBpmns/1508491517524data.bpmn");
		FileInputStream input = new FileInputStream(file);
		MultipartFile multipartFile = new MockMultipartFile("file", file.getName(), "text/plain",
				IOUtils.toByteArray(input));

		return service.saveAndExecute(77L, multipartFile, false);
		// workflowService.updateTask(27L, "Task_5","Success");
		/*
		 * JenkinsHelper helper = new JenkinsHelper();
		 * helper.triggerBuild("testjob1");
		 */

	}

	@RequestMapping(value = WorkflowConstants.GET, method = RequestMethod.GET)
	public WorkflowDto getWorkflow(HttpServletRequest request, @RequestParam Long id) {

		return service.getWorkflowImage(id);

	}

	@RequestMapping(value = WorkflowConstants.GET_ALL, method = RequestMethod.GET)
	public String getSavedWorkflowNames(HttpServletRequest request, @RequestParam String userName,
			@RequestParam Integer projectId) {

		return service.getSavedWorkflowNames(userName, projectId).toString();
	}

	@RequestMapping(value = WorkflowConstants.GET_STATUS, method = RequestMethod.GET)
	public String getStatus(HttpServletRequest request, @RequestParam Long workflowId) {

		return service.getStatus(workflowId).toString();
	}
	
	@RequestMapping(value = WorkflowConstants.GET_PROJECTS, method = RequestMethod.GET)
	public String getProjects(HttpServletRequest request, @RequestParam String userName) {

		return service.findUserProjects(userName);
	}
	
	@RequestMapping(value = WorkflowConstants.GET_OPERATION_RESULT, method = RequestMethod.GET)
	public void getTaskResult(HttpServletRequest request, @RequestParam String userName,
			@RequestParam Integer projectId, @RequestParam Long workflowId,  @RequestParam String taskName) {

		 service.getTaskSuccessFailureCount(userName, projectId, workflowId, taskName);
	}
	
	@RequestMapping(value = WorkflowConstants.GET_TASK_REPORTS, method = RequestMethod.GET)
	public List<Object[]> getTaskReports(HttpServletRequest request,@RequestParam Integer projectId, @RequestParam Long workflowId) {

		 return service.getTaskReports();
	}

}
