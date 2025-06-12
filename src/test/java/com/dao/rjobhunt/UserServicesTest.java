//package com.dao.rjobhunt;
//
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.Date;
//import java.util.Optional;
//import java.util.UUID;
//
//import com.dao.rjobhunt.Service.UserServices;
//import com.dao.rjobhunt.dto.UserDto;
//import com.dao.rjobhunt.models.AccountStatus;
//import com.dao.rjobhunt.models.User;
//import com.dao.rjobhunt.repository.UserInfoRepository;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//public class UserServicesTest {
//
//    @Mock
//    private UserInfoRepository userInfoRepository;
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @InjectMocks
//    private UserServices userService;
//
//    private User userInput;
//
//    @BeforeEach
//    public void setup() {
//        MockitoAnnotations.openMocks(this);
//
//        userInput = new User();
//        userInput.setEmail("test2@example.com");
//        userInput.setPassword("plainPassword");
//        userInput.setPhoneNumber("1234567890");
//        userInput.setGender("Male");
//        
//
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//		Date dob;
//		try {
//			dob = sdf.parse("1995-08-17");
//	        userInput.setDateOfBirth(dob);
//
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}  // this can throw ParseException
//
//        userInput.setAddress("Test Address");
//    }
//
//    @Test
//    public void testRegisterUser_Successful() {
//        when(userInfoRepository.findByEmail("test2@example.com")).thenReturn(Optional.empty());
//        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
//        when(userInfoRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        UserDto result = userService.registerUser(userInput);
//
//        assertNotNull(result);
//        assertEquals("test2@example.com", result.getEmail());
//        assertNotNull(result.getCreatedAt());
//        assertNotNull(result.getAccountStatus());
//        assertEquals("encodedPassword", userInput.getPassword());
//        assertEquals(0, result.getAccountStatus().getStatusId());
//        assertNotNull(result.getAccountStatus().getToken());
//    }
//
//    @Test
//    public void testRegisterUser_EmailAlreadyExists() {
//        when(userInfoRepository.findByEmail("test2@example.com"))
//                .thenReturn(Optional.of(new User()));
//
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
//                () -> userService.registerUser(userInput));
//
//        assertEquals("Email already exists", exception.getMessage());
//    }
//
//    @Test
//    public void testRegisterUser_EncodedPassword() {
//        when(userInfoRepository.findByEmail("test2@example.com")).thenReturn(Optional.empty());
//        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
//        when(userInfoRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        UserDto result = userService.registerUser(userInput);
//
//        assertEquals("encodedPassword", userInput.getPassword());
//        assertNotNull(result.getAccountStatus().getToken());
//    }
//}
