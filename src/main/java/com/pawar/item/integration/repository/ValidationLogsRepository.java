package com.pawar.item.integration.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pawar.item.integration.entity.ValidationLogs;


@Repository
public interface ValidationLogsRepository extends CrudRepository<ValidationLogs, Integer>{
	
	@Query("SELECT vl FROM ValidationLogs vl WHERE vl.validationType = :validationType AND vl.status = :status")
	List<ValidationLogs> findByValidationTypeAndStatus(@Param("validationType") String validationType, @Param("status") Integer status);
	
	@Query("SELECT DISTINCT vl.asnBrcd FROM ValidationLogs vl WHERE vl.validationType = :validationType AND vl.status = :status")
	List<ValidationLogs> findDistinctItemNameByValidationTypeAndStatus(@Param("validationType") String validationType, @Param("status") Integer status);

	List<ValidationLogs> findByItemName(String asnBrcd);

}
