package ru.practicum.item;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto implements Serializable {
    private Long id;
    private Long userId;
    private String url;
    private String resolvedUrl;
    private String mimeType;
    private String title;
    private boolean hasImage;
    private boolean hasVideo;
    private Instant dateResolved; // дата обращения к странице
    private Set<String> tags;
}
