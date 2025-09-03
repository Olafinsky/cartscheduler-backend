package org.cartscheduler.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Entity
@Getter
@Setter
@Table(name = "participant_access_tokens")
public class ParticipantAccessToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "token")
    @NotBlank
    private String token;

    @Column(name = "created_at")
    @NotBlank
    @CreationTimestamp
    private Date createdAt;

    @Column(name = "expires_at")
    @NotBlank
    private Date expiresAt;

    @ManyToOne
    @JoinColumn(name = "participant_id")
    private Participant participant;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;
}
