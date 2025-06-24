package com.dao.rjobhunt;
import com.dao.rjobhunt.Service.ActionHistoryServices;
import com.dao.rjobhunt.models.ActionHistory;
import com.dao.rjobhunt.repository.ActionHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.dao.rjobhunt.others.RequestContext;

import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ActionHistoryServicesTest {

    @Mock
    private ActionHistoryRepository actionHistoryRepository;

    @InjectMocks
    private ActionHistoryServices actionHistoryServices;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAddActionHistory_shouldSetCorrectActionType() {
        String userId = "testUserId";
        String description = "Testing POST method";

        try (MockedStatic<RequestContext> mocked = mockStatic(RequestContext.class)) {
            mocked.when(RequestContext::getHttpMethod).thenReturn("POST");

            when(actionHistoryRepository.save(any(ActionHistory.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            boolean result = actionHistoryServices.addActionHistory(userId, description);

            assertTrue(result);
            verify(actionHistoryRepository, times(1)).save(argThat(action -> 
                "CREATE".equals(action.getActionType())
            ));
        }
    }
    
}
