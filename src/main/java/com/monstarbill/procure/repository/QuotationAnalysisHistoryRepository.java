package com.monstarbill.procure.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.QuotationAnalysisHistory;

/**
 * Repository for the Quotation Analysis and it's child history
 * @author Prashant
 */
@Repository
public interface QuotationAnalysisHistoryRepository extends JpaRepository<QuotationAnalysisHistory, String> {

	public List<QuotationAnalysisHistory> findByQaNumberOrderById(String QaNumber, Pageable pageable);
	
}
