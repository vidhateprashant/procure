package com.monstarbill.procure.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.procure.models.Rtv;
import com.monstarbill.procure.payload.request.PaginationRequest;

@Component("rtvDao")
public interface RtvDao {
	public List<Rtv> findAll(String whereClause, PaginationRequest paginationRequest);

	public Long getCount(String whereClause);

}
