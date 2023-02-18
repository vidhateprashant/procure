package com.monstarbill.procure.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.procure.models.PurchaseOrder;
import com.monstarbill.procure.payload.request.PaginationRequest;

@Component("purchaseOrderDao")
public interface PurchaseOrderDao {

	List<PurchaseOrder> findAll(String whereClause, PaginationRequest paginationRequest);

	Long getCount(String whereClause);
	
}
