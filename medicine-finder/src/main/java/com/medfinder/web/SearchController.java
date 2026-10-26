package com.medfinder.web;

import com.medfinder.model.Medicine;
import com.medfinder.repo.MedicineRepository;
import com.medfinder.service.SearchResult;
import com.medfinder.service.SearchService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Validated
public class SearchController {

    private final SearchService searchService;
    private final MedicineRepository medicineRepository;
    private final double defaultLat;
    private final double defaultLng;

    public SearchController(SearchService searchService,
                            MedicineRepository medicineRepository,
                            @Value("${app.seed.center-lat}") double defaultLat,
                            @Value("${app.seed.center-lng}") double defaultLng) {
        this.searchService = searchService;
        this.medicineRepository = medicineRepository;
        this.defaultLat = defaultLat;
        this.defaultLng = defaultLng;
    }

    /** GET /api/search?q=paracetamol&lat=12.97&lng=77.59&radiusKm=5 */
    @GetMapping("/search")
    public List<SearchResult> search(
            @RequestParam @NotBlank String q,
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
            @RequestParam(defaultValue = "5") @DecimalMin("0.1") @DecimalMax("50") double radiusKm) {
        return searchService.search(q, lat, lng, radiusKm);
    }

    /** Autocomplete for the search box. */
    @GetMapping("/medicines/suggest")
    public List<String> suggest(@RequestParam String q) {
        if (q.trim().length() < 2) return List.of();
        return medicineRepository
                .findTop8ByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(q.trim(), q.trim())
                .stream().map(Medicine::getName).toList();
    }

    /** Lets the frontend centre the map when the browser can't give a location. */
    @GetMapping("/config")
    public Map<String, Double> config() {
        return Map.of("defaultLat", defaultLat, "defaultLng", defaultLng);
    }
}
