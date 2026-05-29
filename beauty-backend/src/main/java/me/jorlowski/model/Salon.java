package me.jorlowski.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "salons")
@Getter @Setter
public class Salon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    private String name;
    private String address;
    private String district;

    @Column(name = "phone_number")
    private String phoneNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "social_media_links", columnDefinition = "jsonb")
    private Map<String, String> socialMediaLinks = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "services_offered", columnDefinition = "jsonb")
    private List<String> servicesOffered;

    @Column(name = "price_range")
    private String priceRange;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    private double rating;

    private int reviews;

    private double lat;
    private double lon;

    @Column(name = "is_manually_edited")
    private boolean isManuallyEdited;
}
