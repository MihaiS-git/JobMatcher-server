package com.jobmatcher.server.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name="addresses")
public class Address extends Auditable{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String street;

    private String city;

    private String state;

    @Column(name="postal_code")
    private String postalCode;

    private String country;
}
