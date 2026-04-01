package com.example.BookWhiz.service.seat;

import com.example.BookWhiz.dto.SeatUpdate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class SeatLockService {

    // 🔒 Seat lock TTL
    private static final Duration LOCK_TTL = Duration.ofMinutes(3);

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final DefaultRedisScript<Long> atomicLockScript;

    // 🔥 Lua script for ATOMIC multi-seat lock
    private static final String ATOMIC_LOCK_LUA = """
        for i = 1, #KEYS do
            if redis.call("EXISTS", KEYS[i]) == 1 then
                return 0
            end
        end
        for i = 1, #KEYS do
            redis.call("SET", KEYS[i], ARGV[1], "EX", ARGV[2])
        end
        return 1
    """;

    public SeatLockService(
            StringRedisTemplate redisTemplate,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;

        this.atomicLockScript = new DefaultRedisScript<>();
        this.atomicLockScript.setScriptText(ATOMIC_LOCK_LUA);
        this.atomicLockScript.setResultType(Long.class);
    }

    // Redis key format
    private String key(Long showId, Long seatId) {
        return "seat-lock:" + showId + ":" + seatId;
    }

    /* ============================================================
       🔒 ATOMIC MULTI-SEAT LOCK (USE THIS FOR BOOKING)
       ============================================================ */
    public boolean lockSeatsAtomically(
            Long showId,
            List<Long> seatIds,
            Long userId
    ) {

        List<String> keys = seatIds.stream()
                .map(seatId -> key(showId, seatId))
                .toList();

        Long result = redisTemplate.execute(
                atomicLockScript,
                keys,
                userId.toString(),
                String.valueOf(LOCK_TTL.toSeconds())
        );

        if (result != null && result == 1) {
            // 🔔 Broadcast LOCKED for all seats
            seatIds.forEach(seatId ->
                    messagingTemplate.convertAndSend(
                            "/topic/seats/" + showId,
                            new SeatUpdate(seatId, "LOCKED", userId)
                    )
            );
            return true;
        }

        return false;
    }

    /* ============================================================
       🔒 SINGLE-SEAT LOCK (OPTIONAL / NON-BOOKING USE)
       ============================================================ */
    public boolean lockSeat(Long showId, Long seatId, Long userId) {

        String redisKey = key(showId, seatId);

        // Same user re-lock → refresh TTL
        String existing = redisTemplate.opsForValue().get(redisKey);
        if (existing != null && existing.equals(userId.toString())) {
            redisTemplate.expire(redisKey, LOCK_TTL);
            return true;
        }

        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, userId.toString(), LOCK_TTL);

        if (Boolean.TRUE.equals(locked)) {
            messagingTemplate.convertAndSend(
                    "/topic/seats/" + showId,
                    new SeatUpdate(seatId, "LOCKED", userId)
            );
            return true;
        }

        return false;
    }

    /* ============================================================
       🔓 UNLOCK SEATS (CONFIRM / CANCEL / EXPIRE)
       ============================================================ */
    public void unlockSeats(Long showId, List<Long> seatIds) {

        for (Long seatId : seatIds) {
            redisTemplate.delete(key(showId, seatId));

            messagingTemplate.convertAndSend(
                    "/topic/seats/" + showId,
                    new SeatUpdate(seatId, "UNLOCKED", null)
            );
        }
    }

    /* ============================================================
       🔍 CHECK IF SEAT IS LOCKED (HELPER)
       ============================================================ */
    public boolean isSeatLocked(Long showId, Long seatId) {
        return redisTemplate.opsForValue()
                .get(key(showId, seatId)) != null;
    }
}