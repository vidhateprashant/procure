package com.monstarbill.procure.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.monstarbill.procure.models.PurchaseOrder;
import com.monstarbill.procure.models.PurchaseOrderHistory;
import com.monstarbill.procure.models.PurchaseOrderItem;
import com.monstarbill.procure.models.QuotationAnalysis;
import com.monstarbill.procure.payload.request.GenerateRfqPoRequest;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.PaginationResponse;

public interface PurchaseOrderService {

	public PurchaseOrder save(PurchaseOrder purchaseOrder);

	public PurchaseOrder findByPoId(Long id);

	public PaginationResponse findAll(PaginationRequest paginationRequest);

	public List<PurchaseOrderHistory> findHistoryById(String poNumber, Pageable pageable);

	public Boolean generatePoRfq(GenerateRfqPoRequest rfqPoRequest);

	public List<PurchaseOrder> generatePoFromQa(QuotationAnalysis quotationAnalysis);

	public List<PurchaseOrder> getPoApproval(String userId);

	public List<PurchaseOrder> findByLocation(Long locationId, Long subsidiaryId, List<String> poStatus);

	public List<PurchaseOrder> findSupplierAndCurrencyByPoId(Long poId);

	public List<PurchaseOrderItem> findByPoIdForItem(Long poId, String itemNature);

	public Boolean sendForApproval(Long id);

	public Boolean approveAllPos(List<Long> poIds);
	
	public PurchaseOrder getByPoId(Long poId);
	
	public PurchaseOrderItem getByPoItemId(Long poId, Long itemId);
	
	public List<PurchaseOrder> getBySupplierSubsidiary(Long supplierId, Long subsidiaryId);

	public String findPoItemsByQaAndItem(Long qaId, Long itemId);

	public byte[] upload(MultipartFile file);

	public Boolean rejectAllPos(List<PurchaseOrder> pos);

	public Boolean updateNextApprover(Long approverId, Long poId);

	public List<PurchaseOrder> generateMultiplePoFromQa(QuotationAnalysis quotationAnalysis);

	public byte[] downloadTemplate();

	public Boolean selfApprove(Long poId);

	public PurchaseOrderItem savePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem);

}
