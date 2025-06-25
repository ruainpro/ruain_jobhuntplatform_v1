package com.dao.rjobhunt.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.dao.rjobhunt.models.User;

@Repository
public interface UserInfoRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email); // Assumes 'email' is a field in UserInfo

	Optional<User> findByPublicId(UUID publicId);

	Optional<User> findByAccountStatus_Token(String token);
	
	@Query("{ '$or': [ " +
		       "{ 'email': { $regex: ?0, $options: 'i' } }, " +
		       "{ 'phoneNumber': { $regex: ?0 } }, " +
		       "{ 'gender': { $regex: ?0, $options: 'i' } }, " +
		       "{ 'dateOfBirth': ?1 }, " +
		       "{ 'createdAt': ?1 } " +
		       "] }")
		List<User> searchByTextAndDate(String text, Date parsedDate);
}