package com.dao.rjobhunt.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dao.rjobhunt.models.ActionHistory;

import java.util.List;

@Repository
public interface ActionHistoryRepository extends MongoRepository<ActionHistory, String> {
	List<ActionHistory> findByUserId(String userId);
}