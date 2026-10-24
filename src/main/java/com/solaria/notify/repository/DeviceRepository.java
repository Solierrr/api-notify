package com.solaria.notify.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.solaria.notify.model.Device;

public interface DeviceRepository extends MongoRepository<Device, String> {

    List<Device> findByAuthId(UUID authId);

    long deleteByInstallationIdAndFcmToken(String installationId, String fcmToken);
}
