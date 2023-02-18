package com.monstarbill.procure.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.monstarbill.procure.models.GrnHistory;

public interface GrnHistoryRepository extends JpaRepository<GrnHistory, String> {

	List<GrnHistory> findByGrnNumber(String grnNumber, Pageable pageable);

}
