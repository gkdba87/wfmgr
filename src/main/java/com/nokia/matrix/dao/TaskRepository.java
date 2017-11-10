/**
 * 
 */
package com.nokia.matrix.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.nokia.matrix.entity.Task;

/**
 * @author 1226211
 *
 */

@RepositoryRestResource
public interface TaskRepository extends CrudRepository<Task, Long>{
	
	/**
	 * @param id
	 * @param taskName
	 * @return
	 */
	@Query("from Task t where t.workflow.id=:id and t.taskName=:taskName")
	public  Task findTaskByName(@Param("id") Long id, @Param("taskName") String taskName);
	
	@Query("from Task t where t.workflow.id=:workflowId")
	public List<Task>  findAllTasksByWorkflowId(@Param("workflowId") Long workflowId);
	
	@Query("delete from Task t where t.id in :taskIdsForDelete")
	void deleteUnMappedTasks1(@Param("taskIdsForDelete") List<Long> taskIdsForDelete);
	
	@Query("delete from Task t where t.id=:taskIdsForDelete")
	void deleteUnMappedTasks(@Param("taskIdsForDelete") Long taskIdsForDelete);
	
	@Query("select t.workflow.projectId, t.taskName , count(t.taskName) from Task t "
			+ " group by t.taskName , t.workflow.projectId order by t.workflow.projectId")
	public List<Object[]> getTaskReports();

	
	/*@Query("select wf.projectId, t.taskName , count(t.task_name) from workflow.workflow wf join workflow.task t on "
			+ "wf.id = t.workflow_id group by t.task_name , wf.project_id order by wf.project_id")
	void getResult();*/

	
}
