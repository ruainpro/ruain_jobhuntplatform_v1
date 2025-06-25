package com.dao.rjobhunt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.text.SimpleDateFormat;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.dao.rjobhunt.Service.UserServices;
import com.dao.rjobhunt.dto.UserDto;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.repository.UserInfoRepository;
public class UserServices_ForProfileTest {


    @Mock
    private UserInfoRepository userInfoRepository;

    @InjectMocks
    private UserServices userServices;

    private UUID testPublicId;
    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testPublicId = UUID.randomUUID();
        testUser = new User();
        testUser.setPublicId(testPublicId);
        testUser.setEmail("test@example.com");
    }

    @Test
    void testGetUserByPublicId() {
        when(userInfoRepository.findByPublicId(testPublicId)).thenReturn(Optional.of(testUser));

        Optional<User> result = userServices.getUserByPublicId(testPublicId);

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void testGetAllUsers() {
        List<User> userList = List.of(testUser);
        when(userInfoRepository.findAll()).thenReturn(userList);

        List<User> result = userServices.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
    }

    @Test
    void testUpdateUserByPublicId() {
        UserDto dto = new UserDto();
        dto.setEmail("new@example.com");
        dto.setAddress("New Street");

        when(userInfoRepository.findByPublicId(testPublicId)).thenReturn(Optional.of(testUser));
        when(userInfoRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userServices.updateUserByPublicId(testPublicId, dto);

        assertEquals("new@example.com", result.getEmail());
        assertEquals("New Street", result.getAddress());
    }

    @Test
    void testSearchByTextAndDate() throws Exception {
        String query = "test";
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2024-01-01");
        List<User> expected = List.of(testUser);

        when(userInfoRepository.searchByTextAndDate(query, date)).thenReturn(expected);

        List<User> result = userInfoRepository.searchByTextAndDate(query, date);

        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
    }
}
