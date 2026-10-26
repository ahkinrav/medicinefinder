package com.medfinder.service;

import com.medfinder.model.Pharmacy;
import com.medfinder.model.Stock;
import com.medfinder.repo.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
public class SearchService {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final StockRepository stockRepository;

    public SearchService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    /** Finds pharmacies within radiusKm that stock the medicine, nearest first. */
    @Transactional(readOnly = true)
    public List<SearchResult> search(String query, double lat, double lng, double radiusKm) {
        LocalTime now = LocalTime.now();
        return stockRepository.findAvailable(query.trim()).stream()
                .map(stock -> toResult(stock, lat, lng, now))
                .filter(r -> r.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(SearchResult::distanceKm))
                .toList();
    }

    private SearchResult toResult(Stock s, double lat, double lng, LocalTime now) {
        Pharmacy p = s.getPharmacy();
        double distance = haversineKm(lat, lng, p.getLatitude(), p.getLongitude());
        return new SearchResult(
                s.getId(), p.getId(), p.getName(), p.getAddress(), p.getPhone(),
                p.getLatitude(), p.getLongitude(),
                s.getMedicine().getName(), s.getMedicine().getGenericName(),
                s.getQuantity(), s.getPrice(),
                Math.round(distance * 100.0) / 100.0,
                p.isOpenAt(now), hoursLabel(p));
    }

    private String hoursLabel(Pharmacy p) {
        if (p.getOpensAt() == null || p.getClosesAt() == null || p.getOpensAt().equals(p.getClosesAt())) {
            return "Open 24 hours";
        }
        return p.getOpensAt() + " – " + p.getClosesAt();
    }

    /** Great-circle distance between two coordinates. */
    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
