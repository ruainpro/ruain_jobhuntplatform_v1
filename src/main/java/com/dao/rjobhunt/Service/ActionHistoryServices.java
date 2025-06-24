package com.dao.rjobhunt.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.dao.rjobhunt.models.ActionHistory;
import com.dao.rjobhunt.others.ActionEntityResolver;
import com.dao.rjobhunt.others.RequestContext;
import com.dao.rjobhunt.repository.ActionHistoryRepository;
import com.dao.rjobhunt.repository.UserInfoRepository;

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
