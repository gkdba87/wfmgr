package com.nokia.matrix.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.nokia.matrix.dao.TaskRepository;
import com.nokia.matrix.entity.BuildInfo;
import com.nokia.matrix.entity.Task;
import com.nokia.matrix.entity.TaskStatus;
import com.nokia.matrix.exception.WorkFlowExecuteException;
import com.nokia.matrix.model.TDefinitions;
import com.nokia.matrix.model.TEndEvent;
import com.nokia.matrix.model.TExclusiveGateway;
import com.nokia.matrix.model.TFlowElement;
import com.nokia.matrix.model.TIntermediateThrowEvent;
import com.nokia.matrix.model.TProcess;
import com.nokia.matrix.model.TRootElement;
import com.nokia.matrix.model.TSequenceFlow;
import com.nokia.matrix.model.TStartEvent;
import com.nokia.matrix.model.TTask;

@Component
//@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = "prototype")
public class ExecutionEngine {
	
	@Autowired
	TaskRepository taskRepo;
	
	@Autowired
	JenkinsHelper jenkinsHelper;
	
	private static final Log log = LogFactory.getLog(ExecutionEngine.class);
	List<JAXBElement<? extends TFlowElement>> tfe;
	private static Map<String, String> mapTaskStatus;
	private String PID, WID;
	private String prevExecutedTask = "0";
	
	/**
	 * 
	 */
	
	public ExecutionEngine() {
		mapTaskStatus = new HashMap<>();
	}

	/**
	 * Parse and save file.
	 * 
	 * @param PID
	 * @param WID
	 * @param inputFile
	 * @throws WorkFlowExecuteException
	 * @throws Exception
	 */
	public List<Task> parseAndGetTasks(String PID, String WID, MultipartFile file) throws WorkFlowExecuteException, Exception {
		
		parse(PID, WID, file.getInputStream());
		return getTasks(PID, WID);
	}

	/**
	 * 
	 * @param inputFile
	 * @throws WorkFlowExecuteException
	 * @throws Exception
	 */
	private void parse(String PID, String WID, InputStream inputStream) throws WorkFlowExecuteException, Exception {

		this.PID = PID;
		this.WID = WID;
		JAXBContext jaxbContext = JAXBContext.newInstance("com.nokia.matrix.model");
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		JAXBElement<?> definition = (JAXBElement<?>) unmarshaller.unmarshal(inputStream);

		TDefinitions tds = (TDefinitions) definition.getValue();
		List<JAXBElement<? extends TRootElement>> lsttreEle = tds.getRootElement();

		for (int i = 0; i < lsttreEle.size(); i++) {
			if (lsttreEle.get(i).getValue() instanceof TProcess) {
				TProcess tp = (TProcess) lsttreEle.get(i).getValue();
				tfe = tp.getFlowElement();
			}
		}
	}

	/**
	 * 
	 * @param inputFile
	 * @throws WorkFlowExecuteException
	 * @throws Exception
	 */
	private List<Task> getTasks(String PID, String WID) throws WorkFlowExecuteException, Exception {

		List<Task> taskList = new ArrayList<>();

		for (int j = 0; j < tfe.size(); j++) {
			switch (tfe.get(j).getDeclaredType().getSimpleName()) {

			case "TTask":
				TTask ttask = (TTask) tfe.get(j).getValue();
				Task t = new Task();
				t.setTaskId(ttask.getId());
				t.setTaskName(ttask.getName());
				taskList.add(t);
				break;

			default:
				break;
			}
		}
		
		return taskList;

		
	}

