package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateTeamRequest(
        List<String> vehicles,

        @Size(max = 255, message = "Tên đội không được vượt quá 255 ký tự")
        String teamName
) {
}
