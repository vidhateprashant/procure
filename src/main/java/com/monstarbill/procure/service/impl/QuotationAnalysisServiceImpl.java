package com.monstarbill.procure.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monstarbill.procure.commons.AppConstants;
import com.monstarbill.procure.commons.CommonUtils;
import com.monstarbill.procure.commons.CustomException;
import com.monstarbill.procure.commons.CustomMessageException;
import com.monstarbill.procure.commons.FilterNames;
import com.monstarbill.procure.dao.QuotationAnalysisDao;
import com.monstarbill.procure.enums.FormNames;
import com.monstarbill.procure.enums.Operation;
import com.monstarbill.procure.enums.TransactionStatus;
import com.monstarbill.procure.feignclient.MasterServiceClient;
import com.monstarbill.procure.feignclient.SetupServiceClient;
import com.monstarbill.procure.models.Location;
import com.monstarbill.procure.models.PurchaseOrder;
import com.monstarbill.procure.models.PurchaseRequisition;
import com.monstarbill.procure.models.Quotation;
import com.monstarbill.procure.models.QuotationAnalysis;
import com.monstarbill.procure.models.QuotationAnalysisHistory;
import com.monstarbill.procure.models.QuotationAnalysisItem;
import com.monstarbill.procure.models.QuotationHistory;
import com.monstarbill.procure.models.Supplier;
import com.monstarbill.procure.payload.request.MailRequest;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.IdNameResponse;
import com.monstarbill.procure.payload.response.PaginationResponse;
import com.monstarbill.procure.repository.PurchaseOrderItemRepository;
import com.monstarbill.procure.repository.PurchaseOrderRepository;
import com.monstarbill.procure.repository.PurchaseRequisitionRepository;
import com.monstarbill.procure.repository.QuotationAnalysisHistoryRepository;
import com.monstarbill.procure.repository.QuotationAnalysisItemRepository;
import com.monstarbill.procure.repository.QuotationAnalysisRepository;
import com.monstarbill.procure.repository.QuotationHistoryRepository;
import com.monstarbill.procure.repository.QuotationRepository;
import com.monstarbill.procure.service.QuotationAnalysisService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class QuotationAnalysisServiceImpl implements QuotationAnalysisService {

	@Autowired
	private QuotationAnalysisRepository quotationAnalysisRepository;

	@Autowired
	private QuotationAnalysisItemRepository quotationAnalysisItemRepository;

	@Autowired
	private QuotationAnalysisHistoryRepository quotationAnalysisHistoryRepository;
	
	@Autowired
	private QuotationRepository quotationRepository;

	@Autowired
	private QuotationHistoryRepository quotationHistoryRepository;
	
	@Autowired
	private MasterServiceClient masterServiceClient;
	
	@Autowired
	private PurchaseOrderItemRepository purchaseOrderItemRepository;
	
	@Autowired
	private SetupServiceClient setupServiceClient;

	@Autowired
	private QuotationAnalysisDao quotationAnalysisDao;
	
	@Autowired
	private PurchaseRequisitionRepository prRepository;
	
	@Autowired
	private PurchaseOrderRepository purchaseOrderRepository;
	
	@Override
	public QuotationAnalysis save(QuotationAnalysis quotationAnalysis) {
		String username = CommonUtils.getLoggedInUsername();
		
		log.info(" Quotation Analysis is Started :: " + quotationAnalysis.toString());
		// ------------------------------------ 1. Save the Quotation :: STARTS ----------------------------------------------------
		// Store the existing Quotation from DB
		Optional<QuotationAnalysis> oldQuotationAnalysis = Optional.empty();

		if (quotationAnalysis.getId() == null) {
			quotationAnalysis.setCreatedBy(username);
			
			Optional<QuotationAnalysis> existingQuotationAnalysis = this.quotationAnalysisRepository.findByRfqIdAndIsDeleted(quotationAnalysis.getRfqId(), false);
			if (existingQuotationAnalysis.isPresent()) {
				log.info("QA is already exist for given RFQ. QA - " + existingQuotationAnalysis.get().getId());
				throw new CustomMessageException("QA is already exist for given RFQ. QA - " + existingQuotationAnalysis.get().getId());
			}
			
			String transactionalDate = CommonUtils.convertDateToFormattedString(quotationAnalysis.getQaDate());
			String documentSequenceNumber = this.setupServiceClient.getDocumentSequenceNumber(transactionalDate, quotationAnalysis.getSubsidiaryId(), FormNames.QA.getFormName(), false);
			if (StringUtils.isEmpty(documentSequenceNumber)) {
				throw new CustomMessageException("Please validate your configuration to generate the QA Number");
			}
			quotationAnalysis.setQaNumber(documentSequenceNumber);
		} else {
			// Get the existing object using the deep copy
			oldQuotationAnalysis = this.quotationAnalysisRepository.findByIdAndIsDeleted(quotationAnalysis.getId(), false);
			if (oldQuotationAnalysis.isPresent()) {
				try {
					oldQuotationAnalysis = Optional.ofNullable((QuotationAnalysis) oldQuotationAnalysis.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		quotationAnalysis.setLastModifiedBy(username);
		QuotationAnalysis quotationAnalysisSaved;
		try {
			quotationAnalysisSaved = this.quotationAnalysisRepository.save(quotationAnalysis);
		} catch (DataIntegrityViolationException e) {
			log.error("Quotation analysis unique constrain violetd." + e.getMostSpecificCause());
			throw new CustomException(" Quotation analysis unique constrain violetd :" + e.getMostSpecificCause());
		}

		if (quotationAnalysisSaved == null) {
			log.error("Error while saving the Quotation Analysis - " + quotationAnalysis.toString());
			throw new CustomMessageException("Error while saving the Quotation Analysis - ");
		}
		log.info("Quotation Analysis Saved successfully : " + quotationAnalysisSaved.toString());

		Long qaId = quotationAnalysisSaved.getId();
		String qaNumber = quotationAnalysisSaved.getQaNumber();
		String bidType = quotationAnalysis.getBidType();
//		String currency = quotationAnalysisSaved.getCurrency();
		
		// update the data in Item history table
		this.updateQuotationAnalysisHistory(quotationAnalysisSaved, oldQuotationAnalysis);
		// ------------------------------------ 1. Save the Quotation Analysis :: FINISHED ----------------------------------------------------
		// ------------------------------------ 2. Save the Quotation Analysis Items :: STARTED ----------------------------------------------------
		log.info("Save the Quotation Analysis Items :: STARTED");
		if (CollectionUtils.isNotEmpty(quotationAnalysis.getQuotationAnalysisItems())) {
			
			// Store the existing Quotation from DB
			Optional<QuotationAnalysisItem> oldQaItem = Optional.empty();
			
			for (QuotationAnalysisItem qaItem : quotationAnalysis.getQuotationAnalysisItems()) {
				
				this.validateQaItem(bidType, qaItem);
				
				// Store the existing Quotation from DB
				oldQaItem = Optional.empty();
				
				if (qaItem.getId() == null) {
					qaItem.setCreatedBy(username);
				} else {
					// Get the existing object using the deep copy
					oldQaItem = this.quotationAnalysisItemRepository.findByIdAndIsDeleted(qaItem.getId(), false);
					if (oldQaItem.isPresent()) {
						try {
							oldQaItem = Optional.ofNullable((QuotationAnalysisItem) oldQaItem.get().clone());
						} catch (CloneNotSupportedException e) {
							log.error("Error while Cloning the object. Please contact administrator.");
							throw new CustomException("Error while Cloning the object. Please contact administrator.");
						}
					}
				}

				qaItem.setQaId(qaId);
				qaItem.setQaNumber(qaNumber);
				qaItem.setLastModifiedBy(username);
				QuotationAnalysisItem qaItemUpdated = this.quotationAnalysisItemRepository.save(qaItem);				

				if (qaItemUpdated == null) {
					log.error("Error while saving the Quotation Analysis Item - " + qaItem.toString());
					throw new CustomMessageException("Error while saving the Quotation Analysis Item.");
				}
				log.info("Quotation Analysis Item Saved successfully : " + qaItemUpdated.toString());
				
				// update the data in history table
				this.updateQuotationAnalysisItemHistory(qaItemUpdated, oldQaItem);
			}
			
		}
		log.info("Save the Quotation Analysis Items :: FINISHED");
		// ------------------------------------ 2. Save the Quotation Analysis Items :: FINISHED ----------------------------------------------------
		// ------------------------------------ 3. Update the status of the RFQ STARTED ---------------------------------------------------------------------
		/**
		 * QA is created.
		 * So updating the RFQ status of the given RFQ. If not found then throwing the error
		 */
		Optional<Quotation> quotation = this.quotationRepository.findByIdAndIsDeleted(quotationAnalysisSaved.getRfqId(), false);
		if (quotation.isPresent()) {
			if (!TransactionStatus.QA_CREATED.getTransactionStatus().equalsIgnoreCase(quotation.get().getStatus())) {
				quotation.get().setStatus(TransactionStatus.QA_CREATED.getTransactionStatus());
				quotation = Optional.ofNullable(this.quotationRepository.save(quotation.get()));
				if (quotation.isPresent()) {
					this.quotationHistoryRepository.save(prepareQuotationHistory(quotation.get().getRfqNumber(), null, AppConstants.QUOTATION, "Status", Operation.UPDATE.toString(), quotation.get().getLastModifiedBy(), TransactionStatus.SUBMITTED.getTransactionStatus(), quotation.get().getStatus()));
					log.info("Status is updated to Process for the RFQ - " + quotationAnalysisSaved.getRfqId());
				} else {
					log.error("Error while updating the RFQ status for the RFQ : " + quotationAnalysis.getRfqId());
					throw new CustomMessageException("Error while updating the RFQ status for the RFQ : " + quotationAnalysisSaved.getRfqId());
				}
			}
		} else {
			log.error("RFQ is Not found against the RFQ Number : " + quotationAnalysis.getRfqId());
			throw new CustomMessageException("RFQ is Not found against the RFQ Number : " + quotationAnalysis.getRfqId());
		}
		// ------------------------------------ 3. Update the status of the RFQ FINISHED --------------------------------------------------------------------
		log.info(" Quotation Analysis is Finished.");
		return quotationAnalysis;
	}
	
	/**
	 * Prepares the history for the Quotation
	 * @param QuotationId
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public QuotationHistory prepareQuotationHistory(String rfqNumber, Long childId, String moduleName, String fieldName, String operation, String lastModifiedBy, String oldValue, String newValue) {
		QuotationHistory quotationHistory = new QuotationHistory();
		quotationHistory.setRfqNumber(rfqNumber);
		quotationHistory.setChildId(childId);
		quotationHistory.setModuleName(moduleName);
		quotationHistory.setFieldName(fieldName);
		quotationHistory.setChangeType(AppConstants.UI);
		quotationHistory.setOperation(operation);
		quotationHistory.setOldValue(oldValue);
		quotationHistory.setNewValue(newValue);
		quotationHistory.setLastModifiedBy(lastModifiedBy);
		return quotationHistory;
	}

	private void validateQaItem(String bidType, QuotationAnalysisItem qaItem) {
		// Item currency should be same as RFQ Currency
//				if (!currency.equalsIgnoreCase(qaItem.getCurrency())) {
//					log.error("Select Item currency and RFQ Currency should be same.");
//					throw new CustomMessageException("Select Item currency and RFQ Currency should be same.");	
//				}
		
		// If bid = close then approved supplier should not be null
//		if (AppConstants.BID_CLOSE.equalsIgnoreCase(bidType)) {
//			if (qaItem.getApprovedSupplier() == null) {
//				log.error("Please select Approved Supplier for the Item.");
//				throw new CustomMessageException("Please select Approved Supplier for the Item.");
//			}
//		} else 
//		if (AppConstants.BID_OPEN.equalsIgnoreCase(bidType)) {
//			if (qaItem.isAwarded() && qaItem.getApprovedSupplier() == null) {
//				log.error("Please select Approved Supplier for the Item.");
//				throw new CustomMessageException("Please select Approved Supplier for the Item.");
//			}
//		}
	}

	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Quotation Analysis is new 
	 * Add entry as a Update if Quotation Analysis is exists
	 * 
	 * @param quotationAnalysis
	 * @param oldQuotationAnalysis
	 */
	private void updateQuotationAnalysisHistory(QuotationAnalysis quotationAnalysis, Optional<QuotationAnalysis> oldQuotationAnalysis) {
		if (oldQuotationAnalysis.isPresent()) {
			// insert the updated fields in history table
			List<QuotationAnalysisHistory> quotationAnalysisHistories = new ArrayList<QuotationAnalysisHistory>();
			try {
				quotationAnalysisHistories = oldQuotationAnalysis.get().compareFields(quotationAnalysis);
				if (CollectionUtils.isNotEmpty(quotationAnalysisHistories)) {
					this.quotationAnalysisHistoryRepository.saveAll(quotationAnalysisHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("Quotation Analysis History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT 
			this.quotationAnalysisHistoryRepository.save(this.prepareQuotationAnalysisHistory(quotationAnalysis.getQaNumber(), null, AppConstants.QUOTATION_ANALYSIS, Operation.CREATE.toString(), quotationAnalysis.getLastModifiedBy(), null, String.valueOf(quotationAnalysis.getId())));
		}
	}
	
	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Quotation Analysis Item is new 
	 * Add entry as a Update if Quotation Analysis Item is exists
	 * 
	 * @param quotationAnalysisItem
	 * @param oldQuotationAnalysisItem
	 */
	private void updateQuotationAnalysisItemHistory(QuotationAnalysisItem quotationAnalysisItem, Optional<QuotationAnalysisItem> oldQuotationAnalysisItem) {
		if (oldQuotationAnalysisItem.isPresent()) {
			// insert the updated fields in history table
			List<QuotationAnalysisHistory> quotationAnalysisHistories = new ArrayList<QuotationAnalysisHistory>();
			try {
				quotationAnalysisHistories = oldQuotationAnalysisItem.get().compareFields(quotationAnalysisItem);
				if (CollectionUtils.isNotEmpty(quotationAnalysisHistories)) {
					this.quotationAnalysisHistoryRepository.saveAll(quotationAnalysisHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("Quotation Analysis History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT 
			this.quotationAnalysisHistoryRepository.save(this.prepareQuotationAnalysisHistory(quotationAnalysisItem.getQaNumber(), quotationAnalysisItem.getId(), AppConstants.QUOTATION_ANALYSIS_ITEM, Operation.CREATE.toString(), quotationAnalysisItem.getLastModifiedBy(), null, String.valueOf(quotationAnalysisItem.getId())));
		}
	}
	
	/**
	 * Prepares the history for the Quotation Analysis
	 * @param QA-Number
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public QuotationAnalysisHistory prepareQuotationAnalysisHistory(String qaNumber, Long childId, String moduleName, String operation, String lastModifiedBy, String oldValue, String newValue) {
		QuotationAnalysisHistory quotationAnalysisHistory = new QuotationAnalysisHistory();
		quotationAnalysisHistory.setQaNumber(qaNumber);
		quotationAnalysisHistory.setChildId(childId);
		quotationAnalysisHistory.setModuleName(moduleName);
		quotationAnalysisHistory.setChangeType(AppConstants.UI);
		quotationAnalysisHistory.setOperation(operation);
		quotationAnalysisHistory.setOldValue(oldValue);
		quotationAnalysisHistory.setNewValue(newValue);
		quotationAnalysisHistory.setLastModifiedBy(lastModifiedBy);
		return quotationAnalysisHistory;
	}


	@Override
	public QuotationAnalysis findById(Long id) {
		log.info(" Quotation Analysis Find By ID is Started :: " + id);
		Optional<QuotationAnalysis> quotationAnalysis = Optional.empty();
		
		quotationAnalysis = this.quotationAnalysisRepository.findByIdAndIsDeleted(id, false);
		if (!quotationAnalysis.isPresent()) {
			log.info("QA is not exist for id - " + id);
			throw new CustomMessageException("QA is not exist for id - " + id);
		}
		
		Optional<Quotation> rfq = this.quotationRepository.findByIdAndIsDeleted(quotationAnalysis.get().getRfqId(), false);
		if (rfq.isPresent()) {
			quotationAnalysis.get().setRfqNumber(rfq.get().getRfqNumber());
		}
		
		// Get all the items with details
		List<QuotationAnalysisItem> qaItems = this.quotationAnalysisItemRepository.findByQaIdAndIsDeleted(quotationAnalysis.get().getId(), false);
		for (QuotationAnalysisItem quotationAnalysisItem : qaItems) {
			Location location = this.masterServiceClient.getLocationsById(quotationAnalysisItem.getPrLocationId());
			if (location != null) {
				quotationAnalysisItem.setPrLocation(location.getLocationName());
			}
			if (quotationAnalysisItem.getPrId() != null) {
				Optional<PurchaseRequisition> purchaseRequisition = this.prRepository.findByIdAndIsDeleted(quotationAnalysisItem.getPrId(), false);
				if (purchaseRequisition.isPresent()) {
					quotationAnalysisItem.setPrNumber(purchaseRequisition.get().getPrNumber());
				}
			}
			
			if (quotationAnalysisItem.getPoId() != null) {
				Optional<PurchaseOrder> purchaseOrder = this.purchaseOrderRepository.findByIdAndIsDeleted(quotationAnalysisItem.getPoId(), false);
				if (purchaseOrder.isPresent()) 
					quotationAnalysisItem.setPoRef(purchaseOrder.get().getPoNumber());
			}
		}
		quotationAnalysis.get().setQuotationAnalysisItems(qaItems);

		log.info(" Quotation Analysis Find By ID is Finished.");
		return quotationAnalysis.get();
	}


	@Override
	public List<QuotationAnalysisHistory> findHistoryById(String qaNumber, Pageable pageable) {
		List<QuotationAnalysisHistory> histories = this.quotationAnalysisHistoryRepository.findByQaNumberOrderById(qaNumber, pageable);
		String createdBy = histories.get(0).getLastModifiedBy();
		histories.forEach(e->{
			e.setCreatedBy(createdBy);
		});
		return histories;
	}

	@Override
	public List<QuotationAnalysis> getQaNumberByPrIds(List<Long> prIds) {
		return this.quotationAnalysisRepository.getQaNumberByPrIds(prIds);
	}

	@Override
	public List<Long> getSuppliersByQaIds(List<Long> qaIds) {
		return this.quotationAnalysisRepository.getApprovedSupplierByQaIds(qaIds);
	}

	@Override
	public List<IdNameResponse> findQaNumbersBySubsidiaryId(Long subsidiaryId) {
		List<IdNameResponse> qaNumbers = new ArrayList<IdNameResponse>();
		List<QuotationAnalysisItem> allQaNumbers = this.quotationAnalysisItemRepository.findQaNumbersWithQuantity(subsidiaryId);
		for (QuotationAnalysisItem qaItem : allQaNumbers) {
			Double poQuantity = purchaseOrderItemRepository.findQuantityByQaId(qaItem.getQaId());
			if (poQuantity == null || qaItem.getQuantity() > poQuantity) {
				IdNameResponse idNameResponse = new IdNameResponse();
				idNameResponse.setId(qaItem.getQaId());
				idNameResponse.setName(qaItem.getQaNumber());
				qaNumbers.add(idNameResponse);
			}
		}
		return qaNumbers;
	}

	@Override
	public List<Supplier> findSupplierByQaId(Long qaId) {
		List<Long> suppliers = this.quotationAnalysisItemRepository.findSupplierByQaId(qaId);
		List<Supplier> supplierMapping = new ArrayList<Supplier>();
		if (CollectionUtils.isNotEmpty(suppliers)) {
			supplierMapping = this.masterServiceClient.getSuppliersByIds(suppliers);
		}
		return supplierMapping;
	}

	@Override
	public List<Long> findPrIdsByQaId(Long qaId) {
		return this.quotationAnalysisItemRepository.findPrIdsByQaId(qaId);
	}

	@Override
	public List<Location> findLocationsByQaIdAndSupplier(Long qaId, Long supplierId) {
		List<Long> locations = this.quotationAnalysisItemRepository.findLocationsByQaIdAndSupplier(qaId, supplierId);
		return this.masterServiceClient.findLocationNamesByIds(locations);
	}

	@Override
	public List<QuotationAnalysisItem> findItemsByQaAndSupplierAndLocation(Long qaId, Long supplierId,
			Long locationId) {
		return this.quotationAnalysisItemRepository.findItemsByQaAndSupplierAndLocation(qaId, supplierId, locationId);
	}

	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<QuotationAnalysis> quotationAnalysis = new ArrayList<QuotationAnalysis>();

		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest);

		// get list
		quotationAnalysis = this.quotationAnalysisDao.findAll(whereClause, paginationRequest);

		// getting count
		Long totalRecords = this.quotationAnalysisDao.getCount(whereClause);

		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(),
				quotationAnalysis, totalRecords);
	}

	private String prepareWhereClause(PaginationRequest paginationRequest) {
		Map<String, ?> filters = paginationRequest.getFilters();

		Long subsidiaryId = null;
		String bidType = null;
		String fromDate = null;
		String toDate = null;
		String qaFromDate = null;
		String qaToDate = null;

		if (filters.containsKey(FilterNames.SUBSIDIARY_ID))
			subsidiaryId = ((Number) filters.get(FilterNames.SUBSIDIARY_ID)).longValue();
		
		if (filters.containsKey(FilterNames.TYPE))
			bidType = (String) filters.get(FilterNames.TYPE);
		
		if (filters.containsKey(FilterNames.FROM_DATE)) 
			fromDate = (String) filters.get(FilterNames.FROM_DATE);
		
		
		if (filters.containsKey(FilterNames.TO_DATE)) 
			toDate = (String) filters.get(FilterNames.TO_DATE);
		
		if (filters.containsKey(FilterNames.QA_FROM_DATE)) 
			qaFromDate = (String) filters.get(FilterNames.QA_FROM_DATE);
		
		if (filters.containsKey(FilterNames.QA_TO_DATE)) 
			qaToDate = (String) filters.get(FilterNames.QA_TO_DATE);
		
		StringBuilder whereClause = new StringBuilder(" AND qa.isDeleted is false ");
		if (subsidiaryId != null && subsidiaryId != 0) {
			whereClause.append(" AND qa.subsidiaryId = ").append(subsidiaryId);
		}
		if (StringUtils.isNotEmpty(bidType)) {
			whereClause.append(" AND lower(q.bidType) like lower ('%").append(bidType).append("%')");
		}
		if (fromDate != null) {
			whereClause.append(" AND to_char(q.bidOpenDate, 'yyyy-MM-dd') >= '").append(fromDate).append("' ");
		}
		if (toDate != null) {
			whereClause.append(" AND to_char(q.bidCloseDate, 'yyyy-MM-dd') <= '").append(toDate).append("' ");
		}
		if (qaFromDate != null) {
			whereClause.append(" AND to_char(qa.qaDate, 'yyyy-MM-dd') >= '").append(qaFromDate).append("' ");
		}
		if (qaToDate != null) {
			whereClause.append(" AND to_char(qa.qaDate, 'yyyy-MM-dd') <= '").append(qaToDate).append("' ");
		}
		
		return whereClause.toString();
	}

	@Override
	public String sendMail(MailRequest mailRequest) {
		try {
			CommonUtils.sendMail(mailRequest.getToMail(), mailRequest.getCcMail(), mailRequest.getSubject(), mailRequest.getBody());
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error while sending the mail.");
			throw new CustomException("Error while sending the mail.");
		}	
		return "Mail sent successfully.";
	}

}
