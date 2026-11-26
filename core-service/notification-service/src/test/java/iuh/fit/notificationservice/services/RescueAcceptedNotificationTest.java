package iuh.fit.notificationservice.services;

import iuh.fit.common.kafka.dto.RescueAcceptedEvent;
import iuh.fit.common.kafka.dto.RescueRejectedEvent;
import iuh.fit.notificationservice.kafka.consumer.RescueRejectedEventConsumer;
import iuh.fit.notificationservice.client.ResourceTeamGrpcClient;
import iuh.fit.notificationservice.dtos.TeamRecipientsSnapshot;
import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.entity.UserNotification;
import iuh.fit.notificationservice.kafka.consumer.RescueEventConsumer;
import iuh.fit.notificationservice.repositories.NotificationRepository;
import iuh.fit.notificationservice.repositories.UserNotificationRepository;
import iuh.fit.notificationservice.socket.WebSocketNotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RescueAcceptedNotificationTest {
    private final RescueAcceptedEvent event = new RescueAcceptedEvent(UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), "Đội A", "0900000000", Instant.now());

    @Test
    void savesTeamRecipientsAndSendsOnlyAfterCommit() {
        verifyTeamPersistence(false);
    }

    @Test
    void savesRejectedNotificationForAllTeamMembersAfterCommit() {
        verifyTeamPersistence(true);
    }

    private RescueRejectedEvent rejectedEvent(String reason) {
        return new RescueRejectedEvent(event.assignmentId(), event.requestId(), event.teamId(),
                event.teamName(), reason, Instant.now());
    }

    private void verifyTeamPersistence(boolean rejected) {
        var recipients = mock(ResourceTeamGrpcClient.class);
        var notifications = mock(NotificationRepository.class);
        var users = mock(UserNotificationRepository.class);
        var socket = mock(WebSocketNotificationService.class);
        var service = new RescueAssignmentNotificationService(recipients, notifications, users, socket);
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        when(recipients.findRecipientsByTeamIds(List.of(event.teamId()))).thenReturn(Map.of(event.teamId(),
                new TeamRecipientsSnapshot(event.teamId(), true, List.of(leader, member, leader))));
        when(notifications.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(UUID.randomUUID());
            return notification;
        });
        TransactionSynchronizationManager.initSynchronization();
        try {
            if (rejected) {
                service.notifyTeamRejected(rejectedEvent("Không đủ nhân lực"));
            } else {
                service.notifyTeamAccepted(event);
            }
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<UserNotification>> saved = ArgumentCaptor.forClass(List.class);
            verify(users).saveAll(saved.capture());
            assertEquals(List.of(leader, member), saved.getValue().stream().map(UserNotification::getUserId).toList());
            assertEquals(rejected ? "RESCUE_REJECTED" : "RESCUE_ACCEPTED", saved.getValue().getFirst().getNotification().getType());
            if (rejected) {
                assertTrue(saved.getValue().getFirst().getNotification().getContent().contains("Không đủ nhân lực"));
            }
            assertEquals(event.requestId(), saved.getValue().getFirst().getNotification().getReferenceId());
            verifyNoInteractions(socket);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(socket).sendToUser(eq(leader), any());
            verify(socket).sendToUser(eq(member), any());
            verifyNoMoreInteractions(socket);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rejectionOnlyBroadcastsOperationsWithoutChangingSosStatus() {
        var socket = mock(WebSocketNotificationService.class);
        var teamNotifications = mock(RescueAssignmentNotificationService.class);
        var rejected = rejectedEvent("Không đủ nhân lực");
        new RescueRejectedEventConsumer(teamNotifications, socket).consume(rejected);
        verify(teamNotifications).notifyTeamRejected(rejected);
        verifyNoMoreInteractions(teamNotifications);
        verify(socket).broadcast(eq(RescueEventConsumer.OPERATIONS_TOPIC), argThat(payload ->
                payload instanceof iuh.fit.notificationservice.dtos.NotificationSocketMessage message
                        && "RESCUE_REJECTED".equals(message.type())
                        && message.content().contains("Không đủ nhân lực")
                        && event.requestId().equals(message.referenceId())));
        verifyNoMoreInteractions(socket);
    }

    @Test
    void rejectionWithoutReasonOmitsReasonText() {
        String expected = "Đội Đội A đã từ chối nhiệm vụ cứu hộ.";
        assertEquals(expected, RescueAssignmentNotificationService.rejectionContent(rejectedEvent(null)));
        assertEquals(expected, RescueAssignmentNotificationService.rejectionContent(rejectedEvent("  ")));
    }

    @Test
    void broadcastsOperationsWithoutCreatingAdminRecipientsAndPreservesStatusChannel() {
        var socket = mock(WebSocketNotificationService.class);
        var teamNotifications = mock(RescueAssignmentNotificationService.class);
        new RescueEventConsumer(socket, teamNotifications).consumeRescueAcceptedEvent(event);
        verify(teamNotifications).notifyTeamAccepted(event);
        verifyNoMoreInteractions(teamNotifications);
        verify(socket).broadcast("/topic/rescue-requests/" + event.requestId() + "/status", event);
        verify(socket).broadcast(eq(RescueEventConsumer.OPERATIONS_TOPIC), argThat(payload ->
                payload instanceof iuh.fit.notificationservice.dtos.NotificationSocketMessage message
                        && "RESCUE_ACCEPTED".equals(message.type())
                        && event.requestId().equals(message.referenceId())));
        verifyNoMoreInteractions(socket);
    }
}
