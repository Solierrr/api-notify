package com.solaria.notify.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.solaria.notify.dto.response.NotificationResponseDTO;
import com.solaria.notify.dto.response.PageResponseDTO;
import com.solaria.notify.dto.response.UnreadCountResponseDTO;
import com.solaria.notify.exception.ResourceNotFoundException;
import com.solaria.notify.model.Notification;
import com.solaria.notify.repository.NotificationRepository;
import com.solaria.notify.security.CurrentUser;

@Service
public class NotificationService {

    static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final MongoTemplate mongoTemplate;
    private final CurrentUser currentUser;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository, MongoTemplate mongoTemplate,
            CurrentUser currentUser, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.mongoTemplate = mongoTemplate;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    // size acima de 100 é limitado a 100 (sem erro); page/size inválidos já são barrados com 400 na borda
    public PageResponseDTO<NotificationResponseDTO> list(int page, int size, boolean unreadOnly) {
        UUID authId = currentUser.authId();
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        Page<Notification> result = unreadOnly
                ? notificationRepository.findByAuthIdAndReadAtIsNullOrderByCreatedAtDesc(authId, pageable)
                : notificationRepository.findByAuthIdOrderByCreatedAtDesc(authId, pageable);
        return new PageResponseDTO<>(result.getContent().stream().map(this::toDto).toList(),
                pageable.getPageNumber(), pageable.getPageSize(), result.getTotalElements());
    }

    public UnreadCountResponseDTO unreadCount() {
        return new UnreadCountResponseDTO(notificationRepository.countByAuthIdAndReadAtIsNull(currentUser.authId()));
    }

    // Notificação de outra pessoa responde 404 igual a inexistente; já lida mantém o readAt original
    public NotificationResponseDTO markRead(UUID id) {
        Notification notification = notificationRepository.findByIdAndAuthId(id, currentUser.authId())
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now(clock));
            notification = notificationRepository.save(notification);
        }
        return toDto(notification);
    }

    public void markAllRead() {
        Query query = new Query(Criteria.where("authId").is(currentUser.authId()).and("readAt").is(null));
        mongoTemplate.updateMulti(query, new Update().set("readAt", Instant.now(clock)), Notification.class);
    }

    private NotificationResponseDTO toDto(Notification n) {
        return new NotificationResponseDTO(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getData(),
                n.getCreatedAt(), n.getReadAt());
    }
}
