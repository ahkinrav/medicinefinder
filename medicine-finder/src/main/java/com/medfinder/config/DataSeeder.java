package com.medfinder.config;

import com.medfinder.model.Medicine;
import com.medfinder.model.Pharmacy;
import com.medfinder.model.Stock;
import com.medfinder.repo.MedicineRepository;
import com.medfinder.repo.PharmacyRepository;
import com.medfinder.repo.StockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Fills the in-memory database with sample pharmacies and stock around app.seed.center-*. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final PharmacyRepository pharmacies;
    private final MedicineRepository medicines;
    private final StockRepository stocks;
    private final double centerLat;
    private final double centerLng;

    public DataSeeder(PharmacyRepository pharmacies, MedicineRepository medicines, StockRepository stocks,
                      @Value("${app.seed.center-lat}") double centerLat,
                      @Value("${app.seed.center-lng}") double centerLng) {
        this.pharmacies = pharmacies;
        this.medicines = medicines;
        this.stocks = stocks;
        this.centerLat = centerLat;
        this.centerLng = centerLng;
    }

    @Override
    public void run(String... args) {
        if (pharmacies.count() > 0) return;

        // name, street, phone, north-south offset (km), east-west offset (km), opens, closes
        Object[][] sites = {
                {"GreenCross Pharmacy", "12 Market Road", "+91 90000 00001", 0.4, 0.3, "08:00", "22:00"},
                {"CityCare Chemists", "45 Station Street", "+91 90000 00002", -0.9, 0.6, "09:00", "21:00"},
                {"Wellspring Medicals", "7 Temple Lane", "+91 90000 00003", 1.4, -0.8, "07:30", "23:00"},
                {"Lifeline 24x7 Pharmacy", "101 Ring Road", "+91 90000 00004", -1.8, -1.2, "00:00", "00:00"},
                {"Sunrise Medicals", "3 Park Avenue", "+91 90000 00005", 2.6, 1.5, "08:30", "20:30"},
                {"HealthFirst Pharmacy", "88 Hospital Road", "+91 90000 00006", -3.2, 2.1, "06:00", "22:00"},
                {"Metro Chemist", "19 Bus Stand Road", "+91 90000 00007", 4.1, -2.4, "09:00", "21:30"},
                {"Apex Drugstore", "56 Lake View Road", "+91 90000 00008", 0.1, -3.5, "10:00", "20:00"},
        };

        String[][] meds = {
                {"Paracetamol 500mg", "Paracetamol"},
                {"Dolo 650", "Paracetamol"},
                {"Ibuprofen 400mg", "Ibuprofen"},
                {"Amoxicillin 500mg", "Amoxicillin"},
                {"Azithromycin 500mg", "Azithromycin"},
                {"Cetirizine 10mg", "Cetirizine"},
                {"Omeprazole 20mg", "Omeprazole"},
                {"Pantoprazole 40mg", "Pantoprazole"},
                {"Metformin 500mg", "Metformin"},
                {"ORS Sachet", "Oral rehydration salts"},
                {"Vitamin C 500mg", "Ascorbic acid"},
                {"Cough Syrup 100ml", "Dextromethorphan"},
        };

        List<Pharmacy> savedPharmacies = new ArrayList<>();
        for (Object[] s : sites) {
            double dLat = (double) s[3] / 111.0; // ~111 km per degree of latitude
            double dLng = (double) s[4] / (111.0 * Math.cos(Math.toRadians(centerLat)));
            savedPharmacies.add(pharmacies.save(new Pharmacy(
                    (String) s[0], (String) s[1], (String) s[2],
                    centerLat + dLat, centerLng + dLng,
                    LocalTime.parse((String) s[5]), LocalTime.parse((String) s[6]))));
        }

        List<Medicine> savedMedicines = new ArrayList<>();
        for (String[] m : meds) savedMedicines.add(medicines.save(new Medicine(m[0], m[1])));

        Random random = new Random(42); // fixed seed → same demo data each run
        for (Pharmacy p : savedPharmacies) {
            for (Medicine m : savedMedicines) {
                if (random.nextDouble() < 0.65) {
                    int qty = random.nextInt(10) == 0 ? 0 : 3 + random.nextInt(58);
                    BigDecimal price = BigDecimal.valueOf(8 + random.nextInt(240));
                    stocks.save(new Stock(p, m, qty, price));
                }
            }
        }
    }
}
