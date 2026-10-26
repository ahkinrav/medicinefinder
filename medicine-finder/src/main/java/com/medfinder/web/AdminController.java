package com.medfinder.web;

import com.medfinder.model.Medicine;
import com.medfinder.model.Pharmacy;
import com.medfinder.model.Stock;
import com.medfinder.repo.MedicineRepository;
import com.medfinder.repo.PharmacyRepository;
import com.medfinder.repo.StockRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Minimal endpoints for pharmacies to add themselves and update stock.
 * NOTE: add authentication (e.g. Spring Security) before exposing these publicly.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public record PharmacyRequest(
            @NotBlank String name, String address, String phone,
            @DecimalMin("-90") @DecimalMax("90") double latitude,
            @DecimalMin("-180") @DecimalMax("180") double longitude,
            LocalTime opensAt, LocalTime closesAt) {}

    public record StockRequest(
            @NotNull Long pharmacyId, @NotBlank String medicineName, String genericName,
            @Min(0) int quantity, @NotNull @DecimalMin("0.0") BigDecimal price) {}

    private final PharmacyRepository pharmacies;
    private final MedicineRepository medicines;
    private final StockRepository stocks;

    public AdminController(PharmacyRepository pharmacies, MedicineRepository medicines, StockRepository stocks) {
        this.pharmacies = pharmacies;
        this.medicines = medicines;
        this.stocks = stocks;
    }

    @PostMapping("/pharmacies")
    public ResponseEntity<Pharmacy> addPharmacy(@Valid @RequestBody PharmacyRequest r) {
        Pharmacy p = new Pharmacy(r.name(), r.address(), r.phone(), r.latitude(), r.longitude(),
                r.opensAt(), r.closesAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(pharmacies.save(p));
    }

    /** Adds the medicine if it's new, then creates or updates this pharmacy's stock line. */
    @PostMapping("/stock")
    public Stock upsertStock(@Valid @RequestBody StockRequest r) {
        Pharmacy pharmacy = pharmacies.findById(r.pharmacyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacy not found"));
        Medicine medicine = medicines.findByNameIgnoreCase(r.medicineName())
                .orElseGet(() -> medicines.save(new Medicine(r.medicineName(), r.genericName())));

        Stock stock = stocks.findByPharmacyIdAndMedicineId(pharmacy.getId(), medicine.getId())
                .orElseGet(() -> new Stock(pharmacy, medicine, 0, BigDecimal.ZERO));
        stock.setQuantity(r.quantity());
        stock.setPrice(r.price());
        return stocks.save(stock);
    }
}
