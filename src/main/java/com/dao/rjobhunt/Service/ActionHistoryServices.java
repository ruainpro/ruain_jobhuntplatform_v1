package com.dao.rjobhunt.Service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dao.rjobhunt.models.ActionHistory;
import com.dao.rjobhunt.others.ActionEntityResolver;
import com.dao.rjobhunt.others.RequestContext;
import com.dao.rjobhunt.repository.ActionHistoryRepository;

@Service
public class ActionHistoryServices {

	@Autowired
	ActionHistoryRepository actionHistoryRepository;

	public boolean addActionHistory(String userId, String description) {

		ActionHistory actionHistory = new ActionHistory();

		if (userId == null) {
			userId = "ANONYMOUS"; // or resolve from SecurityContext if available
		}

		actionHistory.setUserId(userId);
		actionHistory.setDescription(description);

		actionHistory.setTimestamp(Instant.now());

		if (actionHistory.getPublicId() == null) {
			actionHistory.setPublicId(UUID.randomUUID().toString());
		}

		actionHistory.setIpAddress(RequestContext.getIp());
		actionHistory.setDeviceInfo(RequestContext.getUserAgent());

		String method = RequestContext.getHttpMethod();
		String type;

		switch (method) {
		    case "POST":
		        type = "CREATE";
		        break;
		    case "GET":
		        type = "READ";
		        break;
		    case "PUT":
		    case "PATCH":
		        type = "UPDATE";
		        break;
		    case "DELETE":
		        type = "DELETE";
		        break;
		    default:
		        type = "ACTION";
		}
		actionHistory.setActionType(type);

		// Set actionEntity from matched route like "/auth/login" → "AUTH"
		String entity = ActionEntityResolver.resolve(RequestContext.getRoutePattern());
		actionHistory.setActionEntity(entity);

		// Optionally, add controller method info to description
		if (actionHistory.getDescription() == null) {
			actionHistory.setDescription("Action triggered from: " + RequestContext.getControllerMethod());
		}

		return actionHistoryRepository.save(actionHistory) != null;
	}

}
