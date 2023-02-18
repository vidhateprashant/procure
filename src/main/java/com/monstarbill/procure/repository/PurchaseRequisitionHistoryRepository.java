package com.monstarbill.procure.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.PurchaseRequisitionHistory;

/**
 * Repository for the Purchase Requisition and it's childs history
 * @author Prashant
 * 08-Jul-2022
 */
@Repository
public interface PurchaseRequisitionHistoryRepository extends JpaRepository<PurchaseRequisitionHistory, String> {

	public List<PurchaseRequisitionHistory> findByPrNumberOrderById(String prNumber, Pageable pageable);
	
}
