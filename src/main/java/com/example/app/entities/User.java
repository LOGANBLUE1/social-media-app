package com.example.app.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity @Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
    private String password;
    private int image;
}
