package com.monstarbill.procure.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.procure.models.QuotationAnalysis;
import com.monstarbill.procure.payload.request.PaginationRequest;

@Component("quotationAnalysisDao")
public interface QuotationAnalysisDao {

	List<QuotationAnalysis> findAll(String whereClause, PaginationRequest paginationRequest);

	Long getCount(String whereClause);
	
	
}
