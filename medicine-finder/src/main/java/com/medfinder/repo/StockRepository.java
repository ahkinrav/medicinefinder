package com.medfinder.repo;

import com.medfinder.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    @Query("""
        select s from Stock s
        join fetch s.pharmacy p
        join fetch s.medicine m
        where s.quantity > 0
          and (lower(m.name) like lower(concat('%', :q, '%'))
               or lower(m.genericName) like lower(concat('%', :q, '%')))
        """)
    List<Stock> findAvailable(@Param("q") String query);

    Optional<Stock> findByPharmacyIdAndMedicineId(Long pharmacyId, Long medicineId);
}
