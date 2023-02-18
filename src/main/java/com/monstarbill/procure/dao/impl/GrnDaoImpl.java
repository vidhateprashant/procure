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
import com.monstarbill.procure.dao.GrnDao;
import com.monstarbill.procure.models.Grn;
import com.monstarbill.procure.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("grnDaoImpl")
public class GrnDaoImpl implements GrnDao {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public static final String GET_GRN = "select new com.monstarbill.procure.models.Grn(gr.id, gr.grnNumber, gr.subsidiaryId, gr.locationId, gr.poNumber, gr.grnDate, gr.supplierId, sub.name as subsidiaryName, l.locationName as locationName, su.name as supplierName) "
			+ " FROM Grn gr inner join Subsidiary sub ON sub.id = gr.subsidiaryId inner join Location l ON l.id = gr.locationId inner join Supplier su ON su.id = gr.supplierId " 
			+ " WHERE 1=1 ";
	
	public static final String GET_GRN_COUNT = "select count(1) FROM Grn gr inner join Subsidiary sub ON sub.id = gr.subsidiaryId inner join Location l ON l.id = gr.locationId inner join Supplier su ON su.id = gr.supplierId WHERE 1=1  ";

	@Override
	public List<Grn> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<Grn> grn = new ArrayList<Grn>();
		StringBuilder finalSql = new StringBuilder(GET_GRN);
		if (StringUtils.isNotEmpty(whereClause))
			finalSql.append(whereClause.toString());
		finalSql.append(
				CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		log.info("Final SQL to get all GRN " + finalSql.toString());
		try {
			TypedQuery<Grn> sql = this.entityManager.createQuery(finalSql.toString(), Grn.class);
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			grn = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of GRN :: " + ex.toString());
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null)
				errorExceptionMessage = ex.toString();
			throw new CustomException(errorExceptionMessage);
		}
		return grn;
	}

	@Override
	public Long getCount(String whereClause) {
		Long count = 0L;

		StringBuilder finalSql = new StringBuilder(GET_GRN_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause))
			finalSql.append(whereClause.toString());

		log.info("Final SQL to get all GRN Count w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of GRN :: " + ex.toString());

			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null)
				errorExceptionMessage = ex.toString();

			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}

}
