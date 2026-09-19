package com.prizm.realtime;

import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class SpaceEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public SpaceEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void artifactAdded(Long spaceId, Map<String, Object> artifact) {
        send(spaceId, Map.of("type", "ARTIFACT_ADDED", "artifact", artifact));
    }

    public void groupsUpdated(Long spaceId, Long groupId) {
        send(spaceId, Map.of("type", "GROUPS_UPDATED", "groupId", groupId));
    }

    public void memberJoined(Long spaceId, String nickname, String major) {
        send(spaceId, Map.of("type", "MEMBER_JOINED", "nickname", nickname, "major", major));
    }

    private void send(Long spaceId, Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/space/" + spaceId, payload);
    }
}
