package com.example.BookWhiz.service;

import com.example.BookWhiz.model.notification.Notification;
import com.example.BookWhiz.model.notification.Notification.NotifType;
import com.example.BookWhiz.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final SimpMessagingTemplate   ws;

    public NotificationService(NotificationRepository repo, SimpMessagingTemplate ws) {
        this.repo = repo;
        this.ws   = ws;
    }

    /**
     * Send a role-broadcast notification (e.g. all SUPER_ADMINs).
     */
    public void notifyRole(NotifType type, String recipientRole, String title, String message) {
        Notification n = new Notification();
        n.setType(type);
        n.setRecipientRole(recipientRole);
        n.setTitle(title);
        n.setMessage(message);
        repo.save(n);

        // Push via WebSocket to /topic/notifications/{role}
        send("/topic/notifications/" + recipientRole, n);
    }

    /**
     * Send a personal notification to a specific user by email.
     */
    public void notifyUser(NotifType type, String recipientEmail, String title, String message) {
        Notification n = new Notification();
        n.setType(type);
        n.setRecipientEmail(recipientEmail);
        n.setTitle(title);
        n.setMessage(message);
        repo.save(n);

        // Push via WebSocket to /topic/notifications/user/{email}
        String safeTopic = "/topic/notifications/user/" + recipientEmail.replace("@", "_at_");
        send(safeTopic, n);
    }

    private java.util.Map<String, Object> buildPayload(Notification n) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id",        n.getId());
        map.put("type",      n.getType());
        map.put("title",     n.getTitle());
        map.put("message",   n.getMessage());
        map.put("read",      n.isRead());
        map.put("createdAt", n.getCreatedAt().toString());
        return map;
    }

    private void send(String destination, Notification n) {
        java.util.Map<String, Object> payload = buildPayload(n);
        ws.convertAndSend(destination, (Object) payload);
    }
}