package me.jorlowski.strategies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.jorlowski.Salon;
import me.jorlowski.ScraperStrategy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class OverpassApiStrategy implements ScraperStrategy {

    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getSourceName() {
        return "OpenStreetMap (Overpass API - Szybkie Zapytanie)";
    }

    @Override
    public List<Salon> fetchSalons(HttpClient client) {
        System.out.println("[INFO] Pobieranie danych z OSM...");

        String overpassQuery = """
                [out:json][timeout:25];
                area["name"="Warszawa"]->.searchArea;
                (
                  nwr["shop"="hairdresser"](area.searchArea);
                  nwr["amenity"="beauty"](area.searchArea);
                );
                out center;
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OVERPASS_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "WarsawBeautySalonExplorer")
                .POST(HttpRequest.BodyPublishers.ofString(overpassQuery))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("OSM API Error: " + response.statusCode());
                System.err.println("Szczegóły: " + response.body());
                return List.of();
            }

            if (response.body() == null || response.body().isBlank()) {
                System.err.println("Błąd OSM: Serwer zwrócił całkowicie pustą odpowiedź!");
                return List.of();
            }

            return parseResponse(response.body());

        } catch (Exception e) {
            System.err.println("Błąd OSM: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    private List<Salon> parseResponse(String responseBody) throws Exception {
        List<Salon> salons = new ArrayList<>();
        JsonNode root = MAPPER.readTree(responseBody);
        JsonNode elements = root.path("elements");

        for (JsonNode element : elements) {
            JsonNode tags = element.path("tags");

            if (tags == null || tags.isMissingNode() || tags.path("name").isMissingNode()) {
                continue;
            }

            String name = tags.path("name").asText();
            String street = tags.path("addr:street").asText("");
            String houseNumber = tags.path("addr:housenumber").asText("");
            String address = street.isEmpty() ? null : (street + " " + houseNumber).trim();
            String phone = tags.path("phone").asText(tags.path("contact:phone").asText(null));
            String district = tags.path("addr:suburb").asText(null);

            Map<String, String> socialMedia = new HashMap<>();
            addIfPresent(socialMedia, "website", tags.path("website").asText(null));
            addIfPresent(socialMedia, "facebook", tags.path("contact:facebook").asText(null));
            addIfPresent(socialMedia, "instagram", tags.path("contact:instagram").asText(null));
            addIfPresent(socialMedia, "email", tags.path("contact:email").asText(null));

            Double lat = null;
            Double lon = null;
            if (element.has("center")) { // Dla budynków (poligonów)
                lat = element.path("center").path("lat").asDouble();
                lon = element.path("center").path("lon").asDouble();
            } else if (element.has("lat") && element.has("lon")) {
                lat = element.path("lat").asDouble();
                lon = element.path("lon").asDouble();
            }

            salons.add(new Salon(
                    java.util.UUID.randomUUID().toString(), name, address, district, phone,
                    socialMedia.isEmpty() ? null : socialMedia, List.of("Kosmetyka/Fryzjer"),
                    null, null, null,
                    lat, lon,
                    false
            ));
        }
        return salons;
    }

    private List<String> extractServices(JsonNode tags) {
        List<String> rawServices = new ArrayList<>();

        if ("hairdresser".equals(tags.path("shop").asText())) rawServices.add("hairdresser");
        if ("beauty".equals(tags.path("amenity").asText())) rawServices.add("beauty");

        if ("yes".equals(tags.path("barber").asText())) rawServices.add("barber");

        String beautyTags = tags.path("beauty").asText("");
        if (!beautyTags.isEmpty() && !beautyTags.equals("yes")) {
            rawServices.addAll(List.of(beautyTags.split(";")));
        }

        String hairdresserTags = tags.path("hairdresser").asText("");
        if (!hairdresserTags.isEmpty() && !hairdresserTags.equals("yes")) {
            rawServices.addAll(List.of(hairdresserTags.split(";")));
        }

        return rawServices.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .map(s -> {
                    if (s.isEmpty()) return s;
                    return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
                })
                .distinct()
                .toList();
    }

    private void addIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}
