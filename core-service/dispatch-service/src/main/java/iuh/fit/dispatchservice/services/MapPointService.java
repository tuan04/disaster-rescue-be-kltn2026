package iuh.fit.dispatchservice.services;

import iuh.fit.dispatchservice.entity.MapPoint;
import iuh.fit.dispatchservice.repositories.MapPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class MapPointService {
    private final MapPointRepository mapPointRepository;

    public List<MapPoint> getAllMapPoints() {
        return mapPointRepository.findAll();
    }
}
