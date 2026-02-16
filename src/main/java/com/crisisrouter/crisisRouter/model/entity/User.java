package com.crisisrouter.crisisRouter.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
// Hibernate is the object relational mapper, where an instance of class
// is a row. Variables of the class represent collumns

@Entity // This tells Hibernate to make a table out of this class
@Table(name = "users") // specifies table name in PostgreSQL
// Lombok Annotations
@Getter // creates getters and setters for all fields
@Setter
@NoArgsConstructor // creates a blank constructor, Hibernate requires this
                   // to recreate objects when pulled out of db
@AllArgsConstructor
@Builder // easier syntax
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = true)
    private String profilePictureUrl;

    @Column(length = 1000)
    private String description;

    private String address;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ResourceRequest> requests = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Claim> claims = new ArrayList<>();

}
