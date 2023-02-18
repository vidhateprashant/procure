package com.monstarbill.procure.dao.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.monstarbill.procure.commons.CommonUtils;
import com.monstarbill.procure.commons.CustomException;
import com.monstarbill.procure.dao.QuotationAnalysisDao;
import com.monstarbill.procure.models.QuotationAnalysis;
import com.monstarbill.procure.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("quotationAnalysisDaoImpl")
public class QuotationAnalysisDaoImpl implements QuotationAnalysisDao {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_QUOTATIONS_ANALYSIS = " select new com.monstarbill.procure.models.QuotationAnalysis(qa.id, qa.qaNumber, qa.rfqId, qa.subsidiaryId, qa.qaDate, q.bidType as bidType, "
			+ " q.bidOpenDate as bidOpenDate, q.bidCloseDate as bidCloseDate, q.currency as currency, s.name as subsidiaryName ) " 
			+ " FROM QuotationAnalysis qa "
			+ " INNER JOIN Quotation q ON q.id = qa.rfqId " 
			+ " INNER JOIN Subsidiary s ON s.id = qa.subsidiaryId "
			+ " WHERE 1=1 ";
	
	public static final String GET_QUOTATIONS_ANALYSIS_COUNT = " select count(qa) "
			+ " FROM QuotationAnalysis qa INNER JOIN Quotation q ON q.id = qa.rfqId " 
			+ " INNER JOIN Subsidiary s ON s.id = qa.subsidiaryId "
			+ " WHERE 1=1 ";

	@Override
	public List<QuotationAnalysis> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<QuotationAnalysis> quotationAnalysis = new ArrayList<QuotationAnalysis>();
		
		StringBuilder finalSql = new StringBuilder(GET_QUOTATIONS_ANALYSIS);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		// order by clause
		finalSql.append(CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		log.info("Final SQL to get all Quotations analysis w/w/o filter :: " + finalSql.toString());
		
		try {
			TypedQuery<QuotationAnalysis> sql = this.entityManager.createQuery(finalSql.toString(), QuotationAnalysis.class);
			// pagination
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			quotationAnalysis = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of Quotations analysis :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return quotationAnalysis;
	}
	
	@Override
	public Long getCount(String whereClause) {
		Long count = 0L;
		
		StringBuilder finalSql = new StringBuilder(GET_QUOTATIONS_ANALYSIS_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		log.info("Final SQL to get all Quotations analysis Count w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of Quotations analysis :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}

}
