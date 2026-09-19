package iuh.fit.dispatchservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.dispatchservice.dtos.request.CancelAssignmentRequest;
import iuh.fit.dispatchservice.dtos.request.RescueAssignmentRequest;
import iuh.fit.dispatchservice.dtos.response.AssignmentResponse;
import iuh.fit.dispatchservice.entity.Assignment;
import iuh.fit.dispatchservice.services.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
            @RequestParam String note) {
        Assignment assignment = assignmentService.acceptRescueByLeader(
            requestId,
            leaderId,
            note);
        return ResponseEntity.ok(ApiResponse.success(AssignmentResponse.fromEntity(assignment)));
    }


    @PostMapping("/rescue-requests/{requestId}/assign")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> assignRescueByLeader(
            @PathVariable UUID requestId,
            @RequestBody @Valid List<RescueAssignmentRequest> assignmentRequests
    ) {
        List<Assignment> assignments = assignmentService.assignRescueByTeam(
                requestId,
                assignmentRequests
        );
        return ResponseEntity.ok(ApiResponse.success(assignments.stream().map(AssignmentResponse::fromEntity).toList()));
    }


    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getAssignmentByRequestId(
            @PathVariable("requestId") UUID requestId) {
        List<AssignmentResponse> response = assignmentService.getAssignmentByRequestId(requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @GetMapping("/teams/{teamId}/active")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getActiveMissionByTeam(
            @PathVariable(value = "teamId") UUID teamId) {
        AssignmentResponse response = assignmentService.getActiveMissionByTeam(teamId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{assignmentId}/complete")
    public ResponseEntity<Void> complete(
            @PathVariable UUID assignmentId,
            @RequestHeader(value = "X-User-Id") UUID leaderId) {
        assignmentService.complete(assignmentId, leaderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{assignmentId}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID assignmentId,
            @RequestHeader(value = "X-User-Id") UUID leaderId,
            @Valid @RequestBody CancelAssignmentRequest request) {
        assignmentService.cancel(assignmentId, leaderId, request.reason());
        return ResponseEntity.noContent().build();
    }
}
