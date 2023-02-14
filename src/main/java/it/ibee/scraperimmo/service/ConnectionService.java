package it.ibee.scraperimmo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

@Service
public class ConnectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionService.class);

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public String getJsonFromHttpRequest(String url, RequestMethod requestMethod) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .timeout(Duration.of(30, SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();

        } catch (Exception e) {
            LOGGER.error("Generic Error in getJsonFromHttpRequest", e);
        }
        return null;
    }

}
