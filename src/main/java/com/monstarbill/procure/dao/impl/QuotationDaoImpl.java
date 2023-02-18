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
import com.monstarbill.procure.dao.QuotationDao;
import com.monstarbill.procure.models.Quotation;
import com.monstarbill.procure.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("quotationDaoImpl")
public class QuotationDaoImpl implements QuotationDao {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_QUOTATIONS = " select new com.monstarbill.procure.models.Quotation(q.id, s.name, q.rfqDate, q.rfqNumber, q.bidOpenDate, q.bidCloseDate, q.bidType, q.currency, q.status) " 
			+ " FROM Quotation q " 
			+ " INNER JOIN Subsidiary s ON s.id = q.subsidiaryId "
			+ " WHERE 1=1 ";
	
	public static final String GET_QUOTATIONS_COUNT = " select count(q) "
			+ " FROM Quotation q " 
			+ " INNER JOIN Subsidiary s ON s.id = q.subsidiaryId "
			+ " WHERE 1=1 ";

	@Override
	public List<Quotation> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<Quotation> suppliers = new ArrayList<Quotation>();
		
		StringBuilder finalSql = new StringBuilder(GET_QUOTATIONS);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		// order by clause
		finalSql.append(CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		log.info("Final SQL to get all Quotations w/w/o filter :: " + finalSql.toString());
		
		try {
			TypedQuery<Quotation> sql = this.entityManager.createQuery(finalSql.toString(), Quotation.class);
			// pagination
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			suppliers = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of Quotations :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return suppliers;
	}
	
	@Override
	public Long getCount(String whereClause) {
		Long count = 0L;
		
		StringBuilder finalSql = new StringBuilder(GET_QUOTATIONS_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		log.info("Final SQL to get all Quotations Count w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of Quotations :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}

}
