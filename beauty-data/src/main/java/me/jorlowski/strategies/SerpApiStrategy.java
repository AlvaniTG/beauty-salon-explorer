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

public class SerpApiStrategy implements ScraperStrategy {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_PAGES = 6;

    @Override
    public String getSourceName() {
        return "SerpApi (Google Maps)";
    }

    @Override
    public List<Salon> fetchSalons(HttpClient client) {

        String serpApiKey = System.getenv("SERPAPI_KEY");

        if (serpApiKey == null || serpApiKey.isBlank()) {
            throw new IllegalStateException("Blokada uruchomienia: Brak zdefiniowanej zmiennej środowiskowej SERPAPI_KEY.");
        }

        System.out.println("[INFO] Pobieranie danych z SerpApi (Google Maps) z paginacją...");
        List<Salon> allSalons = new ArrayList<>();

        int start = 0;
        int page = 1;

        while (page <= MAX_PAGES) {
            System.out.println(" -> Pobieranie strony " + page + " (start=" + start + ")...");

            String url = "https://serpapi.com/search.json?engine=google_maps&q=salon+fryzjerski+kosmetyczny+warszawa&hl=pl&gl=pl&api_key=" + serpApiKey + "&start=" + start;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    System.err.println("SerpApi Error na stronie " + page + ": " + response.statusCode());
                    break;
                }

                List<Salon> pageSalons = parseResponse(response.body());
                allSalons.addAll(pageSalons);

                JsonNode root = MAPPER.readTree(response.body());
                JsonNode pagination = root.path("serpapi_pagination");
                if (pagination.isMissingNode() || pagination.path("next").isMissingNode()) {
                    System.out.println(" -> Brak kolejnych stron wyników.");
                    break;
                }

                start += 20;
                page++;

            } catch (Exception e) {
                System.err.println("Błąd pobierania z SerpApi na stronie " + page + ": " + e.getMessage());
                break;
            }
        }

        System.out.println("[OK] SerpApi zakończyło pracę. Pobranych salonów: " + allSalons.size());
        return allSalons;
    }

    private List<Salon> parseResponse(String responseBody) throws Exception {
        List<Salon> salons = new ArrayList<>();
        JsonNode root = MAPPER.readTree(responseBody);
        JsonNode localResults = root.path("local_results");

        if (localResults == null || localResults.isMissingNode() || !localResults.isArray()) {
            return salons;
        }

        for (JsonNode result : localResults) {
            String name = result.path("title").asText(null);
            if (name == null) continue;

            String address = result.path("address").asText(null);
            String phone = result.path("phone").asText(null);
            Double rating = result.path("rating").isMissingNode() ? null : result.path("rating").asDouble();
            Integer reviews = result.path("reviews").isMissingNode() ? null : result.path("reviews").asInt();
            String price = result.path("price").asText(null);
            String website = result.path("website").asText(null);

            Map<String, String> socialMedia = new HashMap<>();
            if (website != null && !website.isBlank()) socialMedia.put("website", website);

            List<String> services = new ArrayList<>();
            JsonNode typesNode = result.path("types");

            if (typesNode != null && typesNode.isArray()) {
                for (JsonNode typeNode : typesNode) {
                    String singleType = typeNode.asText("");
                    if (!singleType.isEmpty()) {
                        services.add(singleType);
                    }
                }
            }

            Double lat = null;
            Double lon = null;
            JsonNode gps = result.path("gps_coordinates");
            if (gps != null && !gps.isMissingNode()) {
                lat = gps.path("latitude").asDouble();
                lon = gps.path("longitude").asDouble();
            }

            salons.add(new Salon(
                    java.util.UUID.randomUUID().toString(), name, address, null, phone,
                    socialMedia.isEmpty() ? null : socialMedia, services.isEmpty() ? null : services,
                    price, rating, reviews,
                    lat, lon,
                    false
            ));
        }
        return salons;
    }
}