package com.monstarbill.procure.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import com.monstarbill.procure.commons.CustomBadRequestException;
import com.monstarbill.procure.commons.CustomException;
import com.monstarbill.procure.commons.CustomMessageException;
import com.monstarbill.procure.commons.FilterNames;
import com.monstarbill.procure.dao.QuotationDao;
import com.monstarbill.procure.enums.FormNames;
import com.monstarbill.procure.enums.Operation;
import com.monstarbill.procure.enums.TransactionStatus;
import com.monstarbill.procure.feignclient.MasterServiceClient;
import com.monstarbill.procure.feignclient.SetupServiceClient;
import com.monstarbill.procure.models.PrItem;
import com.monstarbill.procure.models.PurchaseRequisition;
import com.monstarbill.procure.models.Quotation;
import com.monstarbill.procure.models.QuotationAnalysis;
import com.monstarbill.procure.models.QuotationGenaralInfo;
import com.monstarbill.procure.models.QuotationHistory;
import com.monstarbill.procure.models.QuotationItem;
import com.monstarbill.procure.models.QuotationItemVendor;
import com.monstarbill.procure.models.QuotationPr;
import com.monstarbill.procure.models.QuotationVendors;
import com.monstarbill.procure.models.SupplierContact;
import com.monstarbill.procure.payload.request.GenerateRfqPoRequest;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.request.RfqPoRequest;
import com.monstarbill.procure.payload.response.PaginationResponse;
import com.monstarbill.procure.repository.PrItemRepository;
import com.monstarbill.procure.repository.PurchaseRequisitionRepository;
import com.monstarbill.procure.repository.QuotationGeneralInfoRepository;
import com.monstarbill.procure.repository.QuotationHistoryRepository;
import com.monstarbill.procure.repository.QuotationItemRepository;
import com.monstarbill.procure.repository.QuotationItemVendorRepository;
import com.monstarbill.procure.repository.QuotationPrRepository;
import com.monstarbill.procure.repository.QuotationRepository;
import com.monstarbill.procure.repository.QuotationVendorRepository;
import com.monstarbill.procure.service.PurchaseRequisitionService;
import com.monstarbill.procure.service.QuotationAnalysisService;
import com.monstarbill.procure.service.QuotationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class QuotationServiceImpl implements QuotationService {

	@Autowired
	private QuotationRepository quotationRepository;
	
	@Autowired
	private QuotationItemRepository quotationItemRepository;
	
	@Autowired
	private QuotationItemVendorRepository quotationItemVendorRepository;
	
	@Autowired
	private QuotationHistoryRepository quotationHistoryRepository;
	
	@Autowired
	private PurchaseRequisitionService purchaseRequisitionService;
	
	@Autowired
	private QuotationVendorRepository quotationVendorRepository;
	
	@Autowired
	private MasterServiceClient masterServiceClient;

	@Autowired
	private QuotationPrRepository quotationPrRepository;

	@Autowired
	private SetupServiceClient setupServiceClient;
	
	@Autowired
	private QuotationGeneralInfoRepository quotationGeneralInfoRepository;
	
	@Autowired
	private QuotationDao quotationDao;

	@Autowired
	private PrItemRepository prItemRepository;
	
	@Autowired
	private PurchaseRequisitionRepository prRepository;

	@Autowired
	private QuotationAnalysisService quotationAnalysisService;
	
	/**
	 * Prashant
	 * Date - 18-07-2022
	 * This will Save complete RFQ form. Step as below-
	 * 1. Save the Quotation
	 * 2. Save the All Items and Vendors Mapping
	 * 3. Save the All Vendors with default/overridden values
	 */
	@Override
	public Quotation save(Quotation quotation, boolean isSubmitted) {
		
		if (StringUtils.isNotEmpty(quotation.getStatus()) && !TransactionStatus.DRAFT.getTransactionStatus().equalsIgnoreCase(quotation.getStatus())
				&& !quotation.isCreateQa()) {
			log.info("You cannot update the Submitted RFQ.");
			throw new CustomMessageException("You cannot update the Submitted RFQ.");
		}
		
		String username = CommonUtils.getLoggedInUsername();
		
		/**
		 * User can't create RFQ in 2 cases - 
		 * 		1. If QA is exist 
		 * 		2. if another RFQ for Same PR is exist with status = OPEN
		 */
		if (quotation.getId() == null) {
//			Optional<QuotationAnalysis> quotationAnalysis = this.quotationAnalysisRepository.findByRfqIdAndIsDeleted(quotation.getId(), false);
			
//			if (quotationAnalysis.isPresent()) {
//				log.info("QA is already created for selected PR. QA-Number is : " + quotationAnalysis.get().getQaNumber());
//				throw new CustomMessageException("QA is already created for selected PR. QA-Number is : " + quotationAnalysis.get().getQaNumber());
//			} else {
			if (CollectionUtils.isEmpty(quotation.getQuotationPrs())) {
				log.error("PR Number is Required.");
				throw new CustomBadRequestException("PR Number is Required.");
			}
//				for (QuotationPr quotationPr : quotation.getQuotationPrs()) {
//					List<Quotation> quotationsByPr = this.quotationRepository.findByPrIdAndStatusAndIsDeleted(quotationPr.getPrId(), TransactionStatus.SUBMITTED.getTransactionStatus(), false);
//					if (CollectionUtils.isNotEmpty(quotationsByPr)) {
//						log.info("RFQ with Open status is already created for selected PR. RFQ-Number is : " + quotationsByPr.get(0).getRfqNumber());
//						throw new CustomMessageException("RFQ with Open status is already created for selected PR. RFQ-Number is : " + quotationsByPr.get(0).getRfqNumber());
//					}					
//				}
//			}
		}
	
		if (isSubmitted) {
			for (QuotationPr quotationPr : quotation.getQuotationPrs()) {
				List<Quotation> quotationsByPr = this.quotationRepository.findByPrIdAndStatusAndIsDeleted(quotationPr.getPrId(), TransactionStatus.SUBMITTED.getTransactionStatus(), false);
				if (CollectionUtils.isNotEmpty(quotationsByPr)) {
					for (Quotation existingQuotation : quotationsByPr) {
						if (quotation.getId() == null || (!quotation.getId().equals(existingQuotation.getId()))) {
							if (existingQuotation.getBidType().equalsIgnoreCase(quotation.getBidType())) {
								if (CommonUtils.compareDates(quotation.getRfqDate(), Date.from(existingQuotation.getBidCloseDate().toInstant())) == -1) {
									log.error("RFQ Date should be greater than Bid Close date of other Submitted RFQ : " + existingQuotation.getRfqNumber());
									throw new CustomException("RFQ Date should be greater than Bid Close date of other Submitted RFQ : " + existingQuotation.getRfqNumber());
								}
							}
						}
					}
				}					
			}
		}
		
//		if (isSubmitted) {
			boolean isQuotationValid = false;
			isQuotationValid = this.validateQuotation(quotation);
			
			if (!isQuotationValid) {
				log.error("Something is wrong in validation please check you entered correct values.");
				throw new CustomBadRequestException("Something is wrong in validation please check you entered correct values.");
			}
//		}
		
		// ------------------------------------ 1. Save the Quotation :: STARTS ----------------------------------------------------
		// Store the existing Quotation from DB
		Optional<Quotation> oldQuotation = Optional.empty();

		if (StringUtils.isEmpty(quotation.getStatus()) || TransactionStatus.DRAFT.getTransactionStatus().equalsIgnoreCase(quotation.getStatus())) {
			if (isSubmitted) {
				quotation.setStatus(TransactionStatus.SUBMITTED.getTransactionStatus());
			} else {
				quotation.setStatus(TransactionStatus.DRAFT.getTransactionStatus());
			}
		}
		
		if (quotation.isCreateQa()) {
			isSubmitted = true;
			quotation.setStatus(TransactionStatus.QA_CREATED.getTransactionStatus());
		}
		
		if (quotation.getId() == null) {
			quotation.setCreatedBy(username);
			String transactionalDate = CommonUtils.convertDateToFormattedString(quotation.getRfqDate());
			String documentSequenceNumber = this.setupServiceClient.getDocumentSequenceNumber(transactionalDate, quotation.getSubsidiaryId(), FormNames.RFQ.getFormName(), false);
			if (StringUtils.isEmpty(documentSequenceNumber)) {
				throw new CustomMessageException("Please validate your configuration to generate the RFQ Number");
			}
			quotation.setRfqNumber(documentSequenceNumber);
		} else {
			// Get the existing object using the deep copy
			oldQuotation = this.quotationRepository.findByIdAndIsDeleted(quotation.getId(), false);
			if (oldQuotation.isPresent()) {
				try {
					oldQuotation = Optional.ofNullable((Quotation) oldQuotation.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		quotation.setLastModifiedBy(username);
		Quotation quotationUpdated;
		try {
			quotationUpdated = this.quotationRepository.save(quotation);
		}catch (DataIntegrityViolationException e) {
			log.error("Quotation unique constrain violetd." + e.getMostSpecificCause());
			throw new CustomException("Quotation unique constrain violetd :" + e.getMostSpecificCause());
		}
		if (quotationUpdated == null) {
			log.error("Error while saving the Quotation." + quotation.toString());
			throw new CustomMessageException("Error while saving the Quotation.");
		}
		log.info("Quotation Saved successfully." + quotationUpdated.toString());
		
		Long rfqId = quotationUpdated.getId();
		String rfqNumber = quotationUpdated.getRfqNumber();
		
		// update the data in history table
		this.updateQuotationHistory(quotationUpdated, oldQuotation);
		
		/**
		 * Step - 02 : Save the RFQ - Subsidiary Mappings
		 */
		if (CollectionUtils.isEmpty(quotation.getQuotationPrs())) {
			log.error("PR Number is Required.");
			throw new CustomBadRequestException("PR Number is Required.");
		}
		for (QuotationPr quotationPr : quotation.getQuotationPrs()) {
			if (quotationPr.getId() == null) {
				// New Entry
				quotationPr.setQuotationId(rfqId);
				quotationPr.setQuotationNumber(rfqNumber);;
				quotationPr.setCreatedBy(username);
				quotationPr.setLastModifiedBy(username);
				
				quotationPr = this.quotationPrRepository.save(quotationPr);
				
				if (quotationPr == null) {
					log.error("Error while saving the RFQ PR Mapping.");
					throw new CustomMessageException("Error while saving the RFQ PR Mapping.");
				}
				log.info("Quotation PR mapping is saved :: " + quotationPr.getId());
				
				// update history as insert
				this.quotationHistoryRepository.save(this.prepareQuotationHistory(rfqNumber, quotationPr.getId(), AppConstants.QUOTATION_PR_NUMBER, null, Operation.CREATE.toString(), 
						quotationPr.getLastModifiedBy(), null, String.valueOf(quotationPr.getPrId())));
				
				Optional<PurchaseRequisition> pr = this.prRepository.findByIdAndIsDeleted(quotationPr.getPrId(), false);
				if (pr.isPresent()) {
					pr.get().setUsedFor(FormNames.RFQ.getFormName());
					this.prRepository.save(pr.get());
				}
			} else if (quotationPr.isDeleted()) {
				// entry is removed/unchecked/deleted
				quotationPr.setLastModifiedBy(username);
				
				quotationPr = this.quotationPrRepository.save(quotationPr);
				if (quotationPr == null) {
					log.error("Error while Removing the RFQ PR Mapping.");
					throw new CustomMessageException("Error while Removing the RFQ PR Mapping.");
				}
				log.info("Quotation PR mapping is saved :: " + quotationPr.getId());
				
				// update history as insert
				this.quotationHistoryRepository.save(this.prepareQuotationHistory(rfqNumber, quotationPr.getId(), AppConstants.QUOTATION_PR_NUMBER, null, Operation.DELETE.toString(), 
						quotationPr.getLastModifiedBy(), String.valueOf(quotationPr.getPrId()), null));
				
				Optional<PurchaseRequisition> pr = this.prRepository.findByIdAndIsDeleted(quotationPr.getPrId(), false);
				if (pr.isPresent()) {
					pr.get().setUsedFor(null);
					this.prRepository.save(pr.get());
				}
			}
		}
		// ------------------------------------ 1. Save the Quotation :: FINISHED ----------------------------------------------------
		
		/**
		 * Step - 03 : Save the RFQ Item & Vendors
		 */
		// ------------------------------------ 2. Save the Quotation Items & Vendors :: STARTS ----------------------------------------------------
		if (CollectionUtils.isNotEmpty(quotation.getQuotationItems())) {
			String status = quotationUpdated.getStatus();
			boolean isDraftStatus = TransactionStatus.DRAFT.getTransactionStatus().equalsIgnoreCase(status) ? true : false;
			boolean isSubmittedRfq = TransactionStatus.SUBMITTED.getTransactionStatus().equalsIgnoreCase(status) ? true : false;
			
//			String bidType = quotationUpdated.getBidType();
			for (QuotationItem quotationItem : quotation.getQuotationItems()) {
				// -------------------------------- ITEM MAPPING STARTED ---------------------------------------------------------
//				if (AppConstants.BID_CLOSE.equalsIgnoreCase(bidType)) {
//					if (quotationItem.geta)
//				}
				log.info("Quotation Item save Started..." + quotationItem.toString());
				// Store the existing Quotation from DB
				Optional<QuotationItem> oldQuotationItem = Optional.empty();
	
				
				Optional<PrItem> prItem = this.prItemRepository.findByPrIdAndItemIdAndIsDeleted(quotationItem.getPrId(), quotationItem.getItemId(), false);
				if (!prItem.isPresent()) {
					throw new CustomException("Mapping of PR and it's is not found.");
				}
				
				Double remainedQuantity = 0.0;
						
				if (quotationItem.getId() == null) {
					quotationItem.setRfqNumber(rfqNumber);
					quotationItem.setQuotationId(rfqId);
					quotationItem.setCreatedBy(username);
					if (!isDraftStatus) {
						remainedQuantity = prItem.get().getRemainedQuantity() - quotationItem.getQuantity();
					}
				} else {
					// Get the existing object using the deep copy
					oldQuotationItem = this.quotationItemRepository.findByIdAndIsDeleted(quotationItem.getId(), false);
					if (oldQuotationItem.isPresent()) {
						try {
							oldQuotationItem = Optional.ofNullable((QuotationItem) oldQuotationItem.get().clone());
						} catch (CloneNotSupportedException e) {
							log.error("Error while Cloning the object. Please contact administrator.");
							throw new CustomException("Error while Cloning the object. Please contact administrator.");
						}
					}
					if (!isDraftStatus) {
						Double newQuantity = quotationItem.getQuantity();
						Double oldQuantity = 0.0;
						if (!isSubmittedRfq) { 
							oldQuantity = oldQuotationItem.get().getQuantity();
						}
						Double difference = newQuantity - oldQuantity;
						remainedQuantity = prItem.get().getRemainedQuantity() - difference;
						
						if (remainedQuantity < 0) {
							throw new CustomException("RFQ Quantity should be less the PR Item remaining quantity.");
						}
					}
				}
				quotationItem.setLastModifiedBy(username);
				QuotationItem quotationItemUpdated = this.quotationItemRepository.save(quotationItem);
	
				if (!isDraftStatus) {
					prItem.get().setRemainedQuantity(remainedQuantity);
					this.prItemRepository.save(prItem.get());
				}
				
				if (quotationItemUpdated == null) {
					log.info("Error while saving the Quotation Item." + quotationItem.toString());
					throw new CustomMessageException("Error while saving the Quotation Item." + quotationItem.toString());
				}
				// useful to set in vendor list objects
				Long itemId = quotationItemUpdated.getItemId();
				
//				if (isSubmitted) {
//					// useful to check which item assigned to which RFQ
////					Optional<PrItem> prItem = this.prItemRepository.findByPrIdAndItemId(quotationItem.getPrId(), itemId);
//					if (prItem.isPresent()) {
//						prItem.get().setRfqId(rfqId);
//						this.prItemRepository.save(prItem.get());
//					}
//				}
				
				// update the data in Item history table
				this.updateQuotationItemHistory(quotationItemUpdated, oldQuotationItem);
				
				log.info("Quotation Item saved successfully..." + quotationItem.toString());
				// -------------------------------- ITEM MAPPING FINISHED ---------------------------------------------------------
				
				// -------------------------------- ITEM-VENDOR MAPPING STARTED ---------------------------------------------------
				log.info("Quotation Item Vendor save Started...");
				List<QuotationItemVendor> vendors = quotationItem.getItemVendors();
				for (QuotationItemVendor vendor : vendors) {
					if (vendor.getId() == null) {
						/**
						 * Inserting the new vendor for the Item which should not have existing ID of same mapping
						 * setting the attributes of vendor from Item level(parent level)
						 */
						vendor.setQuotationId(rfqId);
						vendor.setItemId(itemId);
						vendor.setRfqNumber(rfqNumber);
						vendor.setLastModifiedBy(username);
						vendor.setCreatedBy(username);
						vendor = this.quotationItemVendorRepository.save(vendor);
						
						if (vendor == null) {
							log.error("Error while saving the RFQ Item Vendor.");
							throw new CustomMessageException("Error while saving the RFQ Item Vendor");
						}
						// update history as insert
//						this.quotationHistoryRepository.save(this.prepareQuotationHistory(rfqNumber, vendor.getId(), AppConstants.QUOTATION_ITEM_VENDOR, null, Operation.CREATE.toString(), vendor.getLastModifiedBy(), null, String.valueOf(vendor.getId())));
//						log.info("Quotation Item Vendor Inserted and History is updated.");
						
						/**
						 * Add to RFQ vendor if it is not exist in RFQ-Vendor Mapping
						 */
						Optional<QuotationVendors> quotationVendor = this.quotationVendorRepository.findByVendorIdAndQuotationIdAndIsDeleted(vendor.getVendorId(), rfqId, false);
	
						// If vendor is not exist in RFQ-Vendor mapping then add it to table
						if (!quotationVendor.isPresent()) {
							quotationVendor = Optional.of(new QuotationVendors());
							quotationVendor.get().setVendorId(vendor.getVendorId());
							quotationVendor.get().setQuotationId(rfqId);
							quotationVendor.get().setRfqNumber(rfqNumber);
							quotationVendor.get().setCreatedBy(username);
							quotationVendor.get().setLastModifiedBy(username);
							
							// Fetch the default contact values of the Supplier/Vendor
							SupplierContact supplierContact = this.masterServiceClient.findContactBySupplierIdAndIsPrimaryContact(vendor.getVendorId(), true);
							if (supplierContact != null) {
								quotationVendor.get().setContactName(supplierContact.getName());
								quotationVendor.get().setEmail(supplierContact.getEmail());
							}
							quotationVendor = Optional.ofNullable(this.quotationVendorRepository.save(quotationVendor.get()));
							
							// update history as INSERT
							if (quotationVendor.isPresent()) {
								this.quotationHistoryRepository.save(this.prepareQuotationHistory(rfqNumber, quotationVendor.get().getId(), AppConstants.QUOTATION_VENDOR, null, Operation.CREATE.toString(), quotationVendor.get().getLastModifiedBy(), null, String.valueOf(quotationVendor.get().getId())));
							} else {
								log.error("Error while Inserting the RFQ Vendor :: " + vendor.toString());
								throw new CustomMessageException("Error while Inserting the RFQ Vendor.");
							}
						}
					} else if (vendor.isDeleted()) {
						/**
						 * Delete from 
						 * 1. RFQ-Item Mapping
						 * 2. Remove the vendor From RFQ-Vendor Mapping if the vendor is not exist in other items
						 * 
						 * You must have ID if you want to delete/uncheck from the list
						 * because list should be populated from the list of objects which contains the ID
						 */
						if (vendor.getId() == null || vendor.getId() == 0) {
							continue;
						}
						
						vendor.setLastModifiedBy(username);
						QuotationItemVendor vendorUpdated = this.quotationItemVendorRepository.save(vendor);
						
						if (vendorUpdated == null) {
							log.error("Error while DELETING the RFQ Item Vendor" + vendor.toString());
							throw new CustomMessageException("Error while saving the RFQ Item Vendor");
						}
						
						// update history as delete
//						this.quotationHistoryRepository.save(this.prepareQuotationHistory(rfqNumber, vendor.getId(), AppConstants.QUOTATION_ITEM_VENDOR, null, Operation.DELETE.toString(), vendor.getLastModifiedBy(), String.valueOf(vendor.getId()), null));
//						log.info("Quotation Item Vendor Inserted and History is updated.");
						
						/**
						 * Below lines will check -
						 * If vendor is exist for Other Item of same RFQ then Do NOT delete from QUOTATION_VENDORS mapping table. (B'coz we are showing distinct ones only)
						 * If vendor is not exist for Other Item of same RFQ then delete from QUOTATION_VENDORS mapping table.
						 */
						Optional<QuotationItemVendor> quotationItemVendor = Optional.empty();
						quotationItemVendor = this.quotationItemVendorRepository.findByVendorIdAndQuotationIdAndIsDeleted(vendor.getVendorId(), vendor.getId(), false);
						
						/**
						 * If mapping exist for Same RFQ with Other Item with same Vendor Id then NO NEED to delete from RFQ_VENDOR mapping
						 * otherwise delete it from the RFQ-VENDOR mapping
						 */
						if (!quotationItemVendor.isPresent()) {
							Optional<QuotationVendors> quotationVendor = this.quotationVendorRepository.findByVendorIdAndQuotationIdAndIsDeleted(vendor.getVendorId(), rfqId, false);
							if (quotationVendor.isPresent()) {
								quotationVendor.get().setDeleted(true);
								quotationVendor = Optional.ofNullable(this.quotationVendorRepository.save(quotationVendor.get()));
								
								if (quotationVendor.isPresent()) {
									// update history as delete
									this.quotationHistoryRepository.save(this.prepareQuotationHistory(rfqNumber, quotationVendor.get().getId(), AppConstants.QUOTATION_VENDOR, null, 
											Operation.DELETE.toString(), quotationVendor.get().getLastModifiedBy(), String.valueOf(quotationVendor.get().getId()), null));
									
									log.info("RFQ Vendor saved successfully.");
								} else {
									log.error("Error while Deleting the RFQ Vendor.");
									throw new CustomMessageException("Error while Deleting the RFQ Vendor.");
								}
							}
							
						}
					}
				}
				log.info("Quotation Item Vendors saved successfully...");
				// -------------------------------- ITEM-VENDOR MAPPING FINISHED ---------------------------------------------------------
			}
		}
		
		if (isSubmitted) {
			for (QuotationPr quotationPr : quotation.getQuotationPrs()) {
				Long prId = quotationPr.getPrId();
				Long unusedItemCount = prItemRepository.findUnprocessedItemsCountForRfq(prId);
				Optional<PurchaseRequisition> purchaseRequisition = prRepository.findByIdAndIsDeleted(prId, false);
				if (unusedItemCount == 0) {
					purchaseRequisition.get().setPrStatus(TransactionStatus.PROCESSED.getTransactionStatus());
				} else {
					purchaseRequisition.get().setPrStatus(TransactionStatus.PARTIALLY_PROCESSED.getTransactionStatus());
				}
				prRepository.save(purchaseRequisition.get());
			}
		}
		// ------------------------------------ 2. Save the Quotation Items & Vendors :: FINISHED ----------------------------------------------------
		
		/**
		 * Step 4 :: store the vendors in table
		 * with defaults/overridden values
		 */
		// ------------------------------------ 3. Save the Quotation Vendors :: STARTS ----------------------------------------------------
		if (quotationUpdated.getId() != null) {
			// Iterate over the vendors Quotation
			if (CollectionUtils.isNotEmpty(quotation.getQuotationVendors())) {
				for (QuotationVendors quotationVendor : quotation.getQuotationVendors()) {
					
					Optional<QuotationVendors> vendor = this.quotationVendorRepository.findByVendorIdAndQuotationIdAndIsDeleted(quotationVendor.getVendorId(), rfqId, false);
					
					// If vendor is not exist in RFQ-Vendor mapping then add it to table
					if (vendor.isPresent()) {
						vendor.get().setMemo(quotationVendor.getMemo());
						vendor.get().setEmail(quotationVendor.getEmail());
						vendor.get().setLastModifiedBy(username);
						this.quotationVendorRepository.save(vendor.get());
					} else {
						quotationVendor.setCreatedBy(username);
						quotationVendor.setQuotationId(quotationUpdated.getId());
						quotationVendor.setRfqNumber(quotationUpdated.getRfqNumber());
						quotationVendor.setLastModifiedBy(username);
						this.quotationVendorRepository.save(quotationVendor);
					}
					// Store the existing Quotation Vendor from DB
//					Optional<QuotationVendors> oldQuotationVendor = Optional.ofNullable(null);
//					
//					if (quotationVendor.getId() == null || quotationVendor.getVendorId() == null || quotationVendor.getVendorId() == 0) {
//						// If No vendor Id exist. It means it is wrong data
//						continue;
//					}
					
					// Get the existing object using the deep copy
//					oldQuotationVendor = this.quotationVendorRepository.findByIdAndIsDeleted(quotationVendor.getId(), false);
//					if (oldQuotationVendor.isPresent()) {
//						try {
//							oldQuotationVendor = Optional.ofNullable((QuotationVendors) oldQuotationVendor.get().clone());
//						} catch (CloneNotSupportedException e) {
//							log.error("Error while Cloning the object. Please contact administrator.");
//							throw new CustomException("Error while Cloning the object. Please contact administrator.");
//						}
//					} else {
//						// if old object is not exist in DB then current object is deleted hence ignore/skip current object
//						continue;
//					}
					
//					quotationVendor.setLastModifiedBy(username);
//					quotationVendor = this.quotationVendorRepository.save(quotationVendor); 
//					
//					if (quotationVendor == null) {
//						log.info("Error while saving the Quotation Vendor.");
//						throw new CustomMessageException("Error while saving the Quotation vendor.");
//					}
//					
//					// update the data in history table
//					this.updateQuotationVendorHistory(quotationVendor, oldQuotationVendor);
					
					log.info("RFQ Vendor is updated successfully.");
				}
			}			
		}
		// ------------------------------------ 3. Save the Quotation Vendors :: FINISHED ----------------------------------------------------
		
		// --------------- save RFQ general Information : STARTED ----------------------------
		List<QuotationGenaralInfo> infos = quotation.getQuotationInfos();
		if (CollectionUtils.isNotEmpty(infos)) {
			for (QuotationGenaralInfo quotationGenaralInfo : infos) {
				this.save(quotationGenaralInfo, rfqId, rfqNumber);
			}
		}
		// --------------- save RFQ general Information : FINISHED ----------------------------
		quotation.setSubmitted(isSubmitted);
		
		
		/**
		 * Create QA as per user's wish
		 */
		if (quotation.isCreateQa()) {
			log.error("Going to save QA from RFQ Page.");
			QuotationAnalysis quotationAnalysis = new QuotationAnalysis();
			quotationAnalysis.setRfqId(quotationUpdated.getId());
			quotationAnalysis.setSubsidiaryId(quotationUpdated.getSubsidiaryId());
			quotationAnalysis.setQaDate(new Date());
			quotationAnalysis.setCreator(quotationUpdated.getCreator());
			
//			List<QuotationAnalysisItem> quotationAnalysisItems = new ArrayList<QuotationAnalysisItem>();
//			for (QuotationItem quotationItem : quotation.getQuotationItems()) {
//				QuotationAnalysisItem quotationAnalysisItem = new QuotationAnalysisItem();
//				
//				quotationAnalysisItem.setItemId(quotationItem.getItemId());
//				quotationAnalysisItem.setCurrency(quotationItem.getCurrency());
//				quotationAnalysisItem.setQuantity(quotationItem.getQuantity());
//				quotationAnalysisItem.setPrId(quotationItem.getPrId());
//				quotationAnalysisItem.setPrLocationId(quotationItem.getPrLocation());
////				approvedSupplier;
////				private String uom;
////				private Double exchangeRate;
////				private Double ratePerUnit;
////				private Double actualRate;
////				private OffsetDateTime expectedDate;
////				private Integer leadTime;
//
//				
//				quotationAnalysisItems.add(quotationAnalysisItem);
//			}
//			
//			quotationAnalysis.setQuotationAnalysisItems(quotationAnalysisItems);
			quotationAnalysisService.save(quotationAnalysis);
			log.error("QA is Created from RFQ Page.");
		}
		
		return quotation;
	}
	
	private void save(QuotationGenaralInfo quotationGenaralInfo, Long quotationId, String rfqNumber) {
		// Store the existing Quotation from DB
		Optional<QuotationGenaralInfo> oldQuotationInfo = Optional.empty();
		String username = CommonUtils.getLoggedInUsername();

		if (quotationGenaralInfo.getId() == null) {
			quotationGenaralInfo.setCreatedBy(username);
		} else {
			// Get the existing object using the deep copy
			oldQuotationInfo = this.quotationGeneralInfoRepository.findByIdAndIsDeleted(quotationGenaralInfo.getId(), false);
			if (oldQuotationInfo.isPresent()) {
				try {
					oldQuotationInfo = Optional.ofNullable((QuotationGenaralInfo) oldQuotationInfo.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		quotationGenaralInfo.setLastModifiedBy(username);
		quotationGenaralInfo.setQuotationId(quotationId);
		quotationGenaralInfo.setQuotationNumber(rfqNumber);
		QuotationGenaralInfo quotationUpdated = null;
		try {
			quotationUpdated = this.quotationGeneralInfoRepository.save(quotationGenaralInfo);
		}catch (DataIntegrityViolationException e) {
			log.error("Quotation unique constrain violetd." + e.getMostSpecificCause());
			throw new CustomException("Quotation unique constrain violetd :" + e.getMostSpecificCause());
		}
		if (quotationUpdated == null) {
			log.error("Error while saving the Quotation Info " + quotationGenaralInfo.toString());
			throw new CustomMessageException("Error while saving the Quotation Info");
		}
		log.info("Quotation Info successfully." + quotationUpdated.toString());
		
		this.updateQuotationHistory(quotationUpdated, oldQuotationInfo);
	}

	private void updateQuotationHistory(QuotationGenaralInfo quotationInfo, Optional<QuotationGenaralInfo> oldQuotationInfo) {
		if (oldQuotationInfo.isPresent()) {
			if (!quotationInfo.isDeleted()) {
				// insert the updated fields in history table
				List<QuotationHistory> quotationHistories = new ArrayList<QuotationHistory>();
				try {
					quotationHistories = oldQuotationInfo.get().compareFields(quotationInfo);
					if (CollectionUtils.isNotEmpty(quotationHistories)) {
						this.quotationHistoryRepository.saveAll(quotationHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
				}
			} else {
				this.quotationHistoryRepository.save(this.prepareQuotationHistory(quotationInfo.getQuotationNumber(), quotationInfo.getId(), AppConstants.QUOTATION_GENERAL_INFO, null, Operation.DELETE.toString(), quotationInfo.getLastModifiedBy(), String.valueOf(quotationInfo.getId()), null));
			}
		} else {
			// Insert in history table as Operation - INSERT 
			this.quotationHistoryRepository.save(this.prepareQuotationHistory(quotationInfo.getQuotationNumber(), quotationInfo.getId(), AppConstants.QUOTATION_GENERAL_INFO, null, Operation.CREATE.toString(), quotationInfo.getLastModifiedBy(), null, String.valueOf(quotationInfo.getId())));
		}
		log.info("Quotation General Info History is updated successfully");	
	}

	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Quotation Vendors is new 
	 * Add entry as a Update if Quotation Vendors is exists
	 * 
	 * @param quotationVendor
	 * @param oldQuotationVendor
	 */
//	private void updateQuotationVendorHistory(QuotationVendors quotationVendor, Optional<QuotationVendors> oldQuotationVendor) {
//		if (oldQuotationVendor.isPresent()) {
//			// insert the updated fields in history table
//			List<QuotationHistory> quotationHistories = new ArrayList<QuotationHistory>();
//			try {
//				quotationHistories = oldQuotationVendor.get().compareFields(quotationVendor);
//				if (CollectionUtils.isNotEmpty(quotationHistories)) {
//					this.quotationHistoryRepository.saveAll(quotationHistories);
//				}
//			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//				log.error("Error while comparing the new and old objects. Please contact administrator.");
//				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
//			}
//			log.info("Quotation Vendors History is updated successfully");
//		} else {
//			// Insert in history table as Operation - INSERT 
//			this.quotationHistoryRepository.save(this.prepareQuotationHistory(quotationVendor.getRfqNumber(), quotationVendor.getId(), AppConstants.QUOTATION_VENDOR, null, Operation.CREATE.toString(), quotationVendor.getLastModifiedBy(), null, String.valueOf(quotationVendor.getId())));
//		}
//	}

	/**
	 * Validate the Quotion with below mentioned validation
	 * 1. RFQ Date should be greater than PR Date
	 * 2. Bid Start Date Time should be lesser than Bid End Date Time 
	 * @param quotation
	 */
	private boolean validateQuotation(Quotation quotation) {
		boolean isQuotationValid = true;

		// 1. Validation of RFQ Date should be greater than PR Date
		for (QuotationPr quotationPr : quotation.getQuotationPrs()) {
			PurchaseRequisition purchaseRequisition = this.purchaseRequisitionService.findById(quotationPr.getPrId());
			int dateStatus = CommonUtils.compareDates(quotation.getRfqDate(), purchaseRequisition.getPrDate());
			
			if (dateStatus == -1) {
				isQuotationValid = false;
				log.error("RFQ Date should be greater than PR Date.");
				throw new CustomBadRequestException("RFQ Date should be greater than PR Date.");
			}			
		}
		// 2. Validation of Bid Start Date Time should be lesser than Bid End Date Time 
		if (quotation.getBidOpenDate().isAfter(quotation.getBidCloseDate())) {
			isQuotationValid = false;
			log.error("RFQ Bid Close Date Time should be greater than RFQ Bid Open Date Time.");
			throw new CustomBadRequestException("RFQ Bid Close Date Time should be greater than RFQ Bid Open Date Time.");
		}
		
		return isQuotationValid;
	}

	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Quotation is new 
	 * Add entry as a Update if Quotation is exists
	 * 
	 * @param quotation
	 * @param oldQuotation
	 */
	private void updateQuotationHistory(Quotation quotation, Optional<Quotation> oldQuotation) {
		if (oldQuotation.isPresent()) {
			// insert the updated fields in history table
			List<QuotationHistory> quotationHistories = new ArrayList<QuotationHistory>();
			try {
				quotationHistories = oldQuotation.get().compareFields(quotation);
				if (CollectionUtils.isNotEmpty(quotationHistories)) {
					this.quotationHistoryRepository.saveAll(quotationHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("Quotation History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT 
			this.quotationHistoryRepository.save(this.prepareQuotationHistory(quotation.getRfqNumber(), null, AppConstants.QUOTATION, null, Operation.CREATE.toString(), quotation.getLastModifiedBy(), null, String.valueOf(quotation.getId())));
		}
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
	@Override
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

	
	@Override
	public Quotation findById(Long id) {
		Optional<Quotation> quotation = Optional.empty();
		quotation = this.quotationRepository.findByIdAndIsDeleted(id, false);
		if (!quotation.isPresent()) {
			log.info("RFQ is not exist for id - " + id);
			throw new CustomMessageException("RFQ is not exist for id - " + id);
		}
		// Get all the items with details
		
		String errorMessage = "";
		List<QuotationPr> quotationPrs = this.quotationPrRepository.findAllByQuotationIdAndIsDeleted(id, false);
		for (QuotationPr quotationPr : quotationPrs) {
			Optional<PurchaseRequisition> pr = this.prRepository.findByIdAndIsDeleted(quotationPr.getPrId(), false);
			if (pr.isPresent()) { 
				quotationPr.setPrNumber(pr.get().getPrNumber());
				if (TransactionStatus.DRAFT.getTransactionStatus().equalsIgnoreCase(quotation.get().getStatus())) {
					if (TransactionStatus.PROCESSED.getTransactionStatus().equalsIgnoreCase(pr.get().getPrStatus())) {
						errorMessage +=	" PR - " + pr.get().getPrNumber() + " is completly processed. ";						
					}
				}
			}
		}
		quotation.get().setQuotationPrs(quotationPrs);
		quotation.get().setErrorMessage(errorMessage);
		List<QuotationItem> quotationItems = this.quotationItemRepository.getItemWithDetails(id);
		for (QuotationItem quotationItem : quotationItems) {
			Optional<PrItem> prItem = this.prItemRepository.findByPrIdAndItemIdAndIsDeleted(quotationItem.getPrId(), quotationItem.getItemId(), false);
			if (prItem.isPresent()) {
				quotationItem.setRemainedQuantity(prItem.get().getRemainedQuantity());
			}
			quotationItem.setItemVendors(this.quotationItemVendorRepository.findByItemIdAndQuotationIdAndIsDeleted(quotationItem.getItemId(), quotationItem.getQuotationId(), false));
		}
		quotation.get().setQuotationItems(quotationItems);
		
		// Get all the distinct vendors
		quotation.get().setQuotationVendors(this.quotationVendorRepository.getAllVendorsByQuotationIdAndIsDeleted(id, false));
		
		List<QuotationGenaralInfo> quotationInfos = this.quotationGeneralInfoRepository.findAllByQuotationIdAndIsDeleted(id, false);
		quotation.get().setQuotationInfos(quotationInfos );
		
		boolean isSubmitted = false;
		if (StringUtils.isNotEmpty(quotation.get().getStatus()) && !TransactionStatus.DRAFT.getTransactionStatus().equalsIgnoreCase(quotation.get().getStatus())) {
			isSubmitted = true;
		}
		quotation.get().setSubmitted(isSubmitted);
		
		// set error message if RFQ draft request is for which pr is fully processed
		
		return quotation.get();
	}

	
	/**
	 * Calls when user add row(item) in RFQ page i.e. for the mapping
	 * 1. Save the overridden values of the Item (INSERT/UPDATE/DELETE supported for history)
	 * 2. Save the mapping of Item with vendors in next Step (INSERT/DELETE supported for history) 
	 */
	@Override
	public QuotationItem save(QuotationItem quotationItem) {
		String username = CommonUtils.getLoggedInUsername();
		// -------------------------------- ITEM MAPPING STARTED ---------------------------------------------------------
		log.info("Quotation Item save Started...");
		// Store the existing Quotation from DB
		Optional<QuotationItem> oldQuotationItem = Optional.ofNullable(null);

		if (quotationItem.getId() == null) {
			quotationItem.setCreatedBy(username);
		} else {
			// Get the existing object using the deep copy
			oldQuotationItem = this.quotationItemRepository.findByIdAndIsDeleted(quotationItem.getId(), false);
			if (oldQuotationItem.isPresent()) {
				try {
					oldQuotationItem = Optional.ofNullable((QuotationItem) oldQuotationItem.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		quotationItem.setLastModifiedBy(username);
		QuotationItem quotationItemUpdated = this.quotationItemRepository.save(quotationItem);

		if (quotationItemUpdated == null) {
			log.info("Error while saving the Quotation Item.");
			throw new CustomMessageException("Error while saving the Quotation Item.");
		}
		// useful to set in vendor list objects
		Long quotationId = quotationItemUpdated.getQuotationId();
		Long itemId = quotationItemUpdated.getItemId();
		String rfqNumber = quotationItemUpdated.getRfqNumber();
		
		// update the data in Item history table
		this.updateQuotationItemHistory(quotationItemUpdated, oldQuotationItem);
		
		log.info("Quotation Item saved successfully...");
		// -------------------------------- ITEM MAPPING FINISHED ---------------------------------------------------------
		// -------------------------------- ITEM-VENDOR MAPPING STARTED ---------------------------------------------------
		log.info("Quotation Item Vendor save Started...");
		List<QuotationItemVendor> vendors = quotationItem.getItemVendors();
		for (QuotationItemVendor vendor : vendors) {
			if (vendor.getId() == null) {
				/**
				 * Inserting the new vendor for the Item which should not have existing ID of same mapping
				 * setting the attributes of vendor from Item level(parent level)
				 */
				vendor.setQuotationId(quotationId);
				vendor.setItemId(itemId);
				vendor.setRfqNumber(rfqNumber);
				vendor.setLastModifiedBy(username);
				vendor.setCreatedBy(username);
				vendor = this.quotationItemVendorRepository.save(vendor);
				
				// update history as insert
				this.quotationHistoryRepository.save(this.prepareQuotationHistory(rfqNumber, vendor.getId(), AppConstants.QUOTATION_VENDOR, null, Operation.CREATE.toString(), vendor.getLastModifiedBy(), null, String.valueOf(vendor.getId())));
			} else if (vendor.isDeleted()) {
				/**
				 * Delete from 
				 * 1. RFQ-Item Mapping
				 * 2. Remove the vendor From RFQ-Vendor Mapping if the vendor is not exist in other items
				 * 
				 * You must have ID if you want to delete/uncheck from the list
				 * because list should be populated from the list of objects which contains the ID
				 */
				if (vendor.getId() == null || vendor.getId() == 0) {
					continue;
				}
				vendor.setLastModifiedBy(username);
				this.quotationItemVendorRepository.save(vendor);
				
				// update history as delete
				this.quotationHistoryRepository.save(this.prepareQuotationHistory(rfqNumber, vendor.getId(), AppConstants.QUOTATION_VENDOR, null, Operation.DELETE.toString(), vendor.getLastModifiedBy(), String.valueOf(vendor.getId()), null));
				
				/**
				 * Below lines will check -
				 * If vendor is exist for Other Item of same RFQ then Do NOT delete from QUOTATION_VENDORS mapping table. (B'coz we are showing distinct ones only)
				 * If vendor is not exist for Other Item of same RFQ then delete from QUOTATION_VENDORS mapping table.
				 */
				Optional<QuotationItemVendor> quotationItemVendor = Optional.empty();
				quotationItemVendor = this.quotationItemVendorRepository.findByVendorIdAndQuotationIdAndIsDeleted(vendor.getVendorId(), vendor.getId(), false);
				
				if (!quotationItemVendor.isPresent()) {
					// this.quotationVendorRepository.findByVendorIdAndRfqNumberAndIsDeleted();
				}
			}
		}
		log.info("Quotation Item Vendors saved successfully...");
		// -------------------------------- ITEM-VENDOR MAPPING FINISHED ---------------------------------------------------------
		return quotationItem;
	}

	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Quotation Item is new 
	 * Add entry as a Update if Quotation Item is exists
	 * 
	 * @param quotationItem
	 * @param oldQuotationItem
	 */
	private void updateQuotationItemHistory(QuotationItem quotationItem, Optional<QuotationItem> oldQuotationItem) {
		if (oldQuotationItem.isPresent()) {
			// insert the updated fields in history table
			List<QuotationHistory> quotationHistories = new ArrayList<QuotationHistory>();
			try {
				quotationHistories = oldQuotationItem.get().compareFields(quotationItem);
				if (CollectionUtils.isNotEmpty(quotationHistories)) {
					this.quotationHistoryRepository.saveAll(quotationHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("Quotation History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT 
			this.quotationHistoryRepository.save(this.prepareQuotationHistory(quotationItem.getRfqNumber(), quotationItem.getId(), AppConstants.QUOTATION_ITEM, null, Operation.CREATE.toString(), quotationItem.getLastModifiedBy(), null, String.valueOf(quotationItem.getId())));
		}
	}

	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<Quotation> quotations = new ArrayList<Quotation>();

		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest);

		// get list
		quotations = this.quotationDao.findAll(whereClause, paginationRequest);

		// getting count
		Long totalRecords = this.quotationDao.getCount(whereClause);

		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(),
				quotations, totalRecords);
	}

	private String prepareWhereClause(PaginationRequest paginationRequest) {
		Map<String, ?> filters = paginationRequest.getFilters();

		String subsidiaryName = null;
		String bidType = null;
		String fromDate = null;
		String toDate = null;

		if (filters.containsKey(FilterNames.SUBSIDIARY_NAME))
			subsidiaryName = (String) filters.get(FilterNames.SUBSIDIARY_NAME);
		
		if (filters.containsKey(FilterNames.TYPE))
			bidType = (String) filters.get(FilterNames.TYPE);
		
		if (filters.containsKey(FilterNames.FROM_DATE)) {
			fromDate = (String) filters.get(FilterNames.FROM_DATE);
		}
		
		if (filters.containsKey(FilterNames.TO_DATE))
			toDate = (String) filters.get(FilterNames.TO_DATE);

		StringBuilder whereClause = new StringBuilder(" AND q.isDeleted is false ");
		if (StringUtils.isNotEmpty(subsidiaryName)) {
			whereClause.append(" AND lower(s.name) like lower ('%").append(subsidiaryName).append("%')");
		}
		if (StringUtils.isNotEmpty(bidType)) {
			whereClause.append(" AND lower(q.bidType) like lower ('%").append(bidType).append("%')");
		}
		if (fromDate != null) {
			whereClause.append(" AND q.bidOpenDate >= '").append(fromDate).append("' ");
		}
		if (toDate != null) {
			whereClause.append(" AND q.bidCloseDate <= '").append(toDate).append("' ");
		}
		
		return whereClause.toString();
	}

	@Override
	public List<QuotationHistory> findHistoryById(String rfqNumber, Pageable pageable) {
		List<QuotationHistory> histories = this.quotationHistoryRepository.findByRfqNumberOrderById(rfqNumber, pageable);
		String createdBy = histories.get(0).getLastModifiedBy();
		histories.forEach(e->{
			e.setCreatedBy(createdBy);
		});
		return histories;
	}

//	@Override
//	public Quotation findByRfqNumber(String rfqNumber) {
//		List<Quotation> quotations = new ArrayList<Quotation>();
//		Optional<Quotation> quotation = Optional.empty();
//		
//		quotations = this.quotationRepository.findByRfqNumberAndIsDeleted(rfqNumber, false);
//		if (CollectionUtils.isNotEmpty(quotations)) {
//			quotation = Optional.of(quotations.get(0));
//		}  else {
//			log.info("RFQ is not exist for RFQ Number - " + rfqNumber);
//			throw new CustomMessageException("RFQ is not exist for RFQ Number - " + rfqNumber);
//		}
//		quotation.get().setQuotationPrs(this.quotationPrRepository.findAllByQuotationIdAndIsDeleted(quotation.get().getId(), false));
//		// Get all the items with details
//		List<QuotationItem> quotationItems = this.quotationItemRepository.getItemWithDetails(quotation.get().getRfqNumber());
//		for (QuotationItem quotationItem : quotationItems) {
//			quotationItem.setItemVendors(this.quotationItemVendorRepository.findByItemIdAndRfqNumberAndIsDeleted(quotationItem.getItemId(), quotationItem.getRfqNumber(), false));
//		}
//		quotation.get().setQuotationItems(quotationItems);
//
//		// find quotation general info
//		List<QuotationGenaralInfo> quotationInfos = this.quotationGeneralInfoRepository.findAllByQuotationNumberAndIsDeleted(rfqNumber, false);
//		quotation.get().setQuotationInfos(quotationInfos );
//		return quotation.get();
//	}

	/**
	 * return all the unique vendors of the RFQ
	 */
	@Override
	public List<QuotationVendors> findVendorsByRfqId(Long rfqId) {
		List<QuotationVendors> quotationVendors = new ArrayList<QuotationVendors>();
		quotationVendors = this.quotationVendorRepository.getAllVendorsByQuotationIdAndIsDeleted(rfqId, false);
		return quotationVendors;
	}

	@Override
	public List<QuotationItem> findQuotationItemByRfqIdAndVendor(Long rfqId, Long vendorId) {
		List<QuotationItem> quotationItems = new ArrayList<QuotationItem>();
		quotationItems = this.quotationItemRepository.findByRfqIdAndVendorIdAndIsDeleted(vendorId, rfqId, false);
		return quotationItems;
	}
	
	@Override
	public List<QuotationItem> findQuotationItemByRfqId(Long rfqId) {
		List<QuotationItem> quotationItems = new ArrayList<QuotationItem>();
		quotationItems = this.quotationItemRepository.findByRfqIdAndIsDeleted(rfqId, false);
		return quotationItems;
	}

	/**
	 * Find RFQ, Update status, Update history, return the Items with Vendors
	 */
	@Override
	public Quotation closeQuotation(Long id) {
		Optional<Quotation> quotation = Optional.empty();
		quotation = this.quotationRepository.findByIdAndIsDeleted(id, false);
		// check RFQ is existing for given ID
		if (!quotation.isPresent()) {
			log.info("RFQ is not exist for id - " + id);
			throw new CustomMessageException("RFQ is not exist for id - " + id);
		}
		log.info("RFQ Found to update the status against ID : " + id);

		/** 
		 * 27-06-2022
		 * If status is updated from the open to close then this is manual close
		 * Then only we are setting the current RFQ as deleted
		 */
		if (TransactionStatus.SUBMITTED.getTransactionStatus().equalsIgnoreCase(quotation.get().getStatus())) {
			quotation.get().setDeleted(true);
		}
		
		quotation.get().setStatus(TransactionStatus.CLOSE.getTransactionStatus());
		quotation = Optional.ofNullable(this.quotationRepository.save(quotation.get()));
		
		// check is there any error while saving 
		if (!quotation.isPresent()) {
			log.info("Error while updating the Status");
			throw new CustomMessageException("Error while updating the Status");
		}
		log.info("RFQ Status is updated against ID : " + id);
		
		// update the history
		this.quotationHistoryRepository.save(this.prepareQuotationHistory(quotation.get().getRfqNumber(), null, AppConstants.QUOTATION, "Status", Operation.UPDATE.toString(), quotation.get().getLastModifiedBy(), TransactionStatus.OPEN.getTransactionStatus(), quotation.get().getStatus()));
		log.info("RFQ History is updated.");
		
		// Get all the items with details
		List<QuotationItem> quotationItems = this.quotationItemRepository.getItemWithDetails(quotation.get().getId());
		for (QuotationItem quotationItem : quotationItems) {
			quotationItem.setItemVendors(this.quotationItemVendorRepository.findByItemIdAndQuotationIdAndIsDeleted(quotationItem.getItemId(), quotationItem.getQuotationId(), false));
		}
		quotation.get().setQuotationItems(quotationItems);
		
		// Get all the distinct vendors
		quotation.get().setQuotationVendors(this.quotationVendorRepository.getAllVendorsByQuotationIdAndIsDeleted(quotation.get().getId(), false));
		
		return quotation.get();
	}

	@Override
	public void generateQuotation(GenerateRfqPoRequest generateRfqPoRequest) {
		List<RfqPoRequest> rfqPoRequests = generateRfqPoRequest.getRfqPoRequests();
		boolean isCreateCommonPo = generateRfqPoRequest.isCreateCommonRfqPo();
		
		if (isCreateCommonPo) {
			Map<String, String> groupedPr = new HashMap<String, String>();
			
			// create grouping as per criteria
			for (RfqPoRequest rfqPoRequest : rfqPoRequests) {
				String keyName = rfqPoRequest.getPrCurrency() + "_" + rfqPoRequest.getBidType();
				String prIds = null;
				if (groupedPr.containsKey(keyName)) {
					prIds = groupedPr.get(keyName) + "|" + String.valueOf(rfqPoRequest.getPrId());
				} else {
					prIds = String.valueOf(rfqPoRequest.getPrId());
				}
				groupedPr.put(keyName, prIds);
			}
			
			// iterate over the grouped pr which will belongs to common PO
			for (Map.Entry<String, String> entry : groupedPr.entrySet()) {
				// fetch the data only for the first PR. which is common for other which is in same PO
				String[] prIds = entry.getValue().split("\\|");
				
				Optional<RfqPoRequest> rfqPoRequest = rfqPoRequests.stream().filter(e -> e.getPrId() == Long.parseLong(prIds[0])).findFirst();
				if (rfqPoRequest.isPresent()) this.prepareRfq(rfqPoRequest.get(), entry.getValue());
			}
		} else {
			// create seprate for each rfq
			this.saveSeparateRfq(rfqPoRequests);
		}
	}

	private void saveSeparateRfq(List<RfqPoRequest> rfqPoRequests) {
		for (RfqPoRequest rfqPoRequest : rfqPoRequests) {
			this.prepareRfq(rfqPoRequest, String.valueOf(rfqPoRequest.getPrId()));
		}
	}
	
	/**
	 * Prepare below objects & their histories -
	 * 1. RFQ
	 * 2. RFQ-PR mapping
	 * 3. RFQ-Items Mapping
	 * 4. RFQ-Vendors Mapping (NOT Valid : vendor is selectable from User input)
	 * @param rfqRequest
	 * @return
	 */
	private Quotation prepareRfq(RfqPoRequest rfqRequest, String prIdStr) {
		try {
			String username = CommonUtils.getLoggedInUsername();
			String transactionalDate = CommonUtils.convertDateToFormattedString(rfqRequest.getTransactionalDate());
			String rfqNumber = this.setupServiceClient.getDocumentSequenceNumber(transactionalDate , rfqRequest.getSubsidiaryId(), FormNames.RFQ.getFormName(), false);
			log.info("RFQ Number is genrated : " + rfqNumber);
			
			// 1. Quotation
			Quotation quotation = new Quotation();
			quotation.setRfqNumber(rfqNumber);
			quotation.setSubsidiaryId(rfqRequest.getSubsidiaryId());
			quotation.setRfqDate(rfqRequest.getTransactionalDate());
			quotation.setBidType(rfqRequest.getBidType());
			quotation.setCurrency(rfqRequest.getPrCurrency());
			quotation.setLocationId(rfqRequest.getPrLocationId());
			quotation.setStatus(TransactionStatus.DRAFT.getTransactionStatus());
			quotation.setCreatedBy(username);
			quotation.setLastModifiedBy(username);
			quotation = this.quotationRepository.save(quotation);
			log.info("Quotation is saved : " + quotation.toString());
			// History of Quotation
			this.quotationHistoryRepository.save(this.prepareQuotationHistory(rfqNumber, null, AppConstants.QUOTATION, null, Operation.CREATE.toString(), username, null, rfqNumber));
			log.info("Quotation History is saved.");
			
			Long quotationId = quotation.getId();
			
			if (StringUtils.isEmpty(prIdStr)) {
				log.error("PR-Number should not be empty while generating the RFQ.");
				throw new CustomMessageException("PR-Number should not be empty while generating the RFQ.");
			}
			
			String[] prIds = prIdStr.split("\\|");
			
			for (int i=0; i < prIds.length; i++) {
				Long prId = Long.parseLong(prIds[i]);
				// 2. Quotation PR Mapping
				QuotationPr quotationPr = new QuotationPr();
				quotationPr.setQuotationId(quotationId);
				quotationPr.setPrId(prId);
				quotationPr.setQuotationNumber(rfqNumber);
				quotationPr.setCreatedBy(username);
				quotationPr.setLastModifiedBy(username);
				quotationPr = this.quotationPrRepository.save(quotationPr);
				log.info("Quotation-PR mapping is saved : " + quotationPr.toString());
				
				// History of quotationPr
				this.quotationHistoryRepository.save(this.prepareQuotationHistory(rfqNumber, quotationPr.getId(), AppConstants.QUOTATION_PR_NUMBER, null, Operation.CREATE.toString(), username, null, prIds[i]));
				log.info("Quotation-PR Mapping History is saved.");
				
				Optional<PurchaseRequisition> pr = this.prRepository.findByIdAndIsDeleted(quotationPr.getPrId(), false);
				if (pr.isPresent()) {
					pr.get().setUsedFor(FormNames.RFQ.getFormName());
					this.prRepository.save(pr.get());
				}
				
				// 3. Quotation Item Mapping
				PurchaseRequisition purchaseRequisition = this.purchaseRequisitionService.findById(prId);//(prNumbers[i]);
				if (purchaseRequisition != null) {
					List<PrItem> prItems = purchaseRequisition.getPrItems();
					if (CollectionUtils.isNotEmpty(prItems)) {
						for (PrItem prItem : prItems) {
							if (prItem.getRemainedQuantity() > 0) {
								QuotationItem quotationItem = new QuotationItem();
								quotationItem.setQuotationId(quotationId);
								quotationItem.setRfqNumber(rfqNumber);
								quotationItem.setItemId(prItem.getItemId());
								quotationItem.setQuantity(prItem.getQuantity());
								quotationItem.setCurrency(purchaseRequisition.getCurrency());
								quotationItem.setReceivedDate(rfqRequest.getTransactionalDate());
								quotationItem.setPrNumber(purchaseRequisition.getPrNumber());
								quotationItem.setPrId(prId);
								quotationItem.setPrLocation(purchaseRequisition.getLocationId());
								quotationItem.setCreatedBy(username);
								quotationItem.setLastModifiedBy(username);
								
								quotationItem = this.quotationItemRepository.save(quotationItem);
								log.info("Quotation Item is saved : " + quotationItem.toString());
								// History of Quotation Item
								this.quotationHistoryRepository.save(this.prepareQuotationHistory(rfqNumber, quotationItem.getId(), AppConstants.QUOTATION_ITEM, null, Operation.CREATE.toString(), username, null, String.valueOf(quotationItem.getId())));
								log.info("Quotation Item history is saved.");
							}
						}
					}
				}			
			}
			log.info("All RFQ's auto generated successfully.");
			return quotation;
		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Error while generating the RFQ. " + ex.getMessage());
			
			String errorExceptionMessage = ex.getLocalizedMessage();
			if (errorExceptionMessage == null) errorExceptionMessage = ex.toString();
			
			throw new CustomException(errorExceptionMessage);
		}
	}

	@Override
	public List<Quotation> findBySubsidiaryId(Long subsidiaryId) {
		List<String> status = new ArrayList<String>();
		status.add(TransactionStatus.DRAFT.getTransactionStatus());
		status.add(TransactionStatus.CLOSE.getTransactionStatus());
		status.add(TransactionStatus.QA_CREATED.getTransactionStatus());
		return this.quotationRepository.findBySubsidiaryIdAndStatusNotIn(subsidiaryId, status);
	}

	@Override
	public String sendMail(Long id) {
		Optional<Quotation> quotation = this.quotationRepository.findByIdAndIsDeleted(id, false);
		if (!quotation.isPresent()) {
			log.error("RFQ for given ID is not exist.");
			throw new CustomMessageException("RFQ for given ID is not exist.");
		}
		if (AppConstants.BID_CLOSE.equalsIgnoreCase(quotation.get().getBidType())
				&& StringUtils.isNotEmpty(quotation.get().getStatus()) 
				&& !TransactionStatus.DRAFT.getTransactionStatus().equalsIgnoreCase(quotation.get().getStatus())) {
			List<String> mails = this.quotationVendorRepository.findDistinctMailByRfqId(id, false);
			String senderMail = String.join(",", mails);
			String subject = "RFQ Created.";
			String body = "Hi,<br><br>This is sample test mail.<br><br><b>Regards</b>,<br>Team Monstarbill";
			try {
				CommonUtils.sendMail(senderMail, null, subject, body);
			} catch (Exception e) {
				e.printStackTrace();
				log.error("Error while sending the mail.");
				throw new CustomException("Error while sending the mail.");
			}			
		} else {
			throw new CustomException("RFQ Bid Type should be Closed & RFQ Should be Submitted to send Mail.");
		}
		
		return "Mail sent successfully";
	}

	@Override
	public String sendNotification(Long id) {
		Optional<Quotation> quotation = this.quotationRepository.findByIdAndIsDeleted(id, false);
		if (!quotation.isPresent()) {
			log.error("Quotation for given ID is not exist.");
			throw new CustomMessageException("Quotation for given ID is not exist.");
		}
		if (AppConstants.BID_OPEN.equalsIgnoreCase(quotation.get().getBidType())
				&& StringUtils.isNotEmpty(quotation.get().getStatus()) 
				&& !TransactionStatus.DRAFT.getTransactionStatus().equalsIgnoreCase(quotation.get().getStatus())) {
			quotation.get().setNoticationSent(true);
			this.quotationRepository.save(quotation.get());
		} else {
			throw new CustomException("RFQ Bid Type should be Open & RFQ Should be Submitted to send Notification.");
		}
		
		return "Notification sent successfully";
	}

	@Override
	public String sendMailToRfqSupplier(String senderMail) {
		String subject = "RFQ Vendor Notification";
		String body = "Hi,<br><br>This is sample test mail.<br><br><b>Regards</b>,<br>Team Monstarbill";
		try {
			CommonUtils.sendMail(senderMail, null, subject, body);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error while sending the mail.");
			throw new CustomException("Error while sending the mail.");
		}

		return "Mail sent successfully";
	}
}
