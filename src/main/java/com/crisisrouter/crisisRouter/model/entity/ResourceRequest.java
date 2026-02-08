package com.crisisrouter.crisisRouter.model.entity;


import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "resource_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relationship to User (The 'creator_id' in your schema)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User user;

    // Relationship to Category (The 'category_id' in your schema)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "severity_level", nullable = false)
    private Integer severityLevel;

    @Column(name = "custom_category")
    private String customCategory;

    @Column(nullable = false)
    private String address;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status; // You can also use an Enum here later

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Add this field
    @Column(name = "image_url")
    private String imageUrl;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = RequestStatus.OPEN; // Default status
        }
    }



}
