package com.nokia.matrix.service;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.nokia.matrix.dao.TaskRepository;
import com.nokia.matrix.entity.BuildInfo;
import com.nokia.matrix.entity.Task;
import com.nokia.matrix.entity.TaskStatus;
import com.nokia.matrix.exception.WorkFlowExecuteException;
import com.nokia.matrix.util.WorkflowUtil;

@Component
public class JenkinsHelper {
	private String jenkinsUrl, jenkinsUser, jenkinsPass, jenkinsJobPath;
	
	@Autowired
	TaskRepository taskRepo;
	
	public JenkinsHelper() {
		
		String jenkinsServer = "http://10.133.114.254";
		String jenkinsPort = "8080";
		jenkinsUser = "admin";
		jenkinsPass = "admin";
		jenkinsUrl = jenkinsServer + ":" + jenkinsPort;
		jenkinsJobPath = "/job/";
		
		// Openshift instance
		/*jenkinsUser = "matrix";
		jenkinsPass = "matrix";
		//jenkinsUrl = jenkinsServer + ":" + jenkinsPort;
		//jenkinsUrl = "https://jenkins-cicd.openshift4-infra-0.project.matrix.com";
		jenkinsUrl = "http://jenkins.cicd.svc:80";
		jenkinsJobPath = "/job/";*/
	}

	private static final Logger log = Logger.getLogger(JenkinsHelper.class);

	public HttpClient getClient() throws WorkFlowExecuteException {
		HttpClient httpClient;
		try {
			CredentialsProvider provider = new BasicCredentialsProvider();
			provider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
					new UsernamePasswordCredentials(jenkinsUser, jenkinsPass));
			httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

		} catch (Exception conExe) {
			throw new WorkFlowExecuteException("Unable to create jenkins clinet" + conExe);
		}

