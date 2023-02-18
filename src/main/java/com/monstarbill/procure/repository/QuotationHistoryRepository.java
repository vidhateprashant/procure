package com.monstarbill.procure.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.QuotationHistory;

/**
 * Repository for the Quotation and it's child history
 * @author Prashant
 */
@Repository
public interface QuotationHistoryRepository extends JpaRepository<QuotationHistory, String> {

	public List<QuotationHistory> findByRfqNumberOrderById(String rfqNumber, Pageable pageable);
	
}
