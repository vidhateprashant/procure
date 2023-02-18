package com.monstarbill.procure.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.RtvHistory;

@Repository
public interface RtvHistoryRepository extends JpaRepository<RtvHistory, String> {

	public List<RtvHistory> findByRtvNumber(String rtvNumber, Pageable pageable);
}
