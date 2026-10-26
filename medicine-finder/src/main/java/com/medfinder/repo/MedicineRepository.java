package com.medfinder.repo;

import com.medfinder.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    Optional<Medicine> findByNameIgnoreCase(String name);

    List<Medicine> findTop8ByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(String name, String generic);
}
