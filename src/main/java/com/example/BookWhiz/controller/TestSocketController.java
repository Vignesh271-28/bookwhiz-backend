package com.example.BookWhiz.controller;


import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public TestSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/api/test/ws/{showId}")
    public String testWs(@PathVariable Long showId) {
        messagingTemplate.convertAndSend(
                "/topic/seats/" + showId,
                "TEST_MESSAGE"
        );
        return "sent to show " + showId;
    }
}