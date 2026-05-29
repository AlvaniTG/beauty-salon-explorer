package me.jorlowski;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import me.jorlowski.strategies.OverpassApiStrategy;
import me.jorlowski.strategies.SerpApiStrategy;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.net.http.HttpClient;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class SalonScraper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        System.out.println("[INFO] Starting data collection process...");
        DataSource dataSource = null;

        try {
            dataSource = setupDatabase();
        } catch (Exception e) {
            System.err.println("[FATAL] Database connection refused or initialization failed: " + e.getMessage());
            System.exit(1);
            return;
        }

        List<ScraperStrategy> strategies = List.of(
//              new OverpassApiStrategy(),
                new SerpApiStrategy()
        );

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        Map<String, Salon> mergedSalonsRegistry = new ConcurrentHashMap<>();

        System.out.println("[INFO] Executing scraper strategies...");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = strategies.stream()
                    .map(strategy -> CompletableFuture.runAsync(() -> {
                        try {
                            List<Salon> fetchedSalons = strategy.fetchSalons(httpClient);
                            if (fetchedSalons != null) {
                                for (Salon salon : fetchedSalons) {
                                    if (salon != null) {
                                        mergedSalonsRegistry.merge(
                                                salon.generateMergeKey(),
                                                salon,
                                                Salon::mergeWith
                                        );
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("[ERROR] Strategy " + strategy.getSourceName() + "failed: " + e.getMessage());
                        }
                    }, executor))
                    .toList();
            futures.forEach(CompletableFuture::join);
        }

        List<Salon> fetchedSalons = List.copyOf(mergedSalonsRegistry.values());
        if (fetchedSalons.isEmpty()) {
            System.out.println("[WARN] No salons fetched from any strategy. Exiting...");
            return;
        }

        System.out.println("[INFO] Successfully fetched and merged " + fetchedSalons.size() + " unique records.");

        Map<String, String> existingDistricts = loadExistingDistrictsFromDb(dataSource);
        List<Salon> salonsForGeocoding = new ArrayList<>();

        for (Salon salon : fetchedSalons) {
            if (salon.district() == null || salon.district().isBlank()) {
                String dbDistrict = existingDistricts.get(salon.generateMergeKey());

                if (dbDistrict != null && !dbDistrict.isBlank()) {
                    salonsForGeocoding.add(new Salon(
                            salon.id(), salon.name(), salon.address(), dbDistrict,
                            salon.phoneNumber(), salon.socialMediaLinks(), salon.servicesOffered(),
                            salon.priceRange(), salon.rating(), salon.reviews(),
                            salon.lat(), salon.lon(), salon.isManuallyEdited()
                    ));
                    continue;
                }
            }
            salonsForGeocoding.add(salon);
        }
        List<Salon> finalSalonsToSave = NominatimDistrictEnricher.enrichDistricts(salonsForGeocoding, httpClient);
        saveToDatabase(dataSource, finalSalonsToSave);
        System.out.println("[INFO] Data collection process finished successfully.");
    }

    private static DataSource setupDatabase() {
        HikariConfig config = new HikariConfig();

        String dbUrl = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:postgresql://localhost:5439/salons_db";
        String dbUser = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "postgres";
        String dbPassword = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "postgres";

        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);

        config.setConnectionTimeout(30000);
        config.setMaximumPoolSize(10);

        HikariDataSource ds = new HikariDataSource(config);

        try {
            Flyway.configure().dataSource(ds).load().migrate();
            System.out.println("[INFO] Flyway database migration applied successfully.");
        } catch (Exception e) {
            System.err.println("[ERROR] Flyway database migration failed: " + e.getMessage());
            throw e;
        }

        return ds;
    }

    private static Map<String, String> loadExistingDistrictsFromDb(DataSource ds) {
        System.out.println("[INFO] Loading existing districts from database...");
        Map<String, String> map = new ConcurrentHashMap<>();
        String sql = "SELECT name, address, district FROM salons WHERE district IS NOT NULL";

        try (Connection conn = ds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                String address = rs.getString("address");
                String district = rs.getString("district");

                String safeName = (name == null ? "" : name).toLowerCase().replaceAll("\\s+", "");
                String safeAddress = (address == null ? "" : address).toLowerCase().replaceAll("\\s+", "");
                map.put(safeName + "|" + safeAddress, district);
            }
            System.out.println("[INFO] Loaded " + map.size() + " known districts from database.");
        } catch (SQLException e) {
            System.err.println("[WARN] Failed to load existing districts: " + e.getMessage() + ". Geocoding will run for all missing records.");
        }
        return map;
    }

    private static void saveToDatabase(DataSource ds, List<Salon> salons) {
        System.out.println("[INFO] Preparing to save/update " + salons.size() + " records in the database...");
        String upsertSql = """
    INSERT INTO salons (
        id, name, address, district, phone_number, social_media_links, 
        services_offered, price_range, rating, reviews, lat, lon, is_manually_edited
    ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?, ?)
    ON CONFLICT (name, address) DO UPDATE SET
        district = EXCLUDED.district,
        phone_number = EXCLUDED.phone_number,
        social_media_links = EXCLUDED.social_media_links,
        services_offered = EXCLUDED.services_offered,
        price_range = EXCLUDED.price_range,
        rating = EXCLUDED.rating,
        reviews = EXCLUDED.reviews,
        lat = EXCLUDED.lat,
        lon = EXCLUDED.lon
    WHERE salons.is_manually_edited = FALSE
    """;

        try (Connection conn = ds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(upsertSql)) {

            conn.setAutoCommit(false);

            for (Salon salon : salons) {
                pstmt.setString(1, salon.id() != null ? salon.id() : UUID.randomUUID().toString());
                pstmt.setString(2, salon.name());
                pstmt.setString(3, salon.address() != null ? salon.address() : "");
                pstmt.setString(4, salon.district());
                pstmt.setString(5, salon.phoneNumber());
                pstmt.setString(6, toJson(salon.socialMediaLinks()));
                pstmt.setString(7, toJson(salon.servicesOffered()));
                pstmt.setString(8, salon.priceRange());
                pstmt.setObject(9, salon.rating());
                pstmt.setObject(10, salon.reviews());
                pstmt.setObject(11, salon.lat());
                pstmt.setObject(12, salon.lon());
                pstmt.setBoolean(13, salon.isManuallyEdited());

                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();
            conn.commit();
            System.out.println("[OK] Successfully saved/updated " + results.length + " records in database.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Database execution failed during batch insert: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERROR] Unexpected error during saving: " + e.getMessage());
        }
    }

    private static String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            System.err.println("[WARN] Failed to serialize JSON object: " + e.getMessage());
            return null;
        }
    }
}
