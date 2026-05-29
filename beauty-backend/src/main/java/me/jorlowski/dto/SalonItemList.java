package me.jorlowski.dto;

import java.util.UUID;

public record SalonItemList(
        UUID id,
        String name,
        String address,
        String district,
        String priceRange,
        double rating,
        int reviews
) {
}
