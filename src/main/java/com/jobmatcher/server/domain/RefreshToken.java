package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String token;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="user_id", nullable = false, unique = true)
    private User user;

    @Column(name="expiry_date", nullable = false)
    private LocalDateTime expiryDate;
}
