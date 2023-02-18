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
import com.monstarbill.procure.dao.RtvDao;
import com.monstarbill.procure.models.Rtv;
import com.monstarbill.procure.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("rtvDaoImpl")
public class RtvDaoImpl implements RtvDao{
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_RTV =  " select new com.monstarbill.procure.models.Rtv(r.id, s.name, r.createdDate, r.rtvNumber, sup.name, r.grnNumber, r.rtvDate) from Rtv r "
			+ " INNER join Subsidiary s ON r.subsidiaryId = s.id "
			+ " INNER JOIN Supplier sup ON r.supplierId = sup.id  "
			+ " WHERE 1 = 1 ";
	
	public static final String GET_RTV_COUNT = " select count(1) from Rtv r "
			+ " INNER join Subsidiary s ON r.subsidiaryId = s.id "
			+ " INNER JOIN Supplier sup ON r.supplierId = sup.id  "
			+ " WHERE 1 = 1 ";

	@Override
	public List<Rtv> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<Rtv> rtv = new ArrayList<Rtv>();
		
		StringBuilder finalSql = new StringBuilder(GET_RTV);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		// order by clause
		finalSql.append(CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		
		log.info("Final SQL to get all rtv w/w/o filter :: " + finalSql.toString());
		
		try {
			TypedQuery<Rtv> sql = this.entityManager.createQuery(finalSql.toString(), Rtv.class);
			// pagination
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			rtv = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of rtv :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return rtv;
	}
	
	@Override
	public Long getCount(String whereClause) {
		Long count = 0L;
		
		StringBuilder finalSql = new StringBuilder(GET_RTV_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause)) finalSql.append(whereClause.toString());
		
		log.info("Final SQL to get all rtv Count w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of rtv :: " + ex.toString());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}}
