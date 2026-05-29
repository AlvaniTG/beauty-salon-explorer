package me.jorlowski;

import java.net.http.HttpClient;
import java.util.List;

public interface ScraperStrategy {
    List<Salon> fetchSalons(HttpClient client);
    String getSourceName();
}
