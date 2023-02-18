package com.monstarbill.procure.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.monstarbill.procure.models.Location;
import com.monstarbill.procure.models.QuotationAnalysis;
import com.monstarbill.procure.models.QuotationAnalysisHistory;
import com.monstarbill.procure.models.QuotationAnalysisItem;
import com.monstarbill.procure.models.Supplier;
import com.monstarbill.procure.payload.request.MailRequest;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.IdNameResponse;
import com.monstarbill.procure.payload.response.PaginationResponse;

public interface QuotationAnalysisService {

	public QuotationAnalysis save(QuotationAnalysis quotationAnalysis);

	public QuotationAnalysis findById(Long id);

	public List<QuotationAnalysisHistory> findHistoryById(String qaNumber, Pageable pageable);

	public List<QuotationAnalysis> getQaNumberByPrIds(List<Long> prIds);

	public List<Long> getSuppliersByQaIds(List<Long> qaIds);

	public List<IdNameResponse> findQaNumbersBySubsidiaryId(Long subsidiaryId);

	public List<Supplier> findSupplierByQaId(Long qaId);

	public List<Long> findPrIdsByQaId(Long qaId);

	public List<Location> findLocationsByQaIdAndSupplier(Long qaId, Long supplierId);

	public List<QuotationAnalysisItem> findItemsByQaAndSupplierAndLocation(Long qaId, Long supplierId,
			Long locationId);

	public PaginationResponse findAll(PaginationRequest paginationRequest);

	public String sendMail(MailRequest mailRequest);

}
