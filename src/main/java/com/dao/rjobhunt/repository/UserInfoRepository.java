package com.dao.rjobhunt.repository;

import com.dao.rjobhunt.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserInfoRepository extends MongoRepository<User, String> {

    /** 🔍 Find user by email */
    Optional<User> findByEmail(String email);

    /** 🔍 Find user by public UUID */
    Optional<User> findByPublicId(UUID publicId);

    /** 🔐 Find user by account verification token */
    Optional<User> findByAccountStatus_Token(String token);

    /** 🔍 Text + date-based search across email, phone, gender, DOB, createdAt */
    @Query("{ '$or': [ " +
            "{ 'email': { $regex: ?0, $options: 'i' } }, " +
            "{ 'phoneNumber': { $regex: ?0 } }, " +
            "{ 'gender': { $regex: ?0, $options: 'i' } }, " +
            "{ 'dateOfBirth': ?1 }, " +
            "{ 'createdAt': ?1 } " +
            "] }")
    List<User> searchByTextAndDate(String text, Date parsedDate);

    /** ✅ Users who enabled email notifications */
    List<User> findByNotification_EmailEnabledTrue();

    /** ✅ Users who enabled SMS notifications */
    List<User> findByNotification_SmsEnabledTrue();

    /** ✅ Users who enabled Discord notifications */
    List<User> findByNotification_DiscordEnabledTrue();

    /** 🔢 Count users by public UUID (custom) */
    long countByPublicId(UUID publicId);

    /**
     * ✅ Custom query: Find by publicId + matching job title (preferredJobTitles) + matching address
     * Useful for personalized notification filtering.
     */
    @Query("{ 'publicId': ?0, 'preferredJobTitles': { $regex: ?1, $options: 'i' }, 'address': { $regex: ?2, $options: 'i' } }")
    Optional<User> findByPublicIdAndPreferredJobTitlesRegexAndAddressRegex(UUID publicId, String keyword, String location);
}