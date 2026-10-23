package com.solaria.notify.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.solaria.notify.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, UUID> {

    Page<Notification> findByAuthIdOrderByCreatedAtDesc(UUID authId, Pageable pageable);

    Page<Notification> findByAuthIdAndReadAtIsNullOrderByCreatedAtDesc(UUID authId, Pageable pageable);

    long countByAuthIdAndReadAtIsNull(UUID authId);

    Optional<Notification> findByIdAndAuthId(UUID id, UUID authId);
}
