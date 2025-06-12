package com.dao.rjobhunt.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dao.rjobhunt.models.User;

@Repository
public interface UserInfoRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email); // Assumes 'email' is a field in UserInfo

	Optional<User> findByPublicId(UUID publicId);

	Optional<User> findByAccountStatus_Token(String token);
}