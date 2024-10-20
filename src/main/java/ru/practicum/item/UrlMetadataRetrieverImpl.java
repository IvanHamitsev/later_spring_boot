package ru.practicum.item;

import lombok.Builder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

public class UrlMetadataRetrieverImpl implements UrlMetadataRetriever {
    private String resolvedUrl;
    private MimeTypes mimeType;
    private String title;
    private Boolean hasImage;
    private Boolean hasVideo;
    private LocalDate dateResolved;

    // объект для непосредственно обменов
    private final HttpClient client;
    // нужен конструктор для инициализации клиента
    UrlMetadataRetrieverImpl(@Value("${url-metadata-retriever.read_timeout-sec:120}") int readTimeout) {
        this.client =  HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(readTimeout))
                .build();
    }


    @Override
    public UrlMetadata retrieve(String urlString) {
        final URI uri;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException e) {
            // Если адрес не соответствует правилам URI адресов, то генерируем исключение.
            throw new RuntimeException("The URL is malformed: " + urlString, e);
        }

        HttpResponse<Void> resp = connect(uri, "HEAD", HttpResponse.BodyHandlers.discarding());

        String contentType = resp.headers()
                .firstValue(HttpHeaders.CONTENT_TYPE)
                .orElse("*");

        MediaType mediaType = MediaType.parseMediaType(contentType);

        final UrlMetadataImpl result;

        if (mediaType.isCompatibleWith(MimeType.valueOf("text/*"))) {
            result = handleText(resp.uri());
        } else if (mediaType.isCompatibleWith(MimeType.valueOf("image/*"))) {
            result = handleImage(resp.uri());
        } else if (mediaType.isCompatibleWith(MimeType.valueOf("video/*"))) {
            result = handleVideo(resp.uri());
        } else {
            throw new RuntimeException("The content type [" + mediaType
                    + "] at the specified URL is not supported.");
        }

        return result.toBuilder()
                .normalUrl(urlString)
                .resolvedUrl(resp.uri().toString())
                .mimeType(mediaType.getType())
                .dateResolved(Instant.now())
                .build();
    }

    private <T> HttpResponse<T> connect(URI url,
                                        String method,
                                        HttpResponse.BodyHandler<T> responseBodyHandler) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();

        final HttpResponse<T> response;
        try {
            response = client.send(request, responseBodyHandler);
        } catch (IOException e) {
            throw new RuntimeException("Cannot retrieve data from the URL: " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Cannot get the metadata for url: " + url
                    + " because the thread was interrupted.", e);
        }

        HttpStatus status = HttpStatus.resolve(response.statusCode());
        if(status == null) {
            throw new RuntimeException("The server returned an unknown status code: " + response.statusCode());
        }

        if(status.equals(HttpStatus.UNAUTHORIZED) || status.equals(HttpStatus.FORBIDDEN)) {
            throw new RuntimeException("There is no access to the resource at the specified URL: " + url);
        }
        if(status.isError()) {
            throw new RuntimeException("Cannot get the data on the item because the server returned an error."
                    + "Response status: " + status);
        }

        return response;
    }

    private UrlMetadataImpl handleText(URI url) {
        // заполняем поля для случая, когда страница содержит текст (в том числе html)
        // дополнительный запрос выясняет, есть ли изображения и видео
        HttpResponse<String> resp = connect(url, "GET", HttpResponse.BodyHandlers.ofString());

        // воспользуемся библиотекой Jsoup для парсинга содержимого
        Document doc = Jsoup.parse(resp.body());

        // ищем в полученном документе html-тэги, говорящие, что он
        // содержит видео или аудио информацию
        Elements imgElements = doc.getElementsByTag("img");
        Elements videoElements = doc.getElementsByTag("video");

        // добавляем полученные данные в ответ. В том числе находим заголовок
        // полученной страницы.
        return UrlMetadataImpl.builder()
                .title(doc.title())
                .hasImage(!imgElements.isEmpty())
                .hasVideo(!videoElements.isEmpty())
                .build();
    }

    private UrlMetadataImpl handleVideo(URI url) {
        // заполняем поля для случая, когда страница содержит видео
        String name = new File(url).getName();
        return UrlMetadataImpl.builder()
                .title(name)
                .hasVideo(true)
                .build();
    }

    private UrlMetadataImpl handleImage(URI url) {
        // заполняем поля для случая, когда страница содержит изображение
        String name = new File(url).getName();
        return UrlMetadataImpl.builder()
                .title(name)
                .hasImage(true)
                .build();
    }

    @lombok.Value
    @Builder(toBuilder = true)
    static class UrlMetadataImpl implements UrlMetadata {
        String normalUrl;
        String resolvedUrl;
        String mimeType;
        String title;
        boolean hasImage;
        boolean hasVideo;
        Instant dateResolved;
    }
}
