package com.monstarbill.procure.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.monstarbill.procure.commons.AppConstants;
import com.monstarbill.procure.commons.CommonUtils;
import com.monstarbill.procure.commons.CustomBadRequestException;
import com.monstarbill.procure.commons.CustomException;
import com.monstarbill.procure.commons.CustomMessageException;
import com.monstarbill.procure.commons.ExcelHelper;
import com.monstarbill.procure.commons.FilterNames;
import com.monstarbill.procure.dao.PurchaseRequisitionDao;
import com.monstarbill.procure.enums.FormNames;
import com.monstarbill.procure.enums.Operation;
import com.monstarbill.procure.enums.TransactionStatus;
import com.monstarbill.procure.feignclient.MasterServiceClient;
import com.monstarbill.procure.feignclient.SetupServiceClient;
import com.monstarbill.procure.models.Item;
import com.monstarbill.procure.models.Location;
import com.monstarbill.procure.models.PrItem;
import com.monstarbill.procure.models.PurchaseRequisition;
import com.monstarbill.procure.models.PurchaseRequisitionHistory;
import com.monstarbill.procure.payload.request.ApprovalRequest;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.ApprovalPreference;
import com.monstarbill.procure.payload.response.IdNameResponse;
import com.monstarbill.procure.payload.response.PaginationResponse;
import com.monstarbill.procure.repository.PrItemRepository;
import com.monstarbill.procure.repository.PurchaseRequisitionHistoryRepository;
import com.monstarbill.procure.repository.PurchaseRequisitionRepository;
import com.monstarbill.procure.service.PurchaseRequisitionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class PurchaseRequisitionServiceImpl implements PurchaseRequisitionService {

	@Autowired
	private PurchaseRequisitionRepository prRepository;
	
	@Autowired
	private PurchaseRequisitionHistoryRepository prHistoryRepository;
	
	@Autowired
	private PurchaseRequisitionDao purchaseRequisitionDao;
	
	@Autowired
	private PrItemRepository prItemRepository;
	
	@Autowired
	private SetupServiceClient setupServiceClient;
	
	@Autowired
	private MasterServiceClient masterServiceClient;
	
	private static final List<String> PR_TYPES = Arrays.asList("Project", "Administrative", "Pre-Sale");
	
	@Override
	public PurchaseRequisition save(PurchaseRequisition purchaseRequisition) {
		Optional<PurchaseRequisition> oldPurchaseRequisition = Optional.ofNullable(null);

		if (CollectionUtils.isEmpty(purchaseRequisition.getPrItems())) {
			throw new CustomBadRequestException("PR should contain atlead one item.");
		}
		
		if (purchaseRequisition.getId() == null) {
			purchaseRequisition.setCreatedBy(CommonUtils.getLoggedInUsername());
			purchaseRequisition.setPrStatus(TransactionStatus.OPEN.getTransactionStatus());
			String transactionalDate = CommonUtils.convertDateToFormattedString(purchaseRequisition.getPrDate());
			String documentSequenceNumber = this.setupServiceClient.getDocumentSequenceNumber(transactionalDate, purchaseRequisition.getSubsidiaryId(), FormNames.PR.getFormName(), false);
			if (StringUtils.isEmpty(documentSequenceNumber)) {
				throw new CustomMessageException("Please validate your configuration to generate the PR Number");
			}
			purchaseRequisition.setPrNumber(documentSequenceNumber);
		} else {
			// Get the existing object using the deep copy
			oldPurchaseRequisition = this.prRepository.findByIdAndIsDeleted(purchaseRequisition.getId(), false);
			if (oldPurchaseRequisition.isPresent()) {
				try {
					oldPurchaseRequisition = Optional.ofNullable((PurchaseRequisition) oldPurchaseRequisition.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}

		purchaseRequisition.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		PurchaseRequisition purchaseRequisitionSaved;
		try {
			purchaseRequisitionSaved = this.prRepository.save(purchaseRequisition);
		} catch (DataIntegrityViolationException e) {
			log.error("Purchase requisition unique constrain violetd." + e.getMostSpecificCause());
			throw new CustomException("Purchase requisition unique constrain violetd :" + e.getMostSpecificCause());
		}
		
		if (purchaseRequisitionSaved == null) {
			log.info("Error while saving the Purchase Requisition.");
			throw new CustomMessageException("Error while saving the Purchase Requisition.");
		}
		log.info("PR saved successfully :: " + purchaseRequisitionSaved.getId());
		// update the data in bank history table
		this.updatePurchaseRequisitionHistory(purchaseRequisitionSaved, oldPurchaseRequisition);
		log.info("PR history is updated");
		
		// TODO
		// if old and new approver is not same then we can say approver is changed
		if (purchaseRequisitionSaved.getNextApprover() != null && !purchaseRequisitionSaved.getNextApprover().equals(oldPurchaseRequisition.get().getNextApprover())) {
			// send mail to new approver
		}
		
		if (CollectionUtils.isNotEmpty(purchaseRequisition.getPrItems())) {
			for (PrItem prItem : purchaseRequisition.getPrItems()) {
				prItem.setPrId(purchaseRequisitionSaved.getId());
				prItem.setPrNumber(purchaseRequisitionSaved.getPrNumber());
				this.save(prItem);
			}
			purchaseRequisitionSaved.setPrItems(purchaseRequisition.getPrItems());
		}
		
		return purchaseRequisitionSaved;
	}

	/**
	 * This method save the data in history table
	 * Add entry as a Insert if PR is new 
	 * Add entry as a Update if PR is exists
	 * 
	 * @param purchaseRequisition
	 * @param oldPurchaseRequisition
	 */
	private void updatePurchaseRequisitionHistory(PurchaseRequisition purchaseRequisition, Optional<PurchaseRequisition> oldPurchaseRequisition) {
		if (oldPurchaseRequisition.isPresent()) {
			// insert the updated fields in history table
			List<PurchaseRequisitionHistory> prHistories = new ArrayList<PurchaseRequisitionHistory>();
			try {
				prHistories = oldPurchaseRequisition.get().compareFields(purchaseRequisition);
				if (CollectionUtils.isNotEmpty(prHistories)) {
					this.prHistoryRepository.saveAll(prHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("Purchase Requisition History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT 
			this.prHistoryRepository.save(this.preparePurchaseRequisitionHistory(purchaseRequisition.getPrNumber(), null, AppConstants.PR, Operation.CREATE.toString(), purchaseRequisition.getLastModifiedBy(), null, String.valueOf(purchaseRequisition.getId())));
		}
	}

	/**
	 * Prepares the history for the Item
	 * @param prNumber
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	@Override
	public PurchaseRequisitionHistory preparePurchaseRequisitionHistory(String prNumber, Long childId, String moduleName, String operation, String lastModifiedBy, String oldValue, String newValue) {
		PurchaseRequisitionHistory purchaseRequisitionHistory = new PurchaseRequisitionHistory();
		purchaseRequisitionHistory.setPrNumber(prNumber);
		purchaseRequisitionHistory.setChildId(childId);
		purchaseRequisitionHistory.setModuleName(moduleName);
		purchaseRequisitionHistory.setChangeType(AppConstants.UI);
		purchaseRequisitionHistory.setOperation(operation);
		purchaseRequisitionHistory.setOldValue(oldValue);
		purchaseRequisitionHistory.setNewValue(newValue);
		purchaseRequisitionHistory.setLastModifiedBy(lastModifiedBy);
		return purchaseRequisitionHistory;
	}

	
	@Override
	public PurchaseRequisition findById(Long id) {
		Optional<PurchaseRequisition> purchaseRequisition = Optional.empty();
		purchaseRequisition = this.findPrById(id);
		purchaseRequisition.get().setPrItems(this.getPrItemsMappingByPrId(purchaseRequisition.get().getId()));
		
//		boolean isRfqEnabled = true;
		/**
		 * 27-07-2022
		 * Requirement :: RFQ Button should be disabled if QA is created
		 */
		// get all RFQ based on PR number
//		List<Quotation> quotations = this.quotationRepository.findByPrNumberAndIsDeleted(purchaseRequisition.get().getPrNumber(), false);
//		if (CollectionUtils.isNotEmpty(quotations)) {
//			for (Quotation quotation : quotations) {
//				// get QA based on all the RFQ's
//				Optional<QuotationAnalysis> quotationAnalysis = this.quotationAnalysisRepository.findByRfqNumber(quotation.getRfqNumber());
//				// If QA is there then we restrict user to create the RFQ
//				if (quotationAnalysis.isPresent()) {
//					isRfqEnabled = false;
//				}
//			}
//		}
//		
//		purchaseRequisition.get().setRfqEnabled(isRfqEnabled);
		
		return purchaseRequisition.get();
	}

	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<PurchaseRequisition> purchaseRequisitions = new ArrayList<PurchaseRequisition>();
		
		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest).toString();
		
		// get list
		purchaseRequisitions = this.purchaseRequisitionDao.findAll(whereClause, paginationRequest);
		
		// getting count
		Long totalRecords = this.purchaseRequisitionDao.getCount(whereClause);
		
		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(), purchaseRequisitions, totalRecords);
	}

	private StringBuilder prepareWhereClause(PaginationRequest paginationRequest) { 
		Long subsidiaryId = null;
		String requestor = null;
		String prDate = null;
		String department = null;
		
		Map<String, ?> filters = paginationRequest.getFilters();
		
		if (filters.containsKey(FilterNames.SUBSIDIARY_ID))
			subsidiaryId = ((Number) filters.get(FilterNames.SUBSIDIARY_ID)).longValue();
		if (filters.containsKey(FilterNames.REQUESTOR))
			requestor = (String) filters.get(FilterNames.REQUESTOR);
		if (filters.containsKey(FilterNames.DEPARTMENT))
			department = (String) filters.get(FilterNames.DEPARTMENT);
		if (filters.containsKey(FilterNames.PR_DATE)) 
			prDate = (String) filters.get(FilterNames.PR_DATE);
		
		StringBuilder whereClause = new StringBuilder(" AND p.isDeleted is false ");
		if (subsidiaryId != null && subsidiaryId != 0) {
			whereClause.append(" AND p.subsidiaryId = ").append(subsidiaryId);
		}
		if (StringUtils.isNotEmpty(requestor)) {
			whereClause.append(" AND lower(p.requestor) like lower ('%").append(requestor).append("%')");
		}
		if (StringUtils.isNotEmpty(department)) {
			whereClause.append(" AND lower(p.department) like lower('%").append(department).append("%')");
		}
		if (prDate != null) {
			whereClause.append(" AND to_char(p.prDate, 'yyyy-MM-dd') like '%").append(prDate).append("%'");
		}
		return whereClause;
	}

	@Override
	public boolean deleteById(Long id) {
		PurchaseRequisition purchaseRequisition = new PurchaseRequisition();
		purchaseRequisition = this.findById(id);
		purchaseRequisition.setDeleted(true);
		
		purchaseRequisition = this.prRepository.save(purchaseRequisition);
		
		if (purchaseRequisition == null) {
			log.error("Error while deleting the Purchase Requisition : " + id);
			throw new CustomMessageException("Error while deleting the Purchase Requisition : " + id);
		}
	
		// update the operation in the history
		this.prHistoryRepository.save(this.preparePurchaseRequisitionHistory(purchaseRequisition.getPrNumber(), null, AppConstants.PR, Operation.DELETE.toString(), purchaseRequisition.getLastModifiedBy(), String.valueOf(purchaseRequisition.getId()), null));
		
		return true;
	}

	@Override
	public List<PurchaseRequisitionHistory> findHistoryById(String prNumber, Pageable pageable) {
		List<PurchaseRequisitionHistory> histories = this.prHistoryRepository.findByPrNumberOrderById(prNumber, pageable);
		String createdBy = histories.get(0).getLastModifiedBy();
		histories.forEach(e->{
			e.setCreatedBy(createdBy);
		});
		return histories;
	}

	/**
	 * As per requirement, Item will be add or remove only
	 * No update operation will be there
	 * Date : 11-07-2022
	 */
	@Override
	public PrItem save(PrItem prItem) {
		prItem.setCreatedBy(CommonUtils.getLoggedInUsername());
		prItem.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		prItem.setRemainedQuantity(prItem.getQuantity());
		prItem = prItemRepository.save(prItem);

		// insert mapping history in the history table
		this.prHistoryRepository.save(this.preparePurchaseRequisitionHistory(prItem.getPrNumber(), prItem.getId(), AppConstants.PR_ITEM, Operation.CREATE.toString(), prItem.getLastModifiedBy(), null, String.valueOf(prItem.getId())));
		
		return prItem;
	}

	@Override
	public boolean deletePrItemMapping(Long id) {
		Optional<PrItem> prItem = Optional.ofNullable(null);
		prItem = this.prItemRepository.findByIdAndIsDeleted(id, false);
		if (!prItem.isPresent()) {
			log.error("PR-Item Mapping is not found against the provided ID : " + id);
			throw new CustomMessageException("PR-Item Mapping is not found against the provided ID : " + id);
		}
		
		prItem.get().setDeleted(true);
		prItem = Optional.ofNullable(this.prItemRepository.save(prItem.get()));
		
		if(!prItem.isPresent()) {
			log.error("Not able to delete the PR-Item Mapping against the provided ID : " + id);
			throw new CustomMessageException("Not able to delete the PR-Item Mapping against the provided ID : " + id);
		}
		
		this.prHistoryRepository.save(this.preparePurchaseRequisitionHistory(prItem.get().getPrNumber(), prItem.get().getId(), AppConstants.PR_ITEM, Operation.DELETE.toString(), prItem.get().getLastModifiedBy(), String.valueOf(prItem.get().getId()), null));
		
		return true;
	}

	/**
	 * Returns the List of PR-Item mapping based on PR-Id.
	 * @param prId
	 * @return
	 */
	public List<PrItem> getPrItemsMappingByPrId(Long prId) {
		if (prId == null || prId == 0) {
			log.error("PR id is Not valid.");
			throw new CustomMessageException("PR id is Not valid.");
		}
		
		List<PrItem> prItems = new ArrayList<PrItem>();
		prItems = this.prItemRepository.findByPrId(prId);
		return prItems;
	}

//	@Override
//	public PurchaseRequisition findByPrNumber(String prNumber) {
//		Optional<PurchaseRequisition> purchaseRequisition = Optional.ofNullable(null);
//		purchaseRequisition = this.prRepository.findByPrNumberAndIsDeleted(prNumber, false);
//		if (!purchaseRequisition.isPresent()) {
//			log.info("Purchase Requisition is not exist for prNumber - " + prNumber);
//			throw new CustomMessageException("Purchase Requisition is not exist for prNumber - " + prNumber);
//		}
//		purchaseRequisition.get().setPrItems(this.getPrItemsMappingByPrId(purchaseRequisition.get().getId()));
//		return purchaseRequisition.get();
//	}

	@Override
	public List<IdNameResponse> findDistinctPrNumbers() {
		List<IdNameResponse> prNumbers = new ArrayList<IdNameResponse>();
		prNumbers = this.prRepository.findDistinctPrNumbers(false);
		return prNumbers;
	}

	@Override
	public List<PrItem> findUnprocessedItemsByPrId(Long prId, String formName) {
		List<PrItem> items = new ArrayList<PrItem>();
		items = this.purchaseRequisitionDao.findUnprocessedItemsByPrId(prId, formName);
		return items;
	}

	@Override
	public List<IdNameResponse> findPendingPrForPo(Long subsidiaryId, Long locationId) {
//		List<String> status = new ArrayList<String>();
//		status.add(TransactionStatus.APPROVED.getTransactionStatus());
//		status.add(TransactionStatus.PARTIALLY_PROCESSED.getTransactionStatus());
//		Map<Long, String> prCurrencyMap = prRepository.findPrIdAndNumberMap(subsidiaryId, locationId, false, status, FormNames.PO.getFormName());
		List<IdNameResponse> idNumbers = this.prRepository.findPendingPrForPo(subsidiaryId, locationId, false);
//		for (Map.Entry<Long, String> entry : prCurrencyMap.entrySet()) {
//			Long prId = entry.getKey();
////			Long count = this.prItemRepository.isRfqCreatedForPrId(prId);
////			if (count == null || count == 0) {
//				idNumbers.add(new IdNameResponse(prId, entry.getValue()));
////			}
//		}
		return idNumbers;
	}
	
	@Override
	public List<PurchaseRequisition> getPrApproval(String userId) {
		List<String> status = new ArrayList<String>();
		status.add(TransactionStatus.PENDING_APPROVAL.getTransactionStatus());
		status.add(TransactionStatus.PARTIALLY_APPROVED.getTransactionStatus());
		List<PurchaseRequisition> purchaseRequisitions = new ArrayList<PurchaseRequisition>();
		purchaseRequisitions = this.prRepository.findAllByPrStatus(status, userId);
		log.info("purchase Requisitions are for approval process " + purchaseRequisitions);
		for (PurchaseRequisition purchaseRequisition : purchaseRequisitions) {
			Double totalAmount = 0.0;
			totalAmount = this.prItemRepository.findEstimatedAmountForPr(purchaseRequisition.getId());
			purchaseRequisition.setTotalValue(totalAmount);
			log.info("Total amount for PR " + totalAmount);
		}
		return purchaseRequisitions;
	}
	
	@Override
	public PaginationResponse findAllApprovedPr(PaginationRequest paginationRequest, Long subsidiaryId) {
		List<PurchaseRequisition> purchaseRequisitions = new ArrayList<PurchaseRequisition>();

		// preparing where clause
		String whereClause = this.prepareWhereClauses(paginationRequest).toString();

		// get list
		purchaseRequisitions = this.purchaseRequisitionDao.findAllApprovedPr(whereClause, paginationRequest, subsidiaryId);

		// getting count
		Long totalRecords = this.purchaseRequisitionDao.getCountPrApproved(whereClause, subsidiaryId);
		
	/*	for (PurchaseRequisition purchaseRequisition : purchaseRequisitions) {
			Long rfqCount = this.prItemRepository.isRfqCreatedForPrId(purchaseRequisition.getId());
			if (rfqCount != null && rfqCount != 0) {
				purchaseRequisition.setPartiallyProcessedFor(FormNames.RFQ.getFormName());
			} else {
				Long poCount = this.prItemRepository.isPoCreatedForPrId(purchaseRequisition.getId());
				if (poCount != null && poCount != 0) {
					purchaseRequisition.setPartiallyProcessedFor(FormNames.PO.getFormName());
				}
			}
		}
*/
		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(),
				purchaseRequisitions, totalRecords);

	}

	private StringBuilder prepareWhereClauses(PaginationRequest paginationRequest) {
		Long subsidiaryId = null;
		String fromDate = null;
		String toDate = null;
		String prStatus = null;

		Map<String, ?> filters = paginationRequest.getFilters();

		if (filters.containsKey(FilterNames.SUBSIDIARY_ID))
			subsidiaryId = ((Number) filters.get(FilterNames.SUBSIDIARY_ID)).longValue();
		if (filters.containsKey(FilterNames.FROM_DATE))
			fromDate = (String) filters.get(FilterNames.FROM_DATE);
		if (filters.containsKey(FilterNames.TO_DATE))
			toDate = (String) filters.get(FilterNames.TO_DATE);
		if (filters.containsKey(FilterNames.STATUS))
			prStatus = (String) filters.get(FilterNames.STATUS);

		StringBuilder whereClause = new StringBuilder(" AND pr.isDeleted is false ");
		if (subsidiaryId != null && subsidiaryId != 0) {
			whereClause.append(" AND pr.subsidiaryId = ").append(subsidiaryId);
		}
		if (StringUtils.isNotEmpty(fromDate)) {
			whereClause.append(" AND to_char(pr.prDate, 'yyyy-MM-dd') >= '").append(fromDate).append("' ");
		}
		if (StringUtils.isNotEmpty(toDate)) {
			whereClause.append(" AND to_char(pr.prDate, 'yyyy-MM-dd') <= '").append(toDate).append("' ");
		}
		if (StringUtils.isNotEmpty(prStatus)) {
			whereClause.append(" AND pr.prStatus like '%").append(prStatus).append("%' ");
		}
		return whereClause;
	}

	/**
	 * This is first step of approval hence steps are -
	 * 1. Find the max level up to which we have to validate the approver & sequence id and which is the common throughout the conditions
	 * 2. Find the approver using sequence id for that specific record
	 * 3. Update the data in base table of above 2 steps
	 */
	@Override
	public Boolean sendForApproval(Long id) {
		Boolean isSentForApproval = false;

		try {
			/**
			 * Due to single transaction we are getting updated value when we find from repo after the update
			 * hence finding old one first
			 */
			// Get the existing object using the deep copy
			Optional<PurchaseRequisition> oldPurchaseRequisition = this.findOldDeepCopiedPR(id);

			Optional<PurchaseRequisition> purchaseRequisition = Optional.empty();
			purchaseRequisition = this.findPrById(id);

			/**
			 * Check routing is active or not
			 */
			boolean isRoutingActive = purchaseRequisition.get().isApprovalRoutingActive();
			if (!isRoutingActive) {
				log.error("Routing is not active for the PR : " + id + ". Please update your configuration. ");
				throw new CustomMessageException("Routing is not active for the PR : " + id + ". Please update your configuration. ");
			}
			
			Double transactionalAmount = this.prRepository.findTotalEstimatedAmountForPr(id);
			
			// if amount is null then throw error
			if (transactionalAmount == null || transactionalAmount == 0.0) {
				log.error("There is no available Approval Process for this transaction.");
				throw new CustomMessageException("There is no available Approval Process for this transaction.");
			}

			ApprovalRequest approvalRequest = new ApprovalRequest();
			approvalRequest.setSubsidiaryId(purchaseRequisition.get().getSubsidiaryId());
			approvalRequest.setFormName(FormNames.PR.getFormName());
			approvalRequest.setTransactionAmount(transactionalAmount);
			approvalRequest.setLocationId(purchaseRequisition.get().getLocationId());
			approvalRequest.setDepartment(purchaseRequisition.get().getDepartment());
			log.info("Approval object us prepared : " + approvalRequest.toString());

			/**
			 * method will give max level & it's sequence if match otherwise throw error message as no approver process exist
			 * if level or sequence id is null then also throws error message.
			 */
			ApprovalPreference approvalPreference = this.masterServiceClient.findApproverMaxLevel(approvalRequest);
			Long sequenceId = approvalPreference.getSequenceId();
			String level = approvalPreference.getLevel();
			Long approverPreferenceId = approvalPreference.getId();
			log.info("Max level & sequence is found :: " + approvalPreference.toString());
			
			purchaseRequisition.get().setApproverSequenceId(sequenceId);
			purchaseRequisition.get().setApproverMaxLevel(level);
			purchaseRequisition.get().setApproverPreferenceId(approverPreferenceId);
			
			String levelToFindRole = "L1";
			if (AppConstants.APPROVAL_TYPE_INDIVIDUAL.equals(approvalPreference.getApprovalType())) {
				levelToFindRole = level;
			}
			approvalRequest = this.masterServiceClient.findApproverByLevelAndSequence(approverPreferenceId, levelToFindRole, sequenceId);
			
			this.updateApproverDetailsInPr(purchaseRequisition, approvalRequest);
			purchaseRequisition.get().setPrStatus(TransactionStatus.PENDING_APPROVAL.getTransactionStatus());
			log.info("Approver is found and details is updated :: " + purchaseRequisition.get());
			
			this.savePrForApproval(purchaseRequisition.get(), oldPurchaseRequisition);
			
			String approverIdStr = purchaseRequisition.get().getNextApprover();
			masterServiceClient.sendEmailByApproverId(approverIdStr, FormNames.PR.getFormName());
			
			log.info("PR is saved successfully with Approver details.");
			// TODO send email (Optional)
			isSentForApproval = true;
		} catch (Exception e) {
			log.error("Error while sending PR for approval for id - " + id);
			e.printStackTrace();
			throw new CustomMessageException("Error while sending PR for approval for id - " + id + ", Message :: " + e.getLocalizedMessage());
		}
		
		return isSentForApproval;
	}

	private Optional<PurchaseRequisition> findOldDeepCopiedPR(Long id) {
		Optional<PurchaseRequisition> oldPurchaseRequisition = this.prRepository.findByIdAndIsDeleted(id, false);
		if (oldPurchaseRequisition.isPresent()) {
			try {
				oldPurchaseRequisition = Optional.ofNullable((PurchaseRequisition) oldPurchaseRequisition.get().clone());
				log.info("Existing PR is copied.");
			} catch (CloneNotSupportedException e) {
				log.error("Error while Cloning the object. Please contact administrator.");
				throw new CustomException("Error while Cloning the object. Please contact administrator.");
			}
		}
		return oldPurchaseRequisition;
	}

	/**
	 * Save PR after the approval details change
	 * @param purchaseRequisition
	 */
	private void savePrForApproval(PurchaseRequisition purchaseRequisition, Optional<PurchaseRequisition> oldPurchaseRequisition) {
		purchaseRequisition.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		purchaseRequisition = this.prRepository.save(purchaseRequisition);
		
		if (purchaseRequisition == null) {
			log.info("Error while saving the Purchase Requisition after the Approval.");
			throw new CustomMessageException("Error while saving the Purchase Requisition after the Approval.");
		}
		log.info("PR saved successfully :: " + purchaseRequisition.getId());
		
		// update the data in PR history table
		this.updatePurchaseRequisitionHistory(purchaseRequisition, oldPurchaseRequisition);
		log.info("PR history is updated");		
	}

	/**
	 * This method only return the match PR against the ID.. Otherwise throws the Exception
	 * @param id
	 * @return
	 */
	private Optional<PurchaseRequisition> findPrById(Long id) {
		Optional<PurchaseRequisition> purchaseRequisition = Optional.empty();
		purchaseRequisition = this.prRepository.findByIdAndIsDeleted(id, false);
		if (!purchaseRequisition.isPresent()) {
			log.info("Purchase Requisition is not exist for id - " + id);
			throw new CustomMessageException("Purchase Requisition is not exist for id - " + id);
		}
		boolean isRoutingActive = this.findIsApprovalRoutingActive(purchaseRequisition.get().getSubsidiaryId());
//		if (isRoutingActive) {
//			String status = purchaseRequisition.get().getPrStatus();
//			if (!TransactionStatus.OPEN.getTransactionStatus().equalsIgnoreCase(status) && !TransactionStatus.REJECTED.getTransactionStatus().equalsIgnoreCase(status)) {
//				isRoutingActive = false;
//			}
//		}
		purchaseRequisition.get().setApprovalRoutingActive(isRoutingActive);
		return purchaseRequisition;
	}

	/**
	 * Approve the selected PR's
	 */
	@Override
	public Boolean approveAllPrs(List<Long> prIds) {
		Boolean isAllPrApproved = false;
		try {
			for (Long prId : prIds) {
				log.info("Approval Process is started for pr-id :: " + prId);

				/**
				 * Due to single transaction we are getting updated value when we find from repo after the update
				 * hence finding old one first
				 */
				// Get the existing object using the deep copy
				Optional<PurchaseRequisition> oldPurchaseRequisition = this.findOldDeepCopiedPR(prId);

				Optional<PurchaseRequisition> purchaseRequisition = Optional.empty();
				purchaseRequisition = this.findPrById(prId);

				/**
				 * Check routing is active or not
				 */
				boolean isRoutingActive = purchaseRequisition.get().isApprovalRoutingActive();
				if (!isRoutingActive) {
					log.error("Routing is not active for the PR : " + prId + ". Please update your configuration. ");
					throw new CustomMessageException("Routing is not active for the PR : " + prId + ". Please update your configuration. ");
				}
				
				// meta data
				Long approvalPreferenceId = purchaseRequisition.get().getApproverPreferenceId();
				Long sequenceId = purchaseRequisition.get().getApproverSequenceId();
				String maxLevel = purchaseRequisition.get().getApproverMaxLevel();
				
				ApprovalRequest approvalRequest = new ApprovalRequest();
				
				if (!maxLevel.equals(purchaseRequisition.get().getNextApproverLevel())) {
					Long currentLevelNumber = Long.parseLong(purchaseRequisition.get().getNextApproverLevel().replaceFirst("L", "")) + 1;
					String currentLevel = "L" + currentLevelNumber;
					approvalRequest = this.masterServiceClient.findApproverByLevelAndSequence(approvalPreferenceId, currentLevel, sequenceId);
					purchaseRequisition.get().setPrStatus(TransactionStatus.PARTIALLY_APPROVED.getTransactionStatus());
				} else {
					purchaseRequisition.get().setPrStatus(TransactionStatus.APPROVED.getTransactionStatus());
				}
				log.info("Approval Request is found :: " + approvalRequest.toString());

				this.updateApproverDetailsInPr(purchaseRequisition, approvalRequest);
				log.info("Approver is found and details is updated :: " + purchaseRequisition.get());
				
				this.savePrForApproval(purchaseRequisition.get(), oldPurchaseRequisition);
				log.info("PR is saved successfully with Approver details.");

				masterServiceClient.sendEmailByApproverId(purchaseRequisition.get().getNextApprover(), FormNames.PR.getFormName());
				
				log.info("Approval Process is Finished for pr-id :: " + prId);
			}
			
			isAllPrApproved = true;
		} catch (Exception e) {
			log.error("Error while approving the PR.");
			e.printStackTrace();
			throw new CustomMessageException("Error while approving the PR. Message : " + e.getLocalizedMessage());
		}
		return isAllPrApproved;
	}

	/**
	 * Set/Prepares the approver details in the PR object
	 * @param purchaseRequisition
	 * @param approvalRequest
	 */
	private void updateApproverDetailsInPr(Optional<PurchaseRequisition> purchaseRequisition, ApprovalRequest approvalRequest) {
		purchaseRequisition.get().setApprovedBy(purchaseRequisition.get().getNextApprover());
		purchaseRequisition.get().setNextApprover(approvalRequest.getNextApprover());
		purchaseRequisition.get().setNextApproverRole(approvalRequest.getNextApproverRole());
		purchaseRequisition.get().setNextApproverLevel(approvalRequest.getNextApproverLevel());
	}

	@Override
	public Boolean rejectAllPrs(List<PurchaseRequisition> prs) {
		Boolean isAllPrRejected = false;
		try {
			for (PurchaseRequisition pr : prs) {
				log.info("Reject Process is started for pr-id :: " + pr);
				
				String rejectComments = pr.getRejectedComments();
				if (StringUtils.isEmpty(rejectComments)) {
					log.error("Reject Comments is required.");
					throw new CustomException("Reject Comments is required. It is missing for PR : " + pr.getId());
				}

				/**
				 * Due to single transaction we are getting updated value when we find from repo after the update
				 * hence finding old one first
				 */
				// Get the existing object using the deep copy
				Optional<PurchaseRequisition> oldPurchaseRequisition = this.findOldDeepCopiedPR(pr.getId());

				Optional<PurchaseRequisition> purchaseRequisition = Optional.empty();
				purchaseRequisition = this.findPrById(pr.getId());

				purchaseRequisition.get().setPrStatus(TransactionStatus.REJECTED.getTransactionStatus());
				purchaseRequisition.get().setRejectedComments(rejectComments);
				purchaseRequisition.get().setApprovedBy(null);
				purchaseRequisition.get().setNextApprover(null);
				purchaseRequisition.get().setNextApproverRole(null);
				purchaseRequisition.get().setNextApproverLevel(null);
				purchaseRequisition.get().setApproverSequenceId(null);
				purchaseRequisition.get().setApproverMaxLevel(null);
				purchaseRequisition.get().setApproverPreferenceId(null);
				
				log.info("Approval Fields are restored to empty. For PR : " + pr);
				
				this.savePrForApproval(purchaseRequisition.get(), oldPurchaseRequisition);
				log.info("PR is saved successfully with restored Approver details.");

				// TODO send mail to user
				log.info("Approval Process is Finished for pr-id :: " + pr);
			}
			
			isAllPrRejected = true;
		} catch (Exception e) {
			log.error("Error while Rejecting the PR.");
			e.printStackTrace();
			throw new CustomMessageException("Error while Rejecting the PR. Message : " + e.getLocalizedMessage());
		}
		return isAllPrRejected;
	}
	
	private boolean findIsApprovalRoutingActive(Long subsidiaryId) {
		return this.masterServiceClient.findIsApprovalRoutingActive(subsidiaryId, FormNames.PR.getFormName());
	}

	@Override
	public List<PrItem> findUnprocessedItemsByPrIds(List<Long> prIds, String formName) {
		List<PrItem> items = new ArrayList<PrItem>();
		for (Long prId : prIds) {
			items.addAll(this.findUnprocessedItemsByPrId(prId, formName));
		}
		return items;
	}

	@Override
	public List<IdNameResponse> findApprovedPrsBySubsidiary(Long subsidiaryId) {
		List<IdNameResponse> resultList = new ArrayList<IdNameResponse>();
		List<IdNameResponse> prs = this.prRepository.findDistinctApprovedPrNumbersBySubsidiary(subsidiaryId, false);
		for (IdNameResponse pr : prs) {
//			if ("TM/22-23/11163/PR".equalsIgnoreCase(pr.getName())) {
//				System.out.println("========");
//			}
//			Long count = this.prItemRepository.isPoCreatedForPrId(pr.getId());
//			if (count == null || count == 0) {
				
				resultList.add(pr);
//			}
		}
		 return resultList;
	}
	
	@Override
	public Boolean updateNextApprover(Long approverId, Long prId) {
		Optional<PurchaseRequisition> purchaseRequisition = this.prRepository.findByIdAndIsDeleted(prId, false);
		
		if (!purchaseRequisition.isPresent()) {
			log.error("PR Not Found against given Supplier id : " + prId);
			throw new CustomMessageException("PR Not Found against given PR id : " + prId);
		}
		purchaseRequisition.get().setNextApprover(String.valueOf(approverId));
		purchaseRequisition.get().setLastModifiedBy(CommonUtils.getLoggedInUsername());
		this.prRepository.save(purchaseRequisition.get());
		
		return true;
	}
	
	@Override
	public Boolean selfApprove(Long supplierId) {
		Optional<PurchaseRequisition> purchaseRequisition = this.prRepository.findByIdAndIsDeleted(supplierId, false);
		
		if (!purchaseRequisition.isPresent()) {
			log.error("PR Not Found against given PR id : " + supplierId);
			throw new CustomMessageException("PR Not Found against given PR id : " + supplierId);
		}
		purchaseRequisition.get().setPrStatus(TransactionStatus.APPROVED.getTransactionStatus());
		purchaseRequisition.get().setLastModifiedBy(CommonUtils.getLoggedInUsername());
		
		if (this.prRepository.save(purchaseRequisition.get()) != null) return true;
		else throw new CustomException("Error in self approve. Please contact System Administrator");
	}
	
	@Override
	public byte[] upload(MultipartFile file) {
		try {
			return this.importPrsFromExcel(file);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("Something went wrong. Please Contact Administrator. Error : " + e.getLocalizedMessage());
		}
	}

	public byte[] importPrsFromExcel(MultipartFile inputFile) {
		try {
			InputStream inputStream = inputFile.getInputStream();
			@SuppressWarnings("resource")
			Workbook workbook = new XSSFWorkbook(inputStream);
			Sheet sheet = workbook.getSheet("PR");
			Iterator<Row> rows = sheet.iterator();

			int statusColumnNumber = 0;
			int rowNumber = 0;

			boolean isError = false;
			StringBuilder errorMessage = new StringBuilder();

			Map<String, PurchaseRequisition> prMapping = new TreeMap<String, PurchaseRequisition>();

			while (rows.hasNext()) {
				isError = false;
				errorMessage = new StringBuilder();
				int errorCount = 1;
				Row inputCurrentRow = rows.next();
				if (rowNumber == 0) {
					statusColumnNumber = inputCurrentRow.getLastCellNum();
					Cell cell = inputCurrentRow.createCell(statusColumnNumber);
					cell.setCellValue("Imported Status");
					rowNumber++;
					continue;
				}

				boolean isRowEmpty = ExcelHelper.checkIfRowIsEmpty(inputCurrentRow);

				// if row is empty it means all records completed.
				if (isRowEmpty)
					break;

				PurchaseRequisition purchaseRequisition = new PurchaseRequisition();
				String externalId = null;
				// External ID - REQUIRED
				try {
					if (inputCurrentRow.getCell(0) != null) {
						externalId = new DataFormatter().formatCellValue(inputCurrentRow.getCell(0));
					} else {
						log.error("External ID should not be empty.");
						continue;
					}
				} catch (Exception e) {
					log.error("Exception External ID " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of External ID is invalid.");
					isError = true;
					errorCount++;
					throw new CustomException("External ID should not be empty.");
				}
				// ----------------- PR header fields STARTED -----------------------
				if (prMapping.containsKey(externalId)) {
					purchaseRequisition = prMapping.get(externalId);
				}
				purchaseRequisition.setExternalId(externalId);
				
				// Subsidiary - REQUIRED
				try {
					if (inputCurrentRow.getCell(1) != null) {
						String subsidiaryName = inputCurrentRow.getCell(1).getStringCellValue();
						Long subsidiaryId = this.setupServiceClient.getSubsidiaryIdByName(subsidiaryName);
						if (subsidiaryId == null) {
							errorMessage.append(errorCount + ") Subsidiary : " + subsidiaryName + " is not found Please enter the valid Subsidiary Name. ");
							log.error("Subsidiary : " + subsidiaryName + " is not found. Please enter the valid Subsidiary Name. ");
							isError = true;
							errorCount++;
						} else {
							purchaseRequisition.setSubsidiaryId(subsidiaryId);
							purchaseRequisition.setCurrency(this.setupServiceClient.findCurrencyBySubsidiaryName(subsidiaryName));
						}
					} else {
						errorMessage.append(errorCount + ") Subsidiary is required. ");
						log.error("Subsidiary is required. Please enter the valid Subsidiary Name. ");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception subsidiary " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Subsidiary Name is invalid.");
					isError = true;
					errorCount++;
				}
				
				// PR Date - REQUIRED
				try {
					if (inputCurrentRow.getCell(2) != null) {
						purchaseRequisition.setPrDate(inputCurrentRow.getCell(2).getDateCellValue());
					} else {
						errorMessage.append(errorCount + ") PR Date is required. ");
						log.error("PR Date is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception PR Date " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of PR Date is invalid.");
					isError = true;
					errorCount++;
				}
				
				// PR Type - REQUIRED
				String prType = "";
				try {
					if (inputCurrentRow.getCell(3) != null) {
						prType = inputCurrentRow.getCell(3).getStringCellValue();
						if (!PR_TYPES.contains(prType)) {
							errorMessage.append(errorCount + ") PR Type is invalid. ");
							log.error("PR Type is invalid.");
							isError = true;
							errorCount++;
						} else purchaseRequisition.setType(prType);
					} else {
						errorMessage.append(errorCount + ") PR Type is required. ");
						log.error("PR Type is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception PR Type " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of PR Type is invalid.");
					isError = true;
					errorCount++;
				}

				// Project Name
				try {
					if (inputCurrentRow.getCell(4) != null) {
						String projectName = inputCurrentRow.getCell(4).getStringCellValue();
						Boolean isNameValid = this.masterServiceClient.getValidateProjectName(projectName);
						if (isNameValid) {
							errorMessage.append(errorCount + ") Project Name is Not exist. ");
							log.error("Project Name is Not exist.");
							isError = true;
							errorCount++;
						} else purchaseRequisition.setProjectName(projectName);
						
					} else if ("Project".equalsIgnoreCase(prType)) {
						errorMessage.append(errorCount + ") Project Name is required. ");
						log.error("Project Name is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Project Name " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Project Name is invalid.");
					isError = true;
					errorCount++;
				}

				// Department - REQUIRED
				try {
					if (inputCurrentRow.getCell(5) != null) {
						purchaseRequisition.setDepartment(inputCurrentRow.getCell(5).getStringCellValue());
					} else {
						errorMessage.append(errorCount + ") Department is required.");
						log.error("Department is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Vendor Type " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Vendor Type is invalid.");
					isError = true;
					errorCount++;
				}

				// Location
				try {
					if (inputCurrentRow.getCell(6) != null) {
						String locationName = inputCurrentRow.getCell(6).getStringCellValue();
						List<Location> locations = this.masterServiceClient.getLocationsByLocationName(locationName);

						Long locationId = null;
						if (CollectionUtils.isNotEmpty(locations)) locationId = locations.get(0).getId(); 
						else {
							errorMessage.append(errorCount + ") Location is not valid.");
							log.error("Location is not valid.");
							isError = true;
							errorCount++;
						}
						purchaseRequisition.setLocationId(locationId);
					} else {
						errorMessage.append(errorCount + ") Location is required.");
						log.error("Location is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Nature of Supply " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Nature of Supply is invalid.");
					isError = true;
					errorCount++;
				}

				// Priority
				try {
					if (inputCurrentRow.getCell(7) != null) {
						purchaseRequisition.setPriority(inputCurrentRow.getCell(7).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Priority " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Priority is invalid.");
					isError = true;
					errorCount++;
				}

				// Requestor
				try {
					if (inputCurrentRow.getCell(8) != null) {
						purchaseRequisition.setRequestor(inputCurrentRow.getCell(8).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Requestor " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Requestor is invalid.");
					isError = true;
					errorCount++;
				}

				// Memo
				try {
					if (inputCurrentRow.getCell(9) != null) {
						purchaseRequisition.setMemo(inputCurrentRow.getCell(9).getStringCellValue());
					}
				} catch (Exception e) {
					log.error("Exception Memo " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Memo is invalid.");
					isError = true;
					errorCount++;
				}
				// ----------------- PR header fields completed --------------------------

				// -------------- PR Item IS STARTED -------------------------------
				PrItem prItem = new PrItem();
				// Item code - REQUIRED
				try {
					if (inputCurrentRow.getCell(10) != null) {
						String itemName = inputCurrentRow.getCell(10).getStringCellValue();
						prItem.setItemName(itemName);
						
						Item item = this.masterServiceClient.findByName(itemName);
						if (item != null) {
							prItem.setItemId(item.getId());
						} else {
							errorMessage.append(errorCount + ") Item Code is not exist.");
							log.error("Item Code is not exist");
							isError = true;
							errorCount++;
						}
					} else {
						errorMessage.append(errorCount + ") Item Code is required.");
						log.error("Item Code is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Item Code " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Item Code is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Quantity
				try {
					if (inputCurrentRow.getCell(11) != null) {
						Double quantity = inputCurrentRow.getCell(11).getNumericCellValue();
						prItem.setQuantity(quantity);
					} else {
						errorMessage.append(errorCount + ") Item quantity is required.");
						log.error("Item quantity is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception quantity " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of quantity is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Rate
				try {
					if (inputCurrentRow.getCell(12) != null) {
						double rate = inputCurrentRow.getCell(12).getNumericCellValue();
						prItem.setRate(rate);
					}
				} catch (Exception e) {
					log.error("Exception rate " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of rate is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Received Date
				try {
					if (inputCurrentRow.getCell(13) != null) {
						prItem.setReceivedDate(inputCurrentRow.getCell(13).getDateCellValue());
					} else {
						errorMessage.append(errorCount + ") Received Date is required. ");
						log.error("Received Date is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception PR Date " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of PR Date is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Memo
				try {
					if (inputCurrentRow.getCell(14) != null) {
						String memo = inputCurrentRow.getCell(14).getStringCellValue();
						prItem.setMemo(memo);
					}
				} catch (Exception e) {
					log.error("Exception Memo " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Memo is invalid.");
					isError = true;
					errorCount++;
				}
				
				List<PrItem> prItems = new ArrayList<PrItem>();
				prItems = purchaseRequisition.getPrItems();
				if (CollectionUtils.isEmpty(prItems))
					prItems = new ArrayList<PrItem>();
				
				prItems.add(prItem);
				prItems = prItems.stream().distinct().collect(Collectors.toList());
				
				purchaseRequisition.setPrItems(prItems);
				// -------------- PR Items IS FINISHED -------------------------------

				// ADDED IN MAP
				prMapping.put(externalId, purchaseRequisition);
				Cell cell = inputCurrentRow.createCell(statusColumnNumber);
				if (isError) {
					cell.setCellValue(errorMessage.toString());
					purchaseRequisition.setHasError(true);
					continue;
				} else if (purchaseRequisition.isHasError()) {
					cell.setCellValue("Data is not valid for parent or sibling items.");
				} else {
					cell.setCellValue("Imported");
				}
			}
			
			for (Map.Entry<String, PurchaseRequisition> map : prMapping.entrySet()) {
			    log.info(map.getKey() + " ==== >>> " + map.getValue());
			    PurchaseRequisition purchaseRequisition = map.getValue();
			    if (purchaseRequisition != null && !purchaseRequisition.isHasError()) {
					this.save(purchaseRequisition);
					log.info("Purchase Requisition is saved.");
			    }
			}

			FileOutputStream out = null;
			File outputFile = new File("pr_export.xlsx");
			try {
				// Writing the workbook
				out = new FileOutputStream(outputFile);
				workbook.write(out);
				log.info("pr_export.xlsx written successfully on disk.");
			} catch (Exception e) {
				// Display exceptions along with line number
				// using printStackTrace() method
				e.printStackTrace();
				throw new CustomException("Something went wrong. Please Contact Administrator.");
			} finally {
				out.close();
				workbook.close();
			}
			return Files.readAllBytes(outputFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			throw new CustomException("Something went wrong. Please Contact Administrator..");
		}
	}

	@Override
	public byte[] downloadTemplate() {
		DefaultResourceLoader loader = new DefaultResourceLoader();
		try {
			File is = loader.getResource("classpath:/templates/pr_template.xlsx").getFile();
			return Files.readAllBytes(is.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
