package com.dao.rjobhunt.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.bson.Document;

import com.dao.rjobhunt.models.ActionHistory;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.others.ActionEntityResolver;
import com.dao.rjobhunt.others.RequestContext;
import com.dao.rjobhunt.repository.ActionHistoryRepository;
import com.dao.rjobhunt.repository.UserInfoRepository;

@Service
public class ActionHistoryServices {

    @Autowired
    private ActionHistoryRepository actionHistoryRepository;

    @Autowired
    private UserInfoRepository infoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public boolean addActionHistory(String userId, String description) {
        ActionHistory actionHistory = new ActionHistory();

        if (userId == null) {
            userId = "ANONYMOUS";
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
        if (method == null || method.isBlank()) {
            method = "UNKNOWN";
        }

        switch (method.toUpperCase()) {
            case "POST": type = "CREATE"; break;
            case "GET": type = "READ"; break;
            case "PUT":
            case "PATCH": type = "UPDATE"; break;
            case "DELETE": type = "DELETE"; break;
            default: type = "ACTION"; break;
        }
        actionHistory.setActionType(type);

        String entity = ActionEntityResolver.resolve(RequestContext.getRoutePattern());
        actionHistory.setActionEntity(entity);

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

    /**
     * Get all actions ordered by latest timestamp first.
     */
    public List<ActionHistory> getAllActions() {
        return actionHistoryRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    /**
     * Get all actions, enrich with user info, and order by latest timestamp first.
     */
    public List<ActionHistory> getAllActionsWithUserInfo() {
        List<ActionHistory> actions = actionHistoryRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));

        Map<String, User> userMap = infoRepository.findAll().stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));

        for (ActionHistory action : actions) {
            User user = userMap.get(action.getUserId());

            if (user != null) {
                action.setUserEmail(user.getEmail());
                action.setUserRole(user.getRole());
                action.setUserPublicId(user.getPublicId().toString());
            } else {
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
                .map(user -> actionHistoryRepository.searchAcrossFields(keyword, user.getUserId()))
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
