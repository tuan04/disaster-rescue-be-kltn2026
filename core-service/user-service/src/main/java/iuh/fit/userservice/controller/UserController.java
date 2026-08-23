package iuh.fit.userservice.controller;


import iuh.fit.common.response.ApiResponse;
import iuh.fit.userservice.dto.request.UpgradeRescuerRequest;
import iuh.fit.userservice.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @PutMapping("/upgrade-rescuer")
    public ResponseEntity<ApiResponse<?>> upgradeToRescuer(@Validated @RequestBody UpgradeRescuerRequest request) {
       return ResponseEntity.ok(ApiResponse.success(userService.upgradeToRescuer(request)));
    }
}
