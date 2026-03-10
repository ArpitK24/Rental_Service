package com.rentalService.model;


import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "cards")
public class Card {
	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Type(type = "uuid-char")
	@Column(name = "id", updatable = false, nullable = false, length = 36)
	private UUID id;


    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private String imageUrl;

    private int width;
    private int height;
    private OffsetDateTime createdAt;

    // --- Getters & Setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    // --- Convenience constructor ---
    public Card() {}
    public Card(String title, String description, String imageUrl,
                int width, int height, OffsetDateTime createdAt) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.width = width;
        this.height = height;
        this.createdAt = createdAt;
    }
}
