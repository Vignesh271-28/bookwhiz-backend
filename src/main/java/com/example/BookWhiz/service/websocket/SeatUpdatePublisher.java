package com.example.BookWhiz.service.websocket;

import com.example.BookWhiz.dto.response.SeatUpdateMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeatUpdatePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public SeatUpdatePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastSeatUpdate(SeatUpdateMessage message) {
        messagingTemplate.convertAndSend(
                "/topic/seats/" + message.getEventId(),
                message
        );
    }
}

