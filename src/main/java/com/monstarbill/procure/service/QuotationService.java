package com.monstarbill.procure.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.monstarbill.procure.models.Quotation;
import com.monstarbill.procure.models.QuotationHistory;
import com.monstarbill.procure.models.QuotationItem;
import com.monstarbill.procure.models.QuotationVendors;
import com.monstarbill.procure.payload.request.GenerateRfqPoRequest;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.PaginationResponse;

public interface QuotationService {

	public Quotation save(Quotation quotation, boolean isSubmitted);

	public Quotation findById(Long id);

	public QuotationItem save(QuotationItem quotationItem);

	public PaginationResponse findAll(PaginationRequest paginationRequest);

	public List<QuotationHistory> findHistoryById(String rfqNumber, Pageable pageable);

//	public Quotation findByRfqNumber(String rfqNumber);

	public List<QuotationVendors> findVendorsByRfqId(Long rfqId);

	public List<QuotationItem> findQuotationItemByRfqIdAndVendor(Long rfqId, Long vendorId);

	public QuotationHistory prepareQuotationHistory(String rfqNumber, Long childId, String moduleName, String fieldName,
			String operation, String lastModifiedBy, String oldValue, String newValue);

	public Quotation closeQuotation(Long id);

	public void generateQuotation(GenerateRfqPoRequest generateRfqPoRequest);

	public List<Quotation> findBySubsidiaryId(Long subsidiaryId);

	public String sendMail(Long id);

	public String sendNotification(Long id);

	public List<QuotationItem> findQuotationItemByRfqId(Long rfqId);

	public String sendMailToRfqSupplier(String mailId);

}
