package com.solaria.notify.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.solaria.notify.dto.request.InternalNotificationRequestDTO;
import com.solaria.notify.dto.response.NotificationDispatchResponseDTO;
import com.solaria.notify.dto.response.NotificationResponseDTO;
import com.solaria.notify.dto.response.PageResponseDTO;
import com.solaria.notify.dto.response.UnreadCountResponseDTO;
import com.solaria.notify.exception.ResourceNotFoundException;
import com.solaria.notify.model.Device;
import com.solaria.notify.model.Notification;
import com.solaria.notify.push.PushMessage;
import com.solaria.notify.push.PushResult;
import com.solaria.notify.push.PushSender;
import com.solaria.notify.repository.NotificationRepository;
import com.solaria.notify.security.CurrentUser;

@Service
public class NotificationService {

    static final int MAX_PAGE_SIZE = 100;
    static final int RETENTION_DAYS = 90;

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final MongoTemplate mongoTemplate;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final DeviceService deviceService;
    private final PushSender pushSender;

    public NotificationService(NotificationRepository notificationRepository, MongoTemplate mongoTemplate,
            CurrentUser currentUser, Clock clock, DeviceService deviceService, PushSender pushSender) {
        this.deviceService = deviceService;
        this.pushSender = pushSender;
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
                pageable.getPageNumber(), pageable.getPageSize(), result.getTotalElements(),
                result.getTotalPages());
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

    // Grava sempre; falha no envio nunca derruba a gravação. Sem aparelhos não chama o sender.
    public NotificationDispatchResponseDTO dispatch(InternalNotificationRequestDTO dto) {
        Instant now = Instant.now(clock);
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setAuthId(dto.getAuthId());
        notification.setType(dto.getType());
        notification.setTitle(dto.getTitle());
        notification.setBody(dto.getBody());
        notification.setData(dto.getData());
        notification.setSourceService(currentUser.serviceName());
        notification.setCreatedAt(now);
        notification.setExpireAt(now.plus(RETENTION_DAYS, ChronoUnit.DAYS));
        Notification saved = notificationRepository.save(notification);

        List<Device> devices = deviceService.findByAuthId(dto.getAuthId());
        if (devices.isEmpty()) {
            return new NotificationDispatchResponseDTO(saved.getId(), 0, 0, 0);
        }

        List<String> tokens = devices.stream().map(Device::getFcmToken).toList();
        PushMessage message = new PushMessage(saved.getId(), saved.getType(), saved.getTitle(), saved.getBody(),
                saved.getData());
        PushResult result;
        try {
            result = pushSender.send(tokens, message);
        } catch (RuntimeException e) {
            log.error("Falha ao enviar push da notificação {}", saved.getId(), e);
            return new NotificationDispatchResponseDTO(saved.getId(), 0, tokens.size(), 0);
        }

        // tokens inválidos voltam ao aparelho para remoção; falha na limpeza não derruba a resposta
        Set<String> invalid = Set.copyOf(result.invalidTokens());
        List<Device> stale = devices.stream().filter(d -> invalid.contains(d.getFcmToken())).toList();
        int removed = 0;
        if (!stale.isEmpty()) {
            try {
                removed = deviceService.removeStale(stale);
            } catch (RuntimeException e) {
                log.error("Falha ao remover aparelhos com token inválido da notificação {}: {}", saved.getId(),
                        e.getClass().getSimpleName());
            }
        }
        return new NotificationDispatchResponseDTO(saved.getId(), result.sent(), result.failed(), removed);
    }

    private NotificationResponseDTO toDto(Notification n) {
        return new NotificationResponseDTO(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getData(),
                n.getCreatedAt(), n.getReadAt());
    }
}
