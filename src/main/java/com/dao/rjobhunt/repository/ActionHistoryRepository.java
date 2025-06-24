package com.dao.rjobhunt.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.dao.rjobhunt.models.ActionHistory;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActionHistoryRepository extends MongoRepository<ActionHistory, String> {
	List<ActionHistory> findByUserId(String userId);

	Optional<ActionHistory> findByPublicId(String publicId);
	
	@Query("{ $and: [ { userId: ?1 }, { $or: [ " +
	        "{ description: { $regex: ?0, $options: 'i' } }, " +
	        "{ actionType: { $regex: ?0, $options: 'i' } }, " +
	        "{ actionEntity: { $regex: ?0, $options: 'i' } }, " +
	        "{ ipAddress: { $regex: ?0, $options: 'i' } }, " +
	        "{ deviceInfo: { $regex: ?0, $options: 'i' } } " +
	    "] } ] }")
	List<ActionHistory> searchAcrossFields(String keyword, String userId);
}