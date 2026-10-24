package com.solaria.notify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import com.solaria.notify.dto.response.NotificationResponseDTO;
import com.solaria.notify.dto.response.PageResponseDTO;
import com.solaria.notify.dto.response.UnreadCountResponseDTO;
import com.solaria.notify.exception.ResourceNotFoundException;
import com.solaria.notify.model.Notification;
import com.solaria.notify.model.enums.NotificationType;
import com.solaria.notify.repository.NotificationRepository;
import com.solaria.notify.security.CurrentUser;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");
    private static final Instant EARLIER = Instant.parse("2025-12-01T08:00:00Z");

    @Mock
    NotificationRepository repository;
    @Mock
    MongoTemplate mongoTemplate;
    @Mock
    CurrentUser currentUser;

    NotificationService service;

    UUID userA = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, mongoTemplate, currentUser, Clock.fixed(NOW, ZoneOffset.UTC), null, null);
    }

    private Notification notification(Instant readAt) {
        return new Notification(UUID.randomUUID(), userA, NotificationType.NEW_MESSAGE, "t", "b",
                Map.of("k", "v"), "api-messenger", EARLIER, readAt, EARLIER.plusSeconds(1000));
    }

    @Test
    void listUsesUnreadQueryWhenUnreadOnly() {
        when(currentUser.authId()).thenReturn(userA);
        when(repository.findByAuthIdAndReadAtIsNullOrderByCreatedAtDesc(eq(userA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification(null))));

        PageResponseDTO<NotificationResponseDTO> result = service.list(0, 20, true);

        verify(repository).findByAuthIdAndReadAtIsNullOrderByCreatedAtDesc(eq(userA), any(Pageable.class));
        verify(repository, never()).findByAuthIdOrderByCreatedAtDesc(any(), any());
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(NotificationType.NEW_MESSAGE);
    }

    @Test
    void listUsesFullQueryWhenNotUnreadOnly() {
        when(currentUser.authId()).thenReturn(userA);
        when(repository.findByAuthIdOrderByCreatedAtDesc(eq(userA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification(EARLIER))));

        PageResponseDTO<NotificationResponseDTO> result = service.list(0, 20, false);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listClampsSizeTo100() {
        when(currentUser.authId()).thenReturn(userA);
        when(repository.findByAuthIdOrderByCreatedAtDesc(eq(userA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponseDTO<NotificationResponseDTO> result = service.list(0, 500, false);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByAuthIdOrderByCreatedAtDesc(eq(userA), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(result.getSize()).isEqualTo(100);
    }

    @Test
    void markReadSetsReadAt() {
        Notification n = notification(null);
        when(currentUser.authId()).thenReturn(userA);
        when(repository.findByIdAndAuthId(n.getId(), userA)).thenReturn(Optional.of(n));
        when(repository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        NotificationResponseDTO response = service.markRead(n.getId());

        verify(repository).save(n);
        assertThat(response.getReadAt()).isEqualTo(NOW);
    }

    @Test
    void markReadSetsReadAtOnce() {
        Notification n = notification(EARLIER);
        when(currentUser.authId()).thenReturn(userA);
        when(repository.findByIdAndAuthId(n.getId(), userA)).thenReturn(Optional.of(n));

        NotificationResponseDTO response = service.markRead(n.getId());

        assertThat(response.getReadAt()).isEqualTo(EARLIER);
        verify(repository, never()).save(any());
    }

    @Test
    void markReadOfOtherUsersNotificationThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(currentUser.authId()).thenReturn(userA);
        when(repository.findByIdAndAuthId(id, userA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(id)).isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void unreadCountReturnsRepositoryCount() {
        when(currentUser.authId()).thenReturn(userA);
        when(repository.countByAuthIdAndReadAtIsNull(userA)).thenReturn(7L);

        UnreadCountResponseDTO response = service.unreadCount();

        assertThat(response.getCount()).isEqualTo(7L);
    }

    @Test
    void markAllReadUpdatesOnlyOwnUnreadWithNow() {
        when(currentUser.authId()).thenReturn(userA);

        service.markAllRead();

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongoTemplate).updateMulti(query.capture(), update.capture(), eq(Notification.class));
        Document q = query.getValue().getQueryObject();
        assertThat(q.get("authId")).isEqualTo(userA);
        assertThat(q.containsKey("readAt")).isTrue();
        assertThat(q.get("readAt")).isNull();
        assertThat(update.getValue().getUpdateObject().get("$set", Document.class).get("readAt")).isEqualTo(NOW);
    }
}
