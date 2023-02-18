package com.monstarbill.procure.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.procure.models.Quotation;
import com.monstarbill.procure.payload.request.PaginationRequest;

@Component("quotationDao")
public interface QuotationDao {
	
	public List<Quotation> findAll(String whereClause, PaginationRequest paginationRequest);

	public Long getCount(String whereClause);
	
}
