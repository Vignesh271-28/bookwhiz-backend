package com.example.BookWhiz.config.cache;

import com.example.BookWhiz.dto.SeatUpdate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.data.redis.url")
public class SeatLockExpiryListener extends KeyExpirationEventMessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    public SeatLockExpiryListener(
            RedisMessageListenerContainer container,
            SimpMessagingTemplate messagingTemplate
    ) {
        super(container);
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        if (!expiredKey.startsWith("seat-lock:")) {
            return;
        }

        // seat-lock:{showId}:{seatId}
        String[] parts = expiredKey.split(":");
        Long showId = Long.parseLong(parts[1]);
        Long seatId = Long.parseLong(parts[2]);

        messagingTemplate.convertAndSend(
                "/topic/seats/" + showId,
                new SeatUpdate(seatId, "UNLOCKED", null)
        );
    }
}
