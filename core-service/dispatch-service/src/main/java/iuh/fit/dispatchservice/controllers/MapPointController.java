package iuh.fit.dispatchservice.controllers;

import iuh.fit.dispatchservice.entity.MapPoint;
import iuh.fit.dispatchservice.services.MapPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/map-points")
@RequiredArgsConstructor

public class MapPointController {
    private final MapPointService mapPointService;

    @GetMapping
    public ResponseEntity<List<MapPoint>> getAllMapPoints() {
        return ResponseEntity.ok(mapPointService.getAllMapPoints());
    }
}
