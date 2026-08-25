package iuh.fit.notificationservice.service;

import iuh.fit.notificationservice.dtos.SOSResponse;

public interface NotificationService {
    /**
     * Phân phối thông báo cứu hộ SOS cho các cứu hộ viên xung quanh theo bán kính
     *
     * @param event Thông tin sự kiện SOS nhận từ Kafka
     */
    void processSOSEvent(SOSResponse event);
}
