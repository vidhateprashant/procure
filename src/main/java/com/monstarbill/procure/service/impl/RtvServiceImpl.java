package com.monstarbill.procure.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

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
import com.monstarbill.procure.dao.RtvDao;
import com.monstarbill.procure.enums.FormNames;
import com.monstarbill.procure.enums.Operation;
import com.monstarbill.procure.enums.TransactionStatus;
import com.monstarbill.procure.feignclient.MasterServiceClient;
import com.monstarbill.procure.feignclient.SetupServiceClient;
import com.monstarbill.procure.models.Grn;
import com.monstarbill.procure.models.GrnItem;
import com.monstarbill.procure.models.Rtv;
import com.monstarbill.procure.models.RtvHistory;
import com.monstarbill.procure.models.RtvItem;
import com.monstarbill.procure.payload.request.ApprovalRequest;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.ApprovalPreference;
import com.monstarbill.procure.payload.response.PaginationResponse;
import com.monstarbill.procure.repository.GrnItemRepository;
import com.monstarbill.procure.repository.GrnRepository;
import com.monstarbill.procure.repository.RtvHistoryRepository;
import com.monstarbill.procure.repository.RtvItemRepository;
import com.monstarbill.procure.repository.RtvRepository;
import com.monstarbill.procure.service.GrnService;
import com.monstarbill.procure.service.RtvService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class RtvServiceImpl implements RtvService {

	@Autowired
	private RtvRepository rtvRepository;

	@Autowired
	private GrnItemRepository grnItemRepository;
	
	@Autowired
	private RtvItemRepository rtvItemRepository;

	@Autowired
	private RtvHistoryRepository rtvHistoryRepository;

	@Autowired
	private RtvDao rtvDao;
	
	@Autowired
	private GrnService grnService;
	
	@Autowired
	private GrnRepository grnRepository;
	
	@Autowired
	private SetupServiceClient setupServiceClient;

	@Autowired
	private MasterServiceClient masterServiceClient;
	
	@Override
	public Rtv save(Rtv rtv) {
		String username = CommonUtils.getLoggedInUsername();
		
		Optional<Rtv> oldRtv = Optional.empty();
		try {
			// 1. save the rtv
			if (rtv.getId() == null) {
				rtv.setCreatedBy(username);
				String transactionalDate = CommonUtils.convertDateToFormattedString(rtv.getRtvDate());
				String documentSequenceNumber = this.setupServiceClient.getDocumentSequenceNumber(transactionalDate, rtv.getSubsidiaryId(), FormNames.RTV.getFormName(), false);
				if (StringUtils.isEmpty(documentSequenceNumber)) {
					throw new CustomMessageException("Please validate your configuration to generate the PO Number");
				}
				 rtv.setRtvNumber(documentSequenceNumber);
			} else {
				// Get the existing object using the deep copy
				oldRtv = this.rtvRepository.findByIdAndIsDeleted(rtv.getId(), false);
				if (oldRtv.isPresent()) {
					try {
						oldRtv = Optional.ofNullable((Rtv) oldRtv.get().clone());
					} catch (CloneNotSupportedException e) {
						log.error("Error while Cloning the object. Please contact administrator.");
						throw new CustomException("Error while Cloning the object. Please contact administrator.");
					}
				}
			}

			rtv.setLastModifiedBy(username);
			Rtv savedRtv;
			try {
				savedRtv = this.rtvRepository.save(rtv);
			}  catch (DataIntegrityViolationException e) {
				log.error("RTV unique constrain violetd." + e.getMostSpecificCause());
				throw new CustomException("RTV unique constrain violetd :" + e.getMostSpecificCause());
			}
			log.info("RTV is saved successfully.");
			
			this.updateRtvHistory(oldRtv, savedRtv);
			log.info("RTV History is saved successfully.");

			// ----------------------------------- 01. rtv Item Started -------------------------------------------------
			log.info("Save rtv Item Started...");
			Set<Long> grnIds = new TreeSet<Long>();
			List<RtvItem> rtvItems = rtv.getRtvItems();
			List<GrnItem> grnItems = new ArrayList<GrnItem>();
			if (CollectionUtils.isNotEmpty(rtvItems)) {
				for (RtvItem rtvItem : rtvItems) {
					this.saveItem(savedRtv, rtvItem);
					grnItems = this.grnItemRepository.findByGrnIdAndItemId(rtvItem.getGrnId(), rtvItem.getItemId());
					if(grnItems.isEmpty()) {
						log.error("grn id and item id is incorrect ." );
						throw new CustomException("grn id and item id is incorrect  ");
					}
					for (GrnItem grnItem : grnItems) {
						grnItem.setRtvQuantity(rtvItem.getAlreadyReturnQuantity() + rtvItem.getReturnQuantity());
						Double remainedQuantity = grnItem.getUnbilledQuantity() - grnItem.getRtvQuantity();
						grnItem.setUnbilledQuantity(remainedQuantity);
						
						if (remainedQuantity < 0) {
							throw new CustomException("Recieved quantity should be less than or equals to remained quantity.");
						}
						if (grnItem.getUnbilledQuantity() == 0) {
							grnItem.setRtvStatus(TransactionStatus.RETURN.getTransactionStatus());
							if (TransactionStatus.PARTIALLY_BILLED.getTransactionStatus().equalsIgnoreCase(grnItem.getBillStatus())) {
								grnItem.setRtvStatus(TransactionStatus.PARTIALLY_RETURN.getTransactionStatus());	
							}
						} else {
							grnItem.setRtvStatus(TransactionStatus.PARTIALLY_RETURN.getTransactionStatus());
						}
					}
					this.grnItemRepository.saveAll(grnItems);
					grnIds.add(rtvItem.getGrnId());
				}
				
				log.info("Grn header level status update started.");
				List<Grn> grns = new ArrayList<Grn>();
				for (Long grnId : grnIds) {
					Grn grn = this.grnService.getByGrnId(grnId);
					Boolean isProcessed = this.grnService.isGrnFullyProcessed(grnId);
					
					String rtvStatus = TransactionStatus.PARTIALLY_RETURN.getTransactionStatus();
					String billStatus = grn.getBillStatus();
					
					if (isProcessed) {
						if (TransactionStatus.BILLED.getTransactionStatus().equalsIgnoreCase(grn.getBillStatus())) {
							rtvStatus = TransactionStatus.PARTIALLY_RETURN.getTransactionStatus();
							billStatus = TransactionStatus.PARTIALLY_BILLED.getTransactionStatus();
						}
					}
					grn.setStatus(rtvStatus);
					grn.setBillStatus(billStatus);
					grn.setRtvStatus(rtvStatus);
					grns.add(grn);
				}
				this.grnRepository.saveAll(grns);
				log.info("Grn header level status update Finished.");
			}
			log.info("Save rtv Item Finished...");
			// ----------------------------------- 01. rtv Item Finished -------------------------------------------------

			System.gc();
			savedRtv = this.getRtvById(savedRtv.getId());
			return savedRtv;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.toString());
		}
	}

	public RtvItem saveItem(Rtv rtv, RtvItem rtvItem) {
		Optional<RtvItem> oldRtvItem = Optional.empty();
		String username = CommonUtils.getLoggedInUsername();
		Double remainedQuantity = 0.0;

		if (rtvItem.getId() == null) {
			rtvItem.setCreatedBy(username);
			List<GrnItem> grnItems = this.grnItemRepository.findByGrnIdAndItemId(rtvItem.getGrnId(), rtvItem.getItemId());
			for (GrnItem grnItem : grnItems) {
				grnItem.setUnbilledQuantity(grnItem.getUnbilledQuantity() - rtvItem.getReturnQuantity());
			}
			this.grnItemRepository.saveAll(grnItems);
			
		} else {
			// Get existing address using deep copy
			oldRtvItem = this.rtvItemRepository.findByIdAndIsDeleted(rtvItem.getId(), false);
			if (oldRtvItem.isPresent()) {
				try {
					oldRtvItem = Optional.ofNullable((RtvItem) oldRtvItem.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
			List<GrnItem> grnItems = this.grnItemRepository.findByGrnIdAndItemId(rtvItem.getGrnId(), rtvItem.getItemId());
			for (GrnItem grnItem : grnItems) {
			Double newQuantity = rtvItem.getReturnQuantity();
			Double oldQuantity = oldRtvItem.get().getReturnQuantity();
			Double difference = newQuantity - oldQuantity;
			remainedQuantity = grnItem.getUnbilledQuantity() - difference;
				if (remainedQuantity < 0) {
					throw new CustomException("Return quantity should be less than or equals to unbilled quantity.");
				}
				grnItem.setUnbilledQuantity(remainedQuantity);
			}
			this.grnItemRepository.saveAll(grnItems);
		}
		rtvItem.setRtvId(rtv.getId());
		rtvItem.setRtvNumber(rtv.getRtvNumber());
		rtvItem.setLastModifiedBy(username);
		rtvItem = this.rtvItemRepository.save(rtvItem);
		if (rtvItem == null) {
			log.info("Error while Saving the Address in rtv.");
			throw new CustomMessageException("Error while Saving the Address in rtv.");
		}
		log.info("RTV Item is saved successfully.");
		
		// find the updated values and save to history table
		if (oldRtvItem.isPresent()) {
			if (rtvItem.isDeleted()) {
				this.rtvHistoryRepository.save(this.prepareRtvHistory(rtvItem.getRtvNumber(), rtvItem.getId(),
						AppConstants.RTV_ITEM, Operation.DELETE.toString(), rtvItem.getLastModifiedBy(),
						String.valueOf(rtvItem.getId()), null));
			} else {
				List<RtvHistory> rtvHistories = new ArrayList<RtvHistory>();
				try {
					rtvHistories = oldRtvItem.get().compareFields(rtvItem);
					if (CollectionUtils.isNotEmpty(rtvHistories)) {
						this.rtvHistoryRepository.saveAll(rtvHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException(
							"Error while comparing the new and old objects. Please contact administrator.");
				}
			}
		} else {
			this.rtvHistoryRepository.save(this.prepareRtvHistory(rtvItem.getRtvNumber(), rtvItem.getId(),
					AppConstants.RTV_ITEM, Operation.CREATE.toString(), rtvItem.getLastModifiedBy(), null,
					String.valueOf(rtvItem.getId())));
		}
		log.info("RTV Items History is saved successfully.");
		return rtvItem;
	}

	/**
	 * 16-Oct-2022 Prepares the history objects for all forms and their
	 * child. Use this if you need single object only
	 * 
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public RtvHistory prepareRtvHistory(String rtvNumber, Long childId, String moduleName, String operation,
			String lastModifiedBy, String oldValue, String newValue) {
		RtvHistory rtvHistory = new RtvHistory();
		rtvHistory.setRtvNumber(rtvNumber);
		rtvHistory.setChildId(childId);
		rtvHistory.setModuleName(moduleName);
		rtvHistory.setChangeType(AppConstants.UI);
		rtvHistory.setOperation(operation);
		rtvHistory.setOldValue(oldValue);
		rtvHistory.setNewValue(newValue);
		rtvHistory.setLastModifiedBy(lastModifiedBy);
		return rtvHistory;
	}

	// update history of rtv
	private void updateRtvHistory(Optional<Rtv> oldRtv, Rtv rtv) {
		if (rtv != null) {
			if (oldRtv.isPresent()) {
				// insert the updated fields in history table
				List<RtvHistory> rtvHistories = new ArrayList<RtvHistory>();
				try {
					rtvHistories = oldRtv.get().compareFields(rtv);
					if (CollectionUtils.isNotEmpty(rtvHistories)) {
						this.rtvHistoryRepository.saveAll(rtvHistories);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.error("Error while comparing the new and old objects. Please contact administrator.");
					throw new CustomException(
							"Error while comparing the new and old objects. Please contact administrator.");
				}
			} else {
				// Insert in history table as Operation - INSERT
				this.rtvHistoryRepository.save(this.prepareRtvHistory(rtv.getRtvNumber(), null, AppConstants.RTV,
						Operation.CREATE.toString(), rtv.getLastModifiedBy(), null, null));
			}
			log.info("Rtv History Saved successfully.");
		} else {
			log.error("Error while saving the rtv.");
			throw new CustomException("Error while saving the rtv.");
		}
	}

	@Override
	public Rtv getRtvById(Long id) {
		Optional<Rtv> rtv = Optional.ofNullable(new Rtv());
		rtv = rtvRepository.findByIdAndIsDeleted(id, false);

		if (rtv.isPresent()) {
			// 1. Get rtvItem
			List<RtvItem> rtvItem = this.rtvItemRepository.findByRtvIdAndIsDeleted(id, false);
			if (CollectionUtils.isNotEmpty(rtvItem)) {
				rtv.get().setRtvItems(rtvItem);
			}
			boolean isRoutingActive = this.findIsApprovalRoutingActive(rtv.get().getSubsidiaryId());
			if (isRoutingActive) {
				String status = rtv.get().getApprovalStatus();
				if (!TransactionStatus.OPEN.getTransactionStatus().equalsIgnoreCase(status) && !TransactionStatus.REJECTED.getTransactionStatus().equalsIgnoreCase(status)) {
					isRoutingActive = false;
				}
			}
			rtv.get().setApprovalRoutingActive(isRoutingActive);
		} else {
			throw new CustomMessageException("Rtv Not Found against given rtv id : " + id);
		}
		return rtv.get();
	}

	@Override
	public List<RtvHistory> findHistoryByRtvNumber(String rtvNumber, Pageable pageable) {
		return rtvHistoryRepository.findByRtvNumber(rtvNumber, pageable);
	}

	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<Rtv> rtv = new ArrayList<Rtv>();

		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest).toString();

		// get list
		rtv = this.rtvDao.findAll(whereClause, paginationRequest);

		// getting count
		Long totalRecords = this.rtvDao.getCount(whereClause);

		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(), rtv, totalRecords);
	}

	private StringBuilder prepareWhereClause(PaginationRequest paginationRequest) {
		Map<String, ?> filters = paginationRequest.getFilters();

		Long subsidiaryId = null;
		Long supplierId = null;
		String fromDate = null;
		String toDate = null;
		Long location = null;

		if (filters.containsKey(FilterNames.SUBSIDIARY_ID))
			subsidiaryId = ((Number) filters.get(FilterNames.SUBSIDIARY_ID)).longValue();
		if (filters.containsKey(FilterNames.SUPPLIER_ID))
			supplierId = ((Number) filters.get(FilterNames.SUPPLIER_ID)).longValue();
		if (filters.containsKey(FilterNames.FROM_DATE))
			fromDate = (String) filters.get(FilterNames.FROM_DATE);
		if (filters.containsKey(FilterNames.TO_DATE))
			toDate = (String) filters.get(FilterNames.TO_DATE);
		if (filters.containsKey(FilterNames.LOCATION))
			location = ((Number) filters.get(FilterNames.LOCATION)).longValue();

		StringBuilder whereClause = new StringBuilder(" AND r.isDeleted is false ");
		if (subsidiaryId != null) {
			whereClause.append(" AND r.subsidiaryId = ").append(subsidiaryId).append(" ");
		}
		if (supplierId != null) {
			whereClause.append(" AND r.supplierId = ").append(supplierId).append(" ");
		}
		if (StringUtils.isNotEmpty(fromDate)) {
			whereClause.append(" AND to_char(r.rtvDate, 'yyyy-MM-dd') >= '").append(fromDate).append("' ");
		}
		if (StringUtils.isNotEmpty(toDate)) {
			whereClause.append(" AND to_char(r.rtvDate, 'yyyy-MM-dd') <= '").append(toDate).append("' ");
		}
		if (location != null) {
			whereClause.append(" AND r.locationId = ").append(location).append(" ");
		}

		return whereClause;
	}

	@Override
	public Boolean sendForApproval(Long id) {
		Boolean isSentForApproval = false;

		try {
			/**
			 * Due to single transaction we are getting updated value when we find from repo after the update
			 * hence finding old one first
			 */
			// Get the existing object using the deep copy
			Optional<Rtv> oldRtv = this.findOldDeepCopiedRtv(id);

			Optional<Rtv> rtv = Optional.empty();
			rtv = this.findById(id);

			/**
			 * Check routing is active or not
			 */
			boolean isRoutingActive = rtv.get().isApprovalRoutingActive();
			if (!isRoutingActive) {
				log.error("Routing is not active for the RTV : " + id + ". Please update your configuration. ");
				throw new CustomMessageException("Routing is not active for the RTV : " + id + ". Please update your configuration. ");
			}
			
			Double transactionalAmount = this.rtvItemRepository.findTotalEstimatedAmountForRtv(id);
			log.info("Total estimated transaction amount for RTV is :: " + transactionalAmount);
			
			// if amount is null then throw error
			if (transactionalAmount == null || transactionalAmount == 0.0) {
				log.error("There is no available Approval Process for this transaction.");
				throw new CustomMessageException("There is no available Approval Process for this transaction.");
			}
			
			ApprovalRequest approvalRequest = new ApprovalRequest();
			approvalRequest.setSubsidiaryId(rtv.get().getSubsidiaryId());
			approvalRequest.setFormName(FormNames.RTV.getFormName());
			approvalRequest.setTransactionAmount(transactionalAmount);
			approvalRequest.setLocationId(rtv.get().getLocationId());
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
			
			rtv.get().setApproverSequenceId(sequenceId);
			rtv.get().setApproverMaxLevel(level);
			rtv.get().setApproverPreferenceId(approverPreferenceId);
			
			String levelToFindRole = "L1";
			if (AppConstants.APPROVAL_TYPE_INDIVIDUAL.equals(approvalPreference.getApprovalType())) {
				levelToFindRole = level;
			}
			approvalRequest = this.masterServiceClient.findApproverByLevelAndSequence(approverPreferenceId, levelToFindRole, sequenceId);

			this.updateApproverDetailsInRtv(rtv, approvalRequest);
			rtv.get().setApprovalStatus(TransactionStatus.PENDING_APPROVAL.getTransactionStatus());
			log.info("Approver is found and details is updated for RTV :: " + rtv.get());
			
			this.saveRtvForApproval(rtv.get(), oldRtv);
			log.info("RTV is saved successfully with Approver details.");
			
			masterServiceClient.sendEmailByApproverId(rtv.get().getNextApprover(), FormNames.RTV.getFormName());
			
			isSentForApproval = true;
		} catch (Exception e) {
			log.error("Error while sending PR for approval for id - " + id);
			e.printStackTrace();
			throw new CustomMessageException("Error while sending RTV for approval for id - " + id + ", Message : " + e.getLocalizedMessage());
		}
		
		return isSentForApproval;
	}
	
	/**
	 * Save RTV after the approval details change
	 * @param rtv
	 */
	private void saveRtvForApproval(Rtv rtv, Optional<Rtv> oldRtv) {
		rtv.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		rtv = this.rtvRepository.save(rtv);
		
		if (rtv == null) {
			log.info("Error while saving the Rtv after the Approval.");
			throw new CustomMessageException("Error while saving the Rtv after the Approval.");
		}
		log.info("RTV saved successfully :: " + rtv.getRtvNumber());
		
		// update the data in PR history table
		this.updateRtvHistory(oldRtv, rtv);
		log.info("Rtv history is updated. after approval change.");		
	}
	
	/**
	 * Set/Prepares the approver details in the RTV object
	 * 
	 * @param purchaseRequisition
	 * @param approvalRequest
	 */
	private void updateApproverDetailsInRtv(Optional<Rtv> rtv, ApprovalRequest approvalRequest) {
		rtv.get().setApprovedBy(rtv.get().getNextApprover());
		rtv.get().setNextApprover(approvalRequest.getNextApprover());
		rtv.get().setNextApproverRole(approvalRequest.getNextApproverRole());
		rtv.get().setNextApproverLevel(approvalRequest.getNextApproverLevel());
	}
	
	private Optional<Rtv> findOldDeepCopiedRtv(Long id) {
		Optional<Rtv> rtv = this.rtvRepository.findByIdAndIsDeleted(id, false);
		if (rtv.isPresent()) {
			try {
				rtv = Optional.ofNullable((Rtv) rtv.get().clone());
				log.info("Existing Rtv is copied.");
			} catch (CloneNotSupportedException e) {
				log.error("Error while Cloning the object. Please contact administrator.");
				throw new CustomException("Error while Cloning the object. Please contact administrator.");
			}
		}
		return rtv;
	}
	
	public Optional<Rtv> findById(Long id) {
		Optional<Rtv> rtv = Optional.empty();
		rtv = rtvRepository.findByIdAndIsDeleted(id, false);

		if (!rtv.isPresent()) {
			log.error("Rtv Not Found against given rtv id : " + id);
			throw new CustomMessageException("Rtv Not Found against given rtv id : " + id);
		}
		boolean isRoutingActive = this.findIsApprovalRoutingActive(rtv.get().getSubsidiaryId());
		rtv.get().setApprovalRoutingActive(isRoutingActive);
		
		return rtv;
	}
	
	@Override
	public Boolean approveAllRtvs(List<Long> rtvIds) {
		Boolean isAllPoApproved = false;
		try {
			
			for (Long rtvId : rtvIds) {
				log.info("Approval Process is started for RTV-id :: " + rtvId);

				/**
				 * Due to single transaction we are getting updated value when we find from repo after the update
				 * hence finding old one first
				 */
				// Get the existing object using the deep copy
				Optional<Rtv> oldRtv = this.findOldDeepCopiedRtv(rtvId);

				Optional<Rtv> rtv = Optional.empty();
				rtv = this.findById(rtvId);

				/**
				 * Check routing is active or not
				 */
				boolean isRoutingActive = rtv.get().isApprovalRoutingActive();
				if (!isRoutingActive) {
					log.error("Routing is not active for the RTV : " + rtvId + ". Please update your configuration. ");
					throw new CustomMessageException("Routing is not active for the RTV : " + rtvId + ". Please update your configuration. ");
				}
				
				// meta data
				Long approvalPreferenceId = rtv.get().getApproverPreferenceId();
				Long sequenceId = rtv.get().getApproverSequenceId();
				String maxLevel = rtv.get().getApproverMaxLevel();
				
				String approvalPreferenceType = this.masterServiceClient.getTypeByApprovalId(approvalPreferenceId);
				
				ApprovalRequest approvalRequest = new ApprovalRequest();
				
				if (AppConstants.APPROVAL_TYPE_CHAIN.equalsIgnoreCase(approvalPreferenceType)
						&& !maxLevel.equals(rtv.get().getNextApproverLevel())) {
					Long currentLevelNumber = Long.parseLong(rtv.get().getNextApproverLevel().replaceFirst("L", "")) + 1;
					String currentLevel = "L" + currentLevelNumber;
					approvalRequest = this.masterServiceClient.findApproverByLevelAndSequence(approvalPreferenceId, currentLevel, sequenceId);
					rtv.get().setApprovalStatus(TransactionStatus.PARTIALLY_APPROVED.getTransactionStatus());
				} else {
					rtv.get().setApprovalStatus(TransactionStatus.APPROVED.getTransactionStatus());
				}
				log.info("Approval Request is found for Rtv :: " + approvalRequest.toString());

				this.updateApproverDetailsInRtv(rtv, approvalRequest);
				log.info("Approver is found and details is updated :: " + rtv.get());
				
				this.saveRtvForApproval(rtv.get(), oldRtv);
				log.info("Rtv is saved successfully with Approver details.");

				masterServiceClient.sendEmailByApproverId(rtv.get().getNextApprover(), FormNames.RTV.getFormName());
				
				log.info("Approval Process is Finished for Rtv :: " + rtv.get().getRtvNumber());
			}
			
			isAllPoApproved = true;
		} catch (Exception e) {
			log.error("Error while approving the Rtv.");
			e.printStackTrace();
			throw new CustomMessageException("Error while approving the Rtv. Message : " + e.getLocalizedMessage());
		}
		return isAllPoApproved;
	}
	
	private boolean findIsApprovalRoutingActive(Long subsidiaryId) {
		return this.masterServiceClient.findIsApprovalRoutingActive(subsidiaryId, FormNames.RTV.getFormName());
	}
	
	@Override
	public Boolean rejectAllRtvs(List<Rtv> rtvs) {
		for (Rtv rtv : rtvs) {
			String rejectComments = rtv.getRejectedComments();
			
			if (StringUtils.isEmpty(rejectComments)) {
				log.error("Reject Comments is required.");
				throw new CustomException("Reject Comments is required. It is missing for RTV : " + rtv.getId());
			}
			
			Optional<Rtv> oldRtv = this.findOldDeepCopiedRtv(rtv.getId());

			Optional<Rtv> existingRtv = this.rtvRepository.findByIdAndIsDeleted(rtv.getId(), false);
			existingRtv.get().setApprovalStatus(TransactionStatus.REJECTED.getTransactionStatus());
			existingRtv.get().setRejectedComments(rejectComments);
			existingRtv.get().setApprovedBy(null);
			existingRtv.get().setNextApprover(null);
			existingRtv.get().setNextApproverRole(null);
			existingRtv.get().setNextApproverLevel(null);
			existingRtv.get().setApproverSequenceId(null);
			existingRtv.get().setApproverMaxLevel(null);
			existingRtv.get().setApproverPreferenceId(null);
			existingRtv.get().setNoteToApprover(null);

			log.info("Approval Fields are restored to empty. For Rtv : " + rtv);
			
			this.saveRtvForApproval(existingRtv.get(), oldRtv);
			log.info("Rtv is saved successfully with restored Approver details.");

			log.info("Approval Process is Finished for Rtv-id :: " + rtv.getId());
		}
		return true;
	}

	@Override
	public Boolean updateNextApprover(Long approverId, Long rtvId) {
		Optional<Rtv> rtv = this.rtvRepository.findByIdAndIsDeleted(rtvId, false);
		
		if (!rtv.isPresent()) {
			log.error("Rtv Not Found against given Supplier id : " + rtvId);
			throw new CustomMessageException("Rtv Not Found against given Rtv id : " + rtvId);
		}
		rtv.get().setNextApprover(String.valueOf(approverId));
		rtv.get().setLastModifiedBy(CommonUtils.getLoggedInUsername());
		this.rtvRepository.save(rtv.get());
		
		return true;
	}
	
	@Override
	public Boolean selfApprove(Long rtvId) {
		Optional<Rtv> rtv = this.rtvRepository.findByIdAndIsDeleted(rtvId, false);
		
		if (!rtv.isPresent()) {
			log.error("Rtv Not Found against given Rtv id : " + rtvId);
			throw new CustomMessageException("Rtv Not Found against given Rtv id : " + rtvId);
		}
		rtv.get().setApprovalStatus(TransactionStatus.APPROVED.getTransactionStatus());
		rtv.get().setLastModifiedBy(CommonUtils.getLoggedInUsername());
		
		if (this.rtvRepository.save(rtv.get()) != null) return true;
		else throw new CustomException("Error in self approve. Please contact System Administrator");
	}
}
