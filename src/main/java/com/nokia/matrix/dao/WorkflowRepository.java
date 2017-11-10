/**
 * 
 */
package com.nokia.matrix.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.nokia.matrix.entity.Workflow;

/**
 * @author 1226211
 *
 */

@RepositoryRestResource
public interface WorkflowRepository extends CrudRepository<Workflow, Long>{
	
	@Query("select w.id, w.name from Workflow w where w.userName=:userName and w.projectId=:projectId")
	public List<Object[]> findUserWorkflows(@Param("userName") String userName, @Param("projectId") Integer projectId);
	
	@Query("select distinct w.projectId, w.projectName from Workflow w where w.userName=:userName")
	public List<Object[]> findUserProjects(@Param("userName") String userName);
	
}
