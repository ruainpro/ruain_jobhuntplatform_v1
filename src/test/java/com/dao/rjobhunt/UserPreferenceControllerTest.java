package com.dao.rjobhunt;

import com.dao.rjobhunt.Service.UserServices;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class UserPreferenceControllerTest {

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private UserServices userServices;

    private UUID publicId;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        publicId = UUID.fromString("cf49beb7-1d16-e160-206c-01783b6304a0");

        user = new User();
        user.setPublicId(publicId);
        user.setPreferredJobTitles(new ArrayList<>(List.of("software", "developer")));
    }

    @Test
    void testAddPreferredJobTitlesDynamically_addNewTitle() {
        when(userInfoRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));

        List<String> updated = userServices.addPreferredJobTitlesDynamically(publicId, List.of("engineer"));

        assertThat(updated).contains("software", "developer", "engineer");
        verify(mongoTemplate).updateFirst(any(), any(Update.class), eq(User.class));
    }

    @Test
    void testDeletePreferredJobTitlesDynamically_deleteSpecific() {
        when(userInfoRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));

        List<String> updated = userServices.deletePreferredJobTitlesDynamically(publicId, "developer");

        assertThat(updated).containsExactly("software");
        verify(mongoTemplate).updateFirst(any(), any(Update.class), eq(User.class));
    }

    @Test
    void testDeletePreferredJobTitlesDynamically_deleteAll() {
        when(userInfoRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));

        List<String> updated = userServices.deletePreferredJobTitlesDynamically(publicId, null);

        assertThat(updated).isEmpty();
        verify(mongoTemplate).updateFirst(any(), any(Update.class), eq(User.class));
    }
}
