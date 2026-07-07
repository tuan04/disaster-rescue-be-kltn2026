package iuh.fit.dispatchservice.services;

import iuh.fit.dispatchservice.dtos.MapPointRes;
import iuh.fit.dispatchservice.entity.MapPoint;
import iuh.fit.dispatchservice.repositories.MapPointRepository;
import iuh.fit.dispatchservice.utils.MapPointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class MapPointService {
    private final MapPointRepository mapPointRepository;
    private final MapPointMapper mapPointMapper;

    public List<MapPointRes> getAllMapPoints() {
        return mapPointMapper.toResDTO(mapPointRepository.findAll());
    }
}
