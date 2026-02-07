package com.crisisrouter.crisisRouter.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ResourceRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private User user;

    @Column(name = "claimed_at", nullable = false, updatable = false)
    private LocalDateTime claimedAt;

    @Column(nullable = false)
    private String status; // e.g., "ACTIVE", "COMPLETED"


}
