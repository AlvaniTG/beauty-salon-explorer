package me.jorlowski;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class NominatimDistrictEnricher {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&zoom=14";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static List<Salon> enrichDistricts(List<Salon> salons, HttpClient client) {
        System.out.println("[INFO] Uruchamianie Reverse Geocodingu (Nominatim API) dla brakujących dzielnic...");
        List<Salon> enrichedSalons = new ArrayList<>();
        int updatedCount = 0;

        for (Salon salon : salons) {
            if (salon.district() != null || salon.lat() == null || salon.lon() == null) {
                enrichedSalons.add(salon);
                continue;
            }

            String district = fetchDistrictForCoordinates(client, salon.lat(), salon.lon());

            if (district != null) {
                updatedCount++;
                enrichedSalons.add(new Salon(
                        salon.id(), salon.name(), salon.address(), district, salon.phoneNumber(),
                        salon.socialMediaLinks(), salon.servicesOffered(), salon.priceRange(),
                        salon.rating(), salon.reviews(), salon.lat(), salon.lon(), salon.isManuallyEdited()
                ));
            } else {
                enrichedSalons.add(salon);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[OK] Zakończono Geocoding. Pomyślnie przypisano dzielnice dla " + updatedCount + " salonów.");
        return enrichedSalons;
    }

    private static String fetchDistrictForCoordinates(HttpClient client, Double lat, Double lon) {
        String url = String.format(java.util.Locale.US, NOMINATIM_URL, lat, lon);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "WarsawBeautySalonExplorer")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            JsonNode root = MAPPER.readTree(response.body());
            JsonNode addressNode = root.path("address");

            if (addressNode.has("suburb")) return addressNode.get("suburb").asText();
            if (addressNode.has("city_district")) return addressNode.get("city_district").asText();
            if (addressNode.has("borough")) return addressNode.get("borough").asText();
            if (addressNode.has("quarter")) return addressNode.get("quarter").asText();

            return null;
        } catch (Exception e) {
            System.err.println("Błąd Nominatim dla lat:" + lat + " lon:" + lon + " -> " + e.getMessage());
            return null;
        }
    }
}
