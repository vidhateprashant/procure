package com.monstarbill.procure.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.monstarbill.procure.models.Grn;
import com.monstarbill.procure.payload.request.PaginationRequest;

@Component("grnDao")
public interface GrnDao {
	
	public List<Grn> findAll(String whereClause, PaginationRequest paginationRequest);
	public Long getCount(String whereClause);

}




