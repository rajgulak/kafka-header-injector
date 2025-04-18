package com.example.producer.controller;

import com.example.producer.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/")
    public String showForm(Model model) {
        model.addAttribute("topics", messageService.getAvailableTopics());
        return "index";
    }

    @PostMapping("/publish")
    public String publishMessage(@RequestParam String topic,
                               @RequestParam String payload,
                               Model model) {
        Map<String, String> headers = messageService.publishMessage(topic, payload);
        model.addAttribute("headers", headers);
        model.addAttribute("topics", messageService.getAvailableTopics());
        model.addAttribute("message", "Message published successfully!");
        return "index";
    }
} 