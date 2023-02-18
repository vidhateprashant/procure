package com.monstarbill.procure.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.procure.models.PrItem;
import com.monstarbill.procure.models.PurchaseRequisition;
import com.monstarbill.procure.payload.request.PaginationRequest;

@Component("purchaseRequisitionDao")
public interface PurchaseRequisitionDao {
	public List<PurchaseRequisition> findAll(String whereClause, PaginationRequest paginationRequest);
	public Long getCount(String whereClause);
	public List<PrItem> findUnprocessedItemsByPrId(Long prId, String formName);
	public List<PurchaseRequisition> findAllApprovedPr(String whereClause, PaginationRequest paginationRequest, Long subsidiaryId);
	public Long getCountPrApproved(String whereClause, Long subsidiaryId);
}
