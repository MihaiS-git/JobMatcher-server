package com.jobmatcher.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, exclude = {"password", "refreshToken"})
@ToString
@Table(name = "users")
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "account_non_expired", nullable = false)
    private boolean accountNonExpired = true;

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    @Column(name = "credentials_non_expired", nullable = false)
    private boolean credentialsNonExpired = true;

    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "address_id", nullable = true)
    private Address address;

    @Column(nullable = true)
    private String phone;

    @Column(name = "first_name", nullable = true)
    private String firstName;

    @Column(name = "last_name", nullable = true)
    private String lastName;

    @Column(name = "picture_url", nullable = true)
    private String pictureUrl;

    @JsonIgnore
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RefreshToken refreshToken;
}
