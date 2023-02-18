package com.monstarbill.procure.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.monstarbill.procure.models.Rtv;
import com.monstarbill.procure.models.RtvHistory;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.PaginationResponse;

public interface RtvService {
	
	public Rtv save(Rtv rtv);

	public Rtv getRtvById(Long id);
	
	public List<RtvHistory> findHistoryByRtvNumber(String rtvNumber, Pageable pageable);
	
	public PaginationResponse findAll(PaginationRequest paginationRequest);

	public Boolean sendForApproval(Long id);

	public Boolean approveAllRtvs(List<Long> rtvIds);

	public Boolean rejectAllRtvs(List<Rtv> rtvs);

	public Boolean updateNextApprover(Long approverId, Long rtvId);

	public Boolean selfApprove(Long rtvId);
	
}
