package com.dao.rjobhunt.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.el.stream.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.aggregation.VariableOperators.Let;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dao.rjobhunt.models.ActionHistory;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.others.ActionEntityResolver;
import com.dao.rjobhunt.others.RequestContext;
import com.dao.rjobhunt.repository.ActionHistoryRepository;
import com.dao.rjobhunt.repository.UserInfoRepository;
import com.dao.rjobhunt.Service.ActionHistoryServices;
import java.util.stream.Collectors;

@Service
public class ActionHistoryServices {

	@Autowired
	ActionHistoryRepository actionHistoryRepository;
	
	@Autowired
	private UserInfoRepository infoRepository;
	
	@Autowired
	private MongoTemplate mongoTemplate;

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

	public List<ActionHistory> getActionsByUserId(String userId) {
		return actionHistoryRepository.findByUserId(userId);
	}

	public ActionHistory getActionByPublicId(String publicId) {
		return actionHistoryRepository.findByPublicId(publicId).orElse(null);
	}

	public ActionHistory getActionById(String id) {
		return actionHistoryRepository.findById(id).orElse(null);
	}

	public List<ActionHistory> getAllActions() {
		return actionHistoryRepository.findAll();
	}
	
	public List<ActionHistory> getAllActionsWithUserInfo() {
	    List<ActionHistory> actions = actionHistoryRepository.findAll();

	    // Cache users for fast lookup by userId
	    Map<String, User> userMap = infoRepository.findAll().stream()
	            .collect(Collectors.toMap(User::getUserId, user -> user));

	    // Populate transient fields for user info
	    for (ActionHistory action : actions) {
	        User user = userMap.get(action.getUserId());

	        if (user != null) {
	            action.setUserEmail(user.getEmail());
	            action.setUserRole(user.getRole());
	            action.setUserPublicId(user.getPublicId().toString());
	        } else {
	            // Try to match user by email in description using regex
	        	java.util.Optional<User> matchByEmail = userMap.values().stream()
	        		    .filter(u -> action.getDescription().toLowerCase().contains(u.getEmail().toLowerCase()))
	        		    .findFirst();
	            matchByEmail.ifPresent(u -> {
	                action.setUserEmail(u.getEmail());
	                action.setUserRole(u.getRole());
	                action.setUserPublicId(u.getPublicId().toString());
	            });
	        }
	    }

	    return actions;
	}

	public List<ActionHistory> searchUserActionsFlexible(String keyword, String publicIdStr) {
	    UUID publicId = UUID.fromString(publicIdStr);

	    return infoRepository.findByPublicId(publicId)
	            .map(user -> {
	                List<ActionHistory> result = actionHistoryRepository.searchAcrossFields(keyword, user.getUserId());
	                // Optional: Log or sanitize the result
	                return result;
	            })
	            .orElseThrow(() -> new IllegalArgumentException("User not found for publicId: " + publicIdStr));
	}
	
	
	public List<Document> searchActionsWithUserProjection(String keyword, int page, int size) {
	    int skip = (page - 1) * size;

	    MatchOperation match = Aggregation.match(
	        new Criteria().orOperator(
	            Criteria.where("description").regex(keyword, "i"),
	            Criteria.where("actionEntity").regex(keyword, "i"),
	            Criteria.where("actionType").regex(keyword, "i")
	        )
	    );

	    LookupOperation lookup = Aggregation.lookup("user", "userId", "_id", "userInfo");

	    ProjectionOperation project = Aggregation.project("actionType", "actionEntity", "timestamp", "description")
	        .and("userInfo.email").as("userEmail")
	        .and("userInfo.role").as("userRole");

	    SkipOperation skipOp = Aggregation.skip(skip);
	    LimitOperation limitOp = Aggregation.limit(size);

	    Aggregation agg = Aggregation.newAggregation(match, lookup, project, skipOp, limitOp);
	    return mongoTemplate.aggregate(agg, "actionHistory", Document.class).getMappedResults();
	}
}
