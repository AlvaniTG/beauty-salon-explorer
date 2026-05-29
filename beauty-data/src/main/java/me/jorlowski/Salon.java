package me.jorlowski;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record Salon(
        String id,
        String name,
        String address,
        String district,
        String phoneNumber,
        Map<String, String> socialMediaLinks,
        List<String> servicesOffered,
        String priceRange,
        Double rating,
        Integer reviews,
        Double lat,
        Double lon,
        Boolean isManuallyEdited
) {
    public String generateMergeKey() {
        String safeName = (name == null ? "" : name).toLowerCase().replaceAll("\\s+", "");
        String safeAddress = (address == null ? "" : address).toLowerCase().replaceAll("\\s+", "");
        return safeName + "|" + safeAddress;
    }

    public Salon mergeWith(Salon otherSalon) {
        if (this.isManuallyEdited) return this;
        if (otherSalon.isManuallyEdited) return otherSalon;

        return new Salon(
                this.id != null ? this.id : otherSalon.id,
                chooseBetter(this.name, otherSalon.name),
                chooseBetter(this.address, otherSalon.address),
                this.district != null ? this.district : otherSalon.district,
                this.phoneNumber != null ? this.phoneNumber : otherSalon.phoneNumber,
                mergeMaps(this.socialMediaLinks, otherSalon.socialMediaLinks),
                mergeLists(this.servicesOffered, otherSalon.servicesOffered),
                this.priceRange != null ? this.priceRange : otherSalon.priceRange,
                this.rating != null ? this.rating : otherSalon.rating,
                mergeReviews(this.reviews, otherSalon.reviews),
                this.lat != null ? this.lat : otherSalon.lat,
                this.lon != null ? this.lon : otherSalon.lon,
                false
        );
    }

    private String chooseBetter(String s1, String s2) {
        if (s1 == null) return s2;
        if (s2 == null) return s1;
        return s1.length() > s2.length() ? s1 : s2;
    }

    private List<String> mergeLists(List<String> l1, List<String> l2) {
        if (l1 == null && l2 == null) return List.of();
        return Stream.concat(
                l1 == null ? Stream.empty() : l1.stream(),
                l2 == null ? Stream.empty() : l2.stream()
        ).distinct().toList();
    }

    private Map<String, String> mergeMaps(Map<String, String> m1, Map<String, String> m2) {
        if (m1 == null && m2 == null) return Map.of();
        Map<String, String> result = new HashMap<>();
        if (m1 != null) result.putAll(m1);
        if (m2 != null) result.putAll(m2);
        return Map.copyOf(result);
    }

    private Integer mergeReviews(Integer r1, Integer r2) {
        if (r1 != null && r2 != null) {
            return r1 + r2;
        }
        if (r1 != null) {
            return r1;
        }
        return r2;
    }

    @Override
    public String toString() {
        return """
            -------------------------------------------------
            %s
            -------------------------------------------------
              Adres:      %s
              Dzielnica:  %s
              Telefon:    %s
              Ceny:       %s
              Ocena:      %s (Opinii: %s)
              Usługi:     %s
              Sociale:    %s
            """.formatted(
                name != null ? name : "Brak nazwy",
                address != null ? address : "Brak adresu",
                district != null ? district : "Brak danych",
                phoneNumber != null ? phoneNumber : "Brak",
                priceRange != null ? priceRange : "Brak danych",
                rating != null ? rating : "-",
                reviews != null ? reviews : "0",
                servicesOffered != null && !servicesOffered.isEmpty() ? String.join(", ", servicesOffered) : "Brak danych",
                socialMediaLinks != null && !socialMediaLinks.isEmpty() ? socialMediaLinks : "Brak danych"
        );
    }
}
