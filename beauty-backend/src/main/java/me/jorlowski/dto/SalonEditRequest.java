package me.jorlowski.dto;

public record SalonEditRequest(
        String address,
        String district,
        String phoneNumber,
        String priceRange
) {
}
