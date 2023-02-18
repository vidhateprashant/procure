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
import com.monstarbill.procure.dao.PurchaseOrderDao;
import com.monstarbill.procure.models.PurchaseOrder;
import com.monstarbill.procure.payload.request.PaginationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("purchaseOrderDaoImpl")
public class PurchaseOrderdaoImpl implements PurchaseOrderDao {

	@PersistenceContext
	private EntityManager entityManager;

	public static final String GET_PURCHASE_ORDER = "select new com.monstarbill.procure.models.PurchaseOrder(po.id, po.poNumber, po.subsidiaryId, po.poType, "
			+ " po.supplierId, po.totalAmount, po.poDate, po.currency, s.name as subsidiaryName, su.name as supplierName, po.poStatus ) "
			+ " FROM PurchaseOrder po inner join Subsidiary s ON s.id = po.subsidiaryId "
			+ " inner join Supplier su ON su.id = po.supplierId "
			+ " WHERE 1=1 ";

	public static final String GET_PURCHASE_ORDER_COUNT = "select count(1) FROM PurchaseOrder po inner join Subsidiary s ON s.id = po.subsidiaryId "
			+ " inner join Supplier su ON su.id = po.supplierId WHERE 1=1 ";

	@Override
	public List<PurchaseOrder> findAll(String whereClause, PaginationRequest paginationRequest) {
		List<PurchaseOrder> PurchaseOrder = new ArrayList<PurchaseOrder>();
		StringBuilder finalSql = new StringBuilder(GET_PURCHASE_ORDER);
		if (StringUtils.isNotEmpty(whereClause))
			finalSql.append(whereClause.toString());
		finalSql.append(
				CommonUtils.prepareOrderByClause(paginationRequest.getSortColumn(), paginationRequest.getSortOrder()));
		log.info("Final SQL to get all Purchase order " + finalSql.toString());
		try {
			TypedQuery<PurchaseOrder> sql = this.entityManager.createQuery(finalSql.toString(), PurchaseOrder.class);
			sql.setFirstResult(paginationRequest.getPageNumber() * paginationRequest.getPageSize());
			sql.setMaxResults(paginationRequest.getPageSize());
			PurchaseOrder = sql.getResultList();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the list of Purchase order :: " + ex.toString());
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null)
				errorExceptionMessage = ex.toString();
			throw new CustomException(errorExceptionMessage);
		}
		return PurchaseOrder;
	}

	@Override
	public Long getCount(String whereClause) {
		Long count = 0L;

		StringBuilder finalSql = new StringBuilder(GET_PURCHASE_ORDER_COUNT);
		// where clause
		if (StringUtils.isNotEmpty(whereClause))
			finalSql.append(whereClause.toString());

		log.info("Final SQL to get all Purchase order Count w/w/o filter :: " + finalSql.toString());
		try {
			TypedQuery<Long> sql = this.entityManager.createQuery(finalSql.toString(), Long.class);
			count = sql.getSingleResult();
		} catch (Exception ex) {
			log.error("Exception occured at the time of fetching the count of Purchase order :: " + ex.toString());

			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null)
				errorExceptionMessage = ex.toString();

			throw new CustomException(errorExceptionMessage);
		}
		return count;
	}

}
