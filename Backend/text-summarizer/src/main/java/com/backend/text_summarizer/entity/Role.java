package com.backend.text_summarizer.entity;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "roles")
@Entity

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    //  each user can have one or more roles (Many to Many)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;
}