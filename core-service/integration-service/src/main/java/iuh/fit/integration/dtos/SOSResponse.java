package iuh.fit.integration.dtos;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SOSResponse {
    private UUID id;
    private String reporterPhone;
    private String emergencyLevel;
    private String content;
    private String status;
    private String source;
    private double latitude;
    private double longitude;
}
