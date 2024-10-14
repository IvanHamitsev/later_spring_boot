package ru.practicum.item;

import java.time.Instant;
import java.time.LocalDate;

public interface UrlMetadataRetriever {
    public UrlMetadata retrieve(String urlString);

    interface UrlMetadata {
        String getNormalUrl();
        String getResolvedUrl();
        String getMimeType();
        String getTitle();
        boolean isHasImage();
        boolean isHasVideo();
        Instant getDateResolved();
    }

}
