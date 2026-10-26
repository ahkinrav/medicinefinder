package com.medfinder.service;

import java.math.BigDecimal;

/** One pharmacy that has the searched medicine in stock. */
public record SearchResult(
        Long stockId,
        Long pharmacyId,
        String pharmacyName,
        String address,
        String phone,
        double latitude,
        double longitude,
        String medicineName,
        String genericName,
        int quantity,
        BigDecimal price,
        double distanceKm,
        boolean openNow,
        String hours
) {}
