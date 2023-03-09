package com.monstarbill.procure.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.monstarbill.procure.models.Grn;
import com.monstarbill.procure.models.GrnHistory;
import com.monstarbill.procure.models.GrnItem;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.PaginationResponse;

public interface GrnService {

	public List<Grn> save(List<Grn> grn);

	public PaginationResponse findAll(PaginationRequest paginationRequest);

	public Grn getGrnById(Long id);

	public boolean deleteById(Long id);

	public List<GrnHistory> findHistoryById(String grnNumber, Pageable pageable);

	public List<GrnItem> findGrnItemsByGrnNumber(String grnNumber);
	
	
	Grn getByGrnId(Long grnId);
	
	GrnItem getByGrnItemId(Long grnId, Long itemId);
	
	List<Grn> getByPoId(Long poId);

	public List<Grn> getGrnBySubsidiaryId(Long subsidiaryId);

	public List<GrnItem> getGrnItemByGrnId(Long grnId);

	public List<GrnItem> getGrnItemByGrnIdAndItemId(Long grnId, Long itemId);

	public List<GrnItem> saveGrnItem(List<GrnItem> grnItems);

	public  GrnItem saveGrnItemObject( GrnItem grnItem);

	public boolean isGrnFullyProcessed(Long grnId);

}
