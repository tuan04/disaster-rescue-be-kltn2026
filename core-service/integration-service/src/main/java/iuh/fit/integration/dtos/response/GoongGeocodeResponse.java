package iuh.fit.integration.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoongGeocodeResponse(List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            String name,
            String formatted_address,
            String address,
            String deprecated_description) {
    }
}
