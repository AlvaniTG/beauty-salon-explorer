package me.jorlowski.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SalonDetails(
        UUID id,
        String name,
        String address,
        String district,
        String phoneNumber,
        Map<String, String> socialMediaLinks,
        List<String> servicesOffered,
        String priceRange,
        double rating,
        int reviews
) {
}
