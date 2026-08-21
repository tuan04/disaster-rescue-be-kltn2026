package iuh.fit.dispatchservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.dispatchservice.dtos.response.AssignmentResponse;
import iuh.fit.dispatchservice.entity.Assignment;
import iuh.fit.dispatchservice.services.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/rescue-requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<AssignmentResponse>> acceptRescueByLeader(
            @PathVariable UUID requestId,
            @RequestParam UUID leaderId,
            @RequestParam String note
    ) {
        Assignment assignment = assignmentService.acceptRescueByLeader(
                requestId,
                leaderId,
                note
        );
        return ResponseEntity.ok(ApiResponse.success(AssignmentResponse.fromEntity(assignment)));
    }
}
