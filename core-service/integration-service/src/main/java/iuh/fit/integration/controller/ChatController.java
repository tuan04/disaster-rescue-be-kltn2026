package iuh.fit.integration.controller;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.integration.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> chat(@RequestParam String prompt) {
        String response = chatService.chat(prompt);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