	/**
	 * 
	 * @param timeOut 
	 * @param taskname
	 * @return
	 * @throws WorkFlowExecuteException
	 * @throws Exception
	 */
	private BuildInfo executeTask(Long workflowId, String taskName, boolean resumeWorkflow, String timeOut) throws WorkFlowExecuteException, Exception {

		// Check in DB if task is already executed successfully in resume workflow execution case.
		if(resumeWorkflow && isPreviousExecutionSuccess(workflowId, taskName)) {
			BuildInfo buildInfoResume = new  BuildInfo();
			buildInfoResume.setResult(true);
			buildInfoResume.setBuildTimeOut(false);
			return buildInfoResume;
		}
		
		BuildInfo buildInfo =  jenkinsHelper.triggerBuild(taskName, workflowId, timeOut);
		boolean result = buildInfo.isResult();
		updateTask(workflowId, taskName, result ? TaskStatus.SUCCESS : TaskStatus.FAILED, buildInfo.getDuration());
		System.out.println("Executed task :" + taskName + " Result is :" + result);
		return buildInfo;

	}

	/**
	 * 
	 * @param UID
	 * @throws WorkFlowExecuteException
	 * @throws Exception
	 */
	public void executeWorkFlow(Long workflowId, boolean resumeWorkflow) throws WorkFlowExecuteException, Exception {
		boolean end = false;
		String timeOut = null;
		List<String> outGoingSeqFlow = new ArrayList<>();
		for (int i = 0; i < tfe.size() - 1; i++) {
			if (tfe.get(i).getDeclaredType().equals(TStartEvent.class)) {
				TStartEvent tse = (TStartEvent) tfe.get(i).getValue();
				Iterator<QName> itrQ = tse.getOutgoing().iterator();
				while (itrQ.hasNext()) {
					QName seqOutGoing = itrQ.next();
					outGoingSeqFlow.add(seqOutGoing.getLocalPart());
				}
			}
		}

		while (!end) {

			int size = outGoingSeqFlow.size();
			String[] taskList = new String[size];
			for (int i = 0; i < size; i++) {
				String taskToExecute = getNextTaskName(outGoingSeqFlow.get(i));
				if (taskToExecute != null) {
					taskList[i] = taskToExecute;
					prevExecutedTask = taskToExecute;
					if (isNextFlowElementIntermThrowEvent(taskToExecute)) {
						outGoingSeqFlow = getOutGoingSeqFlowForTasks(taskList);
						String nextTask = getNextTaskName(outGoingSeqFlow.get(i));
						timeOut = nextTask;
						taskList[i] = nextTask;
					}
					System.out.println("Task Executed, updating prev task : " + prevExecutedTask);
					BuildInfo buildInfo = executeTask(workflowId, taskToExecute, resumeWorkflow, "20");
					if (buildInfo.isResult()) {
						mapTaskStatus.put(taskToExecute, "1");
					} else {
						mapTaskStatus.put(taskToExecute, "0");
						if(buildInfo.isBuildTimeOut()){
							throw new WorkFlowExecuteException(taskToExecute + " timed out");
						}
						else if (!isNextFlowElementExclusiveGw(taskToExecute)) {
							throw new WorkFlowExecuteException(taskToExecute);
						}
					}
				} else {
					end = true;
					break;
				}
			}
			outGoingSeqFlow = new ArrayList<String>();
			outGoingSeqFlow = getOutGoingSeqFlowForTasks(taskList);
		}
	}

	/**
	 * 
	 * @param taskToExecute
	 * @return
	 * @throws Exception
	 */
	private boolean isNextFlowElementExclusiveGw(String taskToExecute) throws Exception {

		String[] taskList = { taskToExecute };
		List<String> outGoingSeqFlow = getOutGoingSeqFlowForTasks(taskList);
		String seqId = outGoingSeqFlow.get(0);
		for (int i = 0; i < tfe.size(); i++) {
			switch (tfe.get(i).getDeclaredType().getSimpleName()) {

			case "TSequenceFlow":
				TSequenceFlow tsf = (TSequenceFlow) tfe.get(i).getValue();
				if (tsf.getId().equalsIgnoreCase(seqId)) {
					if (tsf.getTargetRef() instanceof TExclusiveGateway) {
						return true;
					}
				}
				break;
			default:
				break;
			}
		}
		return false;
	}

