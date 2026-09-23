package com.powerpuff.backend.dto;

import com.powerpuff.backend.model.Item;
import java.time.Instant;

public record ItemDto(Long id, String title, String description, Instant createdAt) {

    public static ItemDto from(Item item) {
        return new ItemDto(item.getId(), item.getTitle(), item.getDescription(), item.getCreatedAt());
    }
}
