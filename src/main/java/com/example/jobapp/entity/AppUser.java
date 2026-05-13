package com.example.jobapp.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "users")
@Data
public class AppUser {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String passwordHash;

    private Instant createdAt = Instant.now();
}