		return httpClient;
	}

	public BuildInfo triggerBuild(String taskName, Long workflowId, String timeOut) {

		BuildInfo buildInfo = new BuildInfo();
		
		try {

			HttpClient httpClient = getClient();

			log.info("url = " + jenkinsUrl);
			String buildUrl = jenkinsUrl + jenkinsJobPath + taskName + "/build";

			// Generate BASIC scheme object and stick it to the execution
			// context
			BasicScheme basicAuth = new BasicScheme();
			BasicHttpContext context = new BasicHttpContext();
			context.setAttribute("preemptive-auth", basicAuth);

			CrumbJson crumbJson = getCrumb(httpClient, jenkinsUrl);

			HttpPost httpPost = new HttpPost(buildUrl);
			httpPost.addHeader("User-Agent", "Mozilla/5.0");
			httpPost.addHeader(crumbJson.crumbRequestField, crumbJson.crumb);

			// Execute your request with the given context
			HttpResponse resp = httpClient.execute(httpPost, context);
			Header locationHeader = resp.getLastHeader("Location");
			if(null == locationHeader) {
				log.error("Operation Name not found in jenkins server :URL is :" + jenkinsUrl);
				throw new WorkFlowExecuteException("Operation Name not found in jenkins server : " + HttpStatus.INTERNAL_SERVER_ERROR);
			}
			String queueUrl = resp.getLastHeader("Location").getValue();
			
			// Updating task status
			updateTask(workflowId, taskName, TaskStatus.QUEUED, 0);
			
			QueueResponse queueData = getBuildNumberFromQueue(taskName, queueUrl, httpClient);
			
			// Updating task status
			updateTask(workflowId, taskName, TaskStatus.IN_PROGRESS, 0);
			String buildStatusURL= jenkinsUrl + jenkinsJobPath + taskName + "/" + queueData.getBuildNumber() +"/";
			System.out.println("Getting build status URL is : "+ buildStatusURL);
			BuildResponse buildResp  = getBuildStatus(buildStatusURL, timeOut);
			
			if(buildResp.isBuildtimeOut()){
				String stopBuildURL= jenkinsUrl + jenkinsJobPath + taskName + "/" + queueData.getBuildNumber() +"/stop";
				stopBuild(stopBuildURL,crumbJson,httpClient,context);
				buildResp  = getBuildStatus(buildStatusURL, timeOut);
				if(!buildResp.getResult())
					buildResp.setBuildtimeOut(true);
			}
			
			if(!WorkflowUtil.isNull(buildResp)) {
				buildInfo.setResult(buildResp.getResult());
				buildInfo.setDuration(buildResp.getDuration());
				buildInfo.setBuildTimeOut(buildResp.buildtimeOut);
				
			} 
		} catch (WorkFlowExecuteException exe) {
			throw exe;
		}
		catch (Exception exe) {
			log.error("Exception while triggering jenkins job : " + taskName, exe);
			buildInfo.setResult(false);
			buildInfo.setMessage(exe.getMessage());
			throw new WorkFlowExecuteException("Exception while triggering jenkins job : " + HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return buildInfo;

	}

	private void stopBuild(String stopBuildURL, CrumbJson crumbJson, HttpClient httpClient, BasicHttpContext context) throws ClientProtocolException, IOException {
		HttpPost httpPost = new HttpPost(stopBuildURL);
		httpPost.addHeader("User-Agent", "Mozilla/5.0");
		httpPost.addHeader(crumbJson.crumbRequestField, crumbJson.crumb);
		
		HttpResponse resp = httpClient.execute(httpPost, context);
		Header locationHeader = resp.getLastHeader("Location");
		if(null == locationHeader) {
			log.error("Operation Name not found in jenkins server :URL is :" + jenkinsUrl);
			throw new WorkFlowExecuteException("Operation Name not found in jenkins server : " + HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}

	public JSONObject getLatestBuild(String uid, String operation) throws JSONException  {
		
		JSONObject response = null;
		try {
			response = new JSONObject();
			HttpClient httpClient = getClient();

			log.info("url = " + jenkinsUrl);

			CrumbJson crumbJson = getCrumb(httpClient, jenkinsUrl);

			HttpPost httpost = new HttpPost(jenkinsUrl + "rssLatest");
			httpost.addHeader(crumbJson.crumbRequestField, crumbJson.crumb);
			toString(httpClient, httpost);

			HttpGet httpGet = new HttpGet(jenkinsUrl + "/lastBuild/api/json");
			httpGet.addHeader(crumbJson.crumbRequestField, crumbJson.crumb);
			toString(httpClient, httpGet);
			
			response.put("operationName", operation);
			response.put("result", "Success");

		} catch (Exception exe) {
			log.error("Exception while triggering jenkins job : " + uid, exe);
			response.put("operationName", operation);
			response.put("result", "FAILED");
		}

		return response;

	}

	public QueueResponse getBuildNumberFromQueue(String uid, String queueUrl, HttpClient httpClient)
			throws WorkFlowExecuteException {

		String queueResp;
		QueueResponse queueData;
		try {
			HttpGet httpGet = new HttpGet(queueUrl + "api/json");
			queueResp = toString(httpClient, httpGet);
			queueData = parseQueueResp(queueResp);
			
			while (queueData.isInQueue()) {
				log.info("Job still in queue ...." + queueData.getMessage());
				TimeUnit.SECONDS.sleep(5);
				queueResp = toString(httpClient, httpGet);
				queueData = parseQueueResp(queueResp);
			}

		} catch (Exception exe) {
			throw new WorkFlowExecuteException("Exception in queue processing: ");
		}

		return queueData;

	}

	private CrumbJson getCrumb(HttpClient httpClient, String jenkinsUrl) throws Exception {
		HttpGet httpGet = new HttpGet(jenkinsUrl + "/crumbIssuer/api/json");
		httpGet.addHeader("User-Agent", "Mozilla/5.0");
		String crumbResponse = toString(httpClient, httpGet);
		CrumbJson crumbJson = new Gson().fromJson(crumbResponse, CrumbJson.class);
		return crumbJson;
	}

	private BuildResponse getBuildStatus(String buildUrl, String timeOut ) throws Exception {

		BuildResponse buildData;
		long startTime = System.currentTimeMillis(); // fetch starting time
			
		HttpClient httpClient = getClient();
		HttpGet httpGet = new HttpGet(buildUrl + "api/json");
		buildData = parseBuildResp(toString(httpClient, httpGet));
			
		while(buildData.isBuilding()) {
			if(null != timeOut) {
			long waitTime = TimeUnit.SECONDS.toMillis(Long.parseLong(timeOut));
			if ((System.currentTimeMillis() - startTime) > waitTime) {
				buildData.setBuildtimeOut(true);
				log.info("Build wait time has been reached, hence stopping the job ...." + waitTime );
				break;
			}
			}
			log.info("Build is running ....");
			TimeUnit.SECONDS.sleep(5);
			buildData = parseBuildResp(toString(httpClient, httpGet));
		}
		return buildData;
	}


	private BuildResponse parseBuildResp(String buildResp) {

		BuildResponse buildResponse = new BuildResponse();
		try {
			JSONObject jsonobj = new JSONObject(buildResp);
			if (null != jsonobj.get("building")) {
				String buildStatus = jsonobj.getString("building");
				if (null != buildStatus && "false".equals(buildStatus)) {
					String result = jsonobj.getString("result");
					if("SUCCESS".equals(result)){
						buildResponse.setResult(true);
					}
					buildResponse.setDuration(jsonobj.getLong("duration"));
				} else if (isNitherNullNorEmpty(buildStatus) && "true".equals(buildStatus)) {
					buildResponse.setBuilding(true);
				}
			}
		} catch (Exception exe) {
			log.error("Exception while gettig build status :: " + exe);
		}
		return buildResponse;
	}

	private QueueResponse parseQueueResp(String queueResp) {

		QueueResponse queueData = new QueueResponse();
		try {
			JSONObject jsonobj = new JSONObject(queueResp);
			if (null != jsonobj.get("why")) {
				String queueMessage = jsonobj.getString("why");
				if (null != queueMessage && "null".equals(queueMessage)) {
					JSONObject executableObj = (JSONObject) jsonobj.get("executable");
					queueData.setBuildURL(executableObj.getString("url"));
					queueData.setBuildNumber(executableObj.getString("number"));
				} else if (isNitherNullNorEmpty(queueMessage) && !"null".equals(queueMessage)) {
					queueData.setMessage(queueMessage);
					queueData.setInQueue(true);
				}
			}
		} catch (Exception exe) {
			log.error("error..." + exe);
		}
		return queueData;
	}

	// helper construct to deserialize crumb json into
	public static class CrumbJson {
		public String crumb;
		public String crumbRequestField;
	}

	public static class QueueResponse {

		public boolean inQueue = false;
		public String message;
		public String buildURL;
		public String buildNumber;
		public boolean queueTimeOut;

		public boolean isInQueue() {
			return inQueue;
		}

		public void setInQueue(boolean inQueue) {
			this.inQueue = inQueue;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getBuildURL() {
			return buildURL;
		}

		public void setBuildURL(String buildURL) {
			this.buildURL = buildURL;
		}

		public String getBuildNumber() {
			return buildNumber;
		}

		public void setBuildNumber(String buildNumber) {
			this.buildNumber = buildNumber;
		}

		public boolean isQueueTimeOut() {
			return queueTimeOut;
		}

		public void setQueueTimeOut(boolean queueTimeOut) {
			this.queueTimeOut = queueTimeOut;
		}
	}

	public static class BuildResponse {

		public boolean building = false;
		public boolean result = false;
		public long duration;
		public boolean buildtimeOut = false;

		public boolean isBuilding() {
			return building;
		}

		public void setBuilding(boolean building) {
			this.building = building;
		}

		public boolean getResult() {
			return result;
		}

		public void setResult(boolean result) {
			this.result = result;
		}

		public long getDuration() {
			return duration;
		}

		public void setDuration(long duration) {
			this.duration = duration;
		}

		public boolean isBuildtimeOut() {
			return buildtimeOut;
		}

		public void setBuildtimeOut(boolean buildtimeOut) {
			this.buildtimeOut = buildtimeOut;
		}
		
	}

	private static String toString(HttpClient client, HttpRequestBase request) throws Exception {
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String responseBody = client.execute(request, responseHandler);
		log.info(responseBody + "\n");
		return responseBody;
	}

	private boolean isNitherNullNorEmpty(String str) {
		return str != null && !str.isEmpty();
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
	
	
	
}
