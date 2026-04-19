package com.bookshelf.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_participants")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventParticipant {

    @EmbeddedId
    private EventParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventId")
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(name = "registered_at", updatable = false)
    private OffsetDateTime registeredAt;

    @Embeddable
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class EventParticipantId implements Serializable {
        @Column(name = "event_id")
        private UUID eventId;

        @Column(name = "user_id")
        private UUID userId;
    }
}