	private boolean isNextFlowElementIntermThrowEvent(String taskName) throws Exception {

		String[] taskList = { taskName };
		List<String> outGoingSeqFlow = getOutGoingSeqFlowForTasks(taskList);
		String seqId = outGoingSeqFlow.get(0);
		for (int i = 0; i < tfe.size(); i++) {
			switch (tfe.get(i).getDeclaredType().getSimpleName()) {

			case "TSequenceFlow":
				TSequenceFlow tsf = (TSequenceFlow) tfe.get(i).getValue();
				if (tsf.getId().equalsIgnoreCase(seqId)) {
					if (tsf.getTargetRef() instanceof TIntermediateThrowEvent) {
						return true;
					}
				}
				break;
			default:
				break;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param taskList
	 * @return
	 * @throws WorkFlowExecuteException
	 * @throws Exception
	 */
	private List<String> getOutGoingSeqFlowForTasks(String[] taskList) throws WorkFlowExecuteException, Exception {
		List<String> outGoingSeqFlow = new ArrayList<String>();
		for (int i = 0; i < taskList.length; i++) {
			for (int j = 0; j < tfe.size(); j++) {
				switch (tfe.get(j).getDeclaredType().getSimpleName()) {

				case "TTask":
					TTask ttask = (TTask) tfe.get(j).getValue();
					if (ttask.getName().equalsIgnoreCase(taskList[i])) {
						Iterator<QName> itrQ = ttask.getOutgoing().iterator();
						while (itrQ.hasNext()) {
							outGoingSeqFlow.add(itrQ.next().getLocalPart());
						}
						System.out.println("Returning Outgoing " +  outGoingSeqFlow.get(0));
						return outGoingSeqFlow;
					}
					break;
				case "TIntermediateThrowEvent":
					TIntermediateThrowEvent tite = (TIntermediateThrowEvent) tfe.get(j).getValue();
					if (tite.getName().equalsIgnoreCase(taskList[i])) {
						Iterator<QName> itrQ = tite.getOutgoing().iterator();
						while (itrQ.hasNext()) {
							outGoingSeqFlow.add(itrQ.next().getLocalPart());
						}
						System.out.println("Returning Outgoing " +  outGoingSeqFlow.get(0));
						return outGoingSeqFlow;
					}
					break;

				default:
					break;
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @param seqId
	 * @return
	 * @throws WorkFlowExecuteException
	 * @throws Exception
	 */
	private String getNextTaskName(String seqId) throws WorkFlowExecuteException, Exception {

		for (int i = 0; i < tfe.size(); i++) {
			switch (tfe.get(i).getDeclaredType().getSimpleName()) {

			case "TSequenceFlow":
				TSequenceFlow tsf = (TSequenceFlow) tfe.get(i).getValue();
				if (tsf.getTargetRef() instanceof TEndEvent && tsf.getId().equalsIgnoreCase(seqId)) {
					return null;
				}
				if (tsf.getId().equalsIgnoreCase(seqId)) {
					for (int j = 0; j < tfe.size(); j++) {
						switch (tfe.get(j).getDeclaredType().getSimpleName()) {
						case "TTask":
							if (!(tsf.getTargetRef() instanceof TTask)) {
								break;
							}
							TTask targetRef = (TTask) tsf.getTargetRef();
							TTask tt = (TTask) tfe.get(j).getValue();
							if (tt.getId().equalsIgnoreCase(targetRef.getId())) {
								System.out.println("Found Task to execute : " + tt.getName());
								return tt.getName();
							}
							break;
						case "TExclusiveGateway":
							String exGwId;
							TExclusiveGateway teg = (TExclusiveGateway) tfe.get(j).getValue();
							if (!(tsf.getTargetRef() instanceof TExclusiveGateway)) {
								break;
							} else {
								TExclusiveGateway tegtmp = (TExclusiveGateway) tsf.getTargetRef();
								exGwId = tegtmp.getId();
								if (!teg.getId().equalsIgnoreCase(exGwId)) {
									break;
								}
							}

							List<String> outGoingSeqFlow = new ArrayList<String>();

							Iterator<QName> itrQ = teg.getOutgoing().iterator();
							String tmpSeqId = "";
							String prevStatus = mapTaskStatus.get(prevExecutedTask);
							while (itrQ.hasNext()) {
								QName qn = itrQ.next();
								TSequenceFlow tmpTsf = getSequenceObject(qn.getLocalPart());
								if (tmpTsf.getName().equals(prevStatus)) {
									tmpSeqId = qn.getLocalPart();
									outGoingSeqFlow.add(qn.getLocalPart());
								}

							}
							if (!tmpSeqId.equals("")) {
								System.out.println("Calling for Seq Id " + tmpSeqId);
								return getNextTaskName(tmpSeqId);
							}
							break;

						case "TIntermediateThrowEvent":
							if (!(tsf.getTargetRef() instanceof TIntermediateThrowEvent)) {
								break;
							}
							TIntermediateThrowEvent tite = (TIntermediateThrowEvent) tsf.getTargetRef();
							TIntermediateThrowEvent ite = (TIntermediateThrowEvent) tfe.get(j).getValue();
							if (ite.getId().equalsIgnoreCase(tite.getId())) {
								System.out.println("Found throw event to execute : " + ite.getName());
								return ite.getName();
							}
							break;

						case "TEndEvent":
							if (!(tsf.getTargetRef() instanceof TEndEvent)) {
								break;
							}
							TEndEvent targetRefee = (TEndEvent) tsf.getTargetRef();
							TEndEvent tee = (TEndEvent) tfe.get(j).getValue();
							if (tee.getId().equalsIgnoreCase(targetRefee.getId())) {
								return tee.getId();
							}
							break;
						default:
							break;
						}
					}
				}
				break;
			default:
				break;
			}
		}
		return null;
	}

	/**
	 * 
	 * @param seqId
	 * @return
	 */
	private TSequenceFlow getSequenceObject(String seqId) {

		for (int i = 0; i < tfe.size(); i++) {
			switch (tfe.get(i).getDeclaredType().getSimpleName()) {

			case "TSequenceFlow":
				TSequenceFlow tsf = (TSequenceFlow) tfe.get(i).getValue();
				if (tsf.getId().equalsIgnoreCase(seqId)) {
					return tsf;
				}
				break;
			default:
				break;
			}
		}
		return null;
	}

	public void updateTask(Long workflowId, String taskName, TaskStatus status, long duration) {

		try {
			Task task = taskRepo.findTaskByName(workflowId, taskName);
			if(TaskStatus.SUCCESS.name().equals(status.name())) {
				task.setSuccessCount(task.getSuccessCount() + 1);
				task.setDuration(duration);
				
			} else if(TaskStatus.FAILED.name().equals(status.name()))  {
				task.setFailureCount(task.getFailureCount() + 1);
			} 
			task.setStatus(status.name());
			task.setExecutedOn(new Date());
			taskRepo.save(task);
		} catch (NullPointerException exe) {
			log.error("No task found with taskName: " + taskName + "with exception" + exe);
			throw new WorkFlowExecuteException("Task execution failed : ", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception exe) {
			log.error("Exception while updating task " + exe);
			throw new WorkFlowExecuteException("Task execution failed : ", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}
	
	public boolean isPreviousExecutionSuccess(Long workflowId, String taskName) {

		boolean isExecuted = false;
		try {
			Task task = taskRepo.findTaskByName(workflowId, taskName);
			if(TaskStatus.SUCCESS.name().equals(task.getStatus())) {
				isExecuted = true;
			}
		} catch (NullPointerException exe) {
			log.error("No task found with taskName: " + taskName + "with exception" + exe);
			throw new WorkFlowExecuteException("Fetch task status failed : ", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception exe) {
			log.error("Exception while checking task execution " + exe);
			throw new WorkFlowExecuteException("Checking task execution failed : ", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return isExecuted;

	}
	
}
