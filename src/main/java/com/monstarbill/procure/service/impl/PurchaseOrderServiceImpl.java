package com.monstarbill.procure.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
import com.monstarbill.procure.dao.PurchaseOrderDao;
import com.monstarbill.procure.enums.FormNames;
import com.monstarbill.procure.enums.MatchType;
import com.monstarbill.procure.enums.Operation;
import com.monstarbill.procure.enums.PaymentTerm;
import com.monstarbill.procure.enums.PoType;
import com.monstarbill.procure.enums.TransactionStatus;
import com.monstarbill.procure.feignclient.MasterServiceClient;
import com.monstarbill.procure.feignclient.SetupServiceClient;
import com.monstarbill.procure.models.Account;
import com.monstarbill.procure.models.Item;
import com.monstarbill.procure.models.PrItem;
import com.monstarbill.procure.models.PurchaseOrder;
import com.monstarbill.procure.models.PurchaseOrderHistory;
import com.monstarbill.procure.models.PurchaseOrderItem;
import com.monstarbill.procure.models.PurchaseRequisition;
import com.monstarbill.procure.models.QuotationAnalysis;
import com.monstarbill.procure.models.QuotationAnalysisItem;
import com.monstarbill.procure.models.Supplier;
import com.monstarbill.procure.models.SupplierAddress;
import com.monstarbill.procure.models.TaxGroup;
import com.monstarbill.procure.payload.request.ApprovalRequest;
import com.monstarbill.procure.payload.request.GenerateRfqPoRequest;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.request.RfqPoRequest;
import com.monstarbill.procure.payload.response.ApprovalPreference;
import com.monstarbill.procure.payload.response.PaginationResponse;
import com.monstarbill.procure.repository.PrItemRepository;
import com.monstarbill.procure.repository.PurchaseOrderHistoryRepository;
import com.monstarbill.procure.repository.PurchaseOrderItemRepository;
import com.monstarbill.procure.repository.PurchaseOrderRepository;
import com.monstarbill.procure.repository.PurchaseRequisitionRepository;
import com.monstarbill.procure.repository.QuotationAnalysisItemRepository;
import com.monstarbill.procure.repository.QuotationAnalysisRepository;
import com.monstarbill.procure.service.PurchaseOrderService;
import com.monstarbill.procure.service.PurchaseRequisitionService;
import com.monstarbill.procure.service.QuotationAnalysisService;
import com.monstarbill.procure.service.QuotationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

	@Autowired
	private PurchaseOrderRepository purchaseOrderRepository;
	
	@Autowired
	private PurchaseOrderItemRepository purchaseOrderItemRepository;
	
	@Autowired
	private PurchaseOrderHistoryRepository purchaseOrderHistoryRepository;

	@Autowired
	private PurchaseRequisitionService purchaseRequisitionService;
	
	@Autowired
	private QuotationAnalysisService quotationAnalysisService;
	
	@Autowired
	private QuotationService quotationService;
	
	@Autowired
	private PurchaseOrderDao purchaseOrderDao;
	
	@Autowired
	private QuotationAnalysisRepository quotationAnalysisRepository;
	
	@Autowired
	private PrItemRepository prItemRepository;
	
	@Autowired
	private PurchaseRequisitionRepository prRepository;
	
	@Autowired
	private QuotationAnalysisItemRepository qaItemRepository;
	
	@Autowired
	private SetupServiceClient setupServiceClient;
	
	@Autowired
	private MasterServiceClient masterServiceClient;
	/**
	 * 1. save the PO
	 * 2. save all the items of the PO line by line
	 */
	@Override
	public PurchaseOrder save(PurchaseOrder purchaseOrder) {
		log.info("PO save started...");
		
		this.validateManualPo(purchaseOrder);
		
		Optional<PurchaseOrder> oldPurchaseOrder = Optional.empty();
		String username = CommonUtils.getLoggedInUsername();
		
		if (purchaseOrder.getId() == null) {
			purchaseOrder.setOriginalSupplierId(purchaseOrder.getSupplierId());
			purchaseOrder.setCreatedBy(CommonUtils.getLoggedInUsername());
			purchaseOrder.setPoStatus(TransactionStatus.OPEN.getTransactionStatus());
			String transactionalDate = CommonUtils.convertDateToFormattedString(purchaseOrder.getPoDate());
			String documentSequenceNumber = this.setupServiceClient.getDocumentSequenceNumber(transactionalDate, purchaseOrder.getSubsidiaryId(), FormNames.PO.getFormName(), false);
			if (StringUtils.isEmpty(documentSequenceNumber)) {
				throw new CustomMessageException("Please validate your configuration to generate the PO Number");
			}
			purchaseOrder.setPoNumber(documentSequenceNumber);
		} else {
			// Get the existing object using the deep copy
			oldPurchaseOrder = this.purchaseOrderRepository.findByIdAndIsDeleted(purchaseOrder.getId(), false);
			if (oldPurchaseOrder.isPresent()) {
				try {
					oldPurchaseOrder = Optional.ofNullable((PurchaseOrder) oldPurchaseOrder.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		// check for supplier
		if (purchaseOrder.isSupplierUpdatable()) {
			purchaseOrder.setNoteToApprover("Tha Supplier has changed from QA.");
		} else {
			purchaseOrder.setSupplierId(purchaseOrder.getOriginalSupplierId());
			purchaseOrder.setNoteToApprover(null);
		}
		purchaseOrder.setLastModifiedBy(username);
		PurchaseOrder purchaseOrderSaved;
		try {
			purchaseOrderSaved = this.purchaseOrderRepository.save(purchaseOrder);
			
			/**
			 * if PO is PR based then
			 * 		mark PR as PO
			 */
			if (PoType.PR_BASED.getPoType().equalsIgnoreCase(purchaseOrder.getPoType())) {
				String prIdStr = purchaseOrderSaved.getPrId();
				String[] prIds = prIdStr.split("\\|");
				for (int i = 0; i < prIds.length; i++) {
					Long prId = Long.parseLong(prIds[i]);
					Optional<PurchaseRequisition> pr = this.prRepository.findByIdAndIsDeleted(prId, false);
					if (pr.isPresent()) {
						pr.get().setUsedFor(FormNames.PO.getFormName());
						this.prRepository.save(pr.get());
					}
				}
				
				if (oldPurchaseOrder.isPresent()) {
					String oldPrIdStr = oldPurchaseOrder.get().getPrId();
					String[] oldPrIds = oldPrIdStr.split("\\|");
					
					HashSet<String> prIdsSet = new HashSet<String>(Arrays.asList(prIds));
					HashSet<String> oldPrIdsSet = new HashSet<String>(Arrays.asList(oldPrIds));
					oldPrIdsSet.removeAll(prIdsSet);
					
					// if user unchecked the PR then it should be null
					if (oldPrIdsSet.size() > 0) {
						for (String prIdString : oldPrIdsSet) {
							Long prId = Long.parseLong(prIdString);
							Optional<PurchaseRequisition> pr = this.prRepository.findByIdAndIsDeleted(prId, false);
							if (pr.isPresent()) {
								
								
								// remove the PO ref from pr item if item removed from
								List<PrItem> prItems = this.prItemRepository.findByPrIdAndPoId(prId, purchaseOrderSaved.getId());
								for (PrItem prItem : prItems) {
									prItem.setPoId(null);
									prItem.setLastModifiedBy(username);
									this.prItemRepository.save(prItem);
								}
								
								Long totalItems = this.prItemRepository.findCountByPrId(prId);
								Long unprocessedItems = this.prItemRepository.findUnprocessedItemsCountByPo(prId);
								
								String status = TransactionStatus.PARTIALLY_PROCESSED.getTransactionStatus();
								if (totalItems == unprocessedItems) {
									status = TransactionStatus.APPROVED.getTransactionStatus();
									pr.get().setUsedFor(null);
								}
								pr.get().setPrStatus(status);
								this.prRepository.save(pr.get());
								
								List<PurchaseOrderItem> poItemsToDelete = this.purchaseOrderItemRepository.findByPoIdAndPrId(purchaseOrderSaved.getId(), prId);
								for (PurchaseOrderItem purchaseOrderItem : poItemsToDelete) {
									purchaseOrderItem.setDeleted(true);
									purchaseOrderItem.setLastModifiedBy(username);
									this.purchaseOrderItemRepository.save(purchaseOrderItem);
								}
								
								for (PurchaseOrderItem poItem : purchaseOrder.getPurchaseOrderItems()) {
									if (prId.equals(poItem.getPrId())) {
										poItem.setDeleted(true);
									}
								}
							}
						}
					}
				}
			}
			
		} catch (DataIntegrityViolationException e) {
			log.error("Purchase order unique constrain violetd." + e.getMostSpecificCause());
			throw new CustomException("Purchase order unique constrain violetd :" + e.getMostSpecificCause());
		}
		
		if (purchaseOrderSaved == null) {
			log.info("Error while saving the Purchase Order.");
			throw new CustomMessageException("Error while saving the Purchase Order.");
		}
		log.info("PO saved successfully :: " + purchaseOrderSaved.getId());
		
		// update the data in Item history table
		this.updatePurchaseOrderHistory(purchaseOrderSaved, oldPurchaseOrder);
		Long poId = purchaseOrderSaved.getId();
		
		log.info("PO Items is started.");
		List<PurchaseOrderItem> poItems = purchaseOrder.getPurchaseOrderItems();
		if (CollectionUtils.isNotEmpty(poItems)) {
			for (PurchaseOrderItem purchaseOrderItem : poItems) {
				log.info("PO Item is Started :: " + purchaseOrderItem.toString());
				purchaseOrderItem.setPoId(poId);
				purchaseOrderItem.setPoNumber(purchaseOrderSaved.getPoNumber());
				purchaseOrderItem.setCreatedBy(username); 
				purchaseOrderItem.setLastModifiedBy(username);
				purchaseOrderItem = this.save(purchaseOrderItem);
				log.info("PO Item is Finished :: " + purchaseOrderItem.getId());
				
				if (PoType.PR_BASED.getPoType().equalsIgnoreCase(purchaseOrder.getPoType())) {
					// useful to check which item assigned to which RFQ
					Optional<PrItem> prItem = this.prItemRepository.findByPrIdAndItemIdAndIsDeleted(purchaseOrderItem.getPrId(), purchaseOrderItem.getItemId(), false);
					if (prItem.isPresent()) {
						if (!purchaseOrderItem.isDeleted()) {
							prItem.get().setPoId(purchaseOrderSaved.getId());
						} else {
							prItem.get().setPoId(null);
						}
						this.prItemRepository.save(prItem.get());
					}
				} else if (PoType.QA_BASED.getPoType().equalsIgnoreCase(purchaseOrder.getPoType())) {
					List<QuotationAnalysisItem> qaItems = this.qaItemRepository.findByQaIdAndItemIdAndApprovedSupplierAndIsDeleted(purchaseOrder.getQaId(), purchaseOrderItem.getItemId(), purchaseOrder.getSupplierId(), false);
					for (QuotationAnalysisItem qaItem : qaItems) {
						qaItem.setPoId(poId);
						this.qaItemRepository.save(qaItem);
					}
				}
			}
			purchaseOrderSaved.setPurchaseOrderItems(purchaseOrder.getPurchaseOrderItems());
		}
		log.info("PO Items is Finished.");
		
		log.info("PO with childs saved successfully.");
		if (PoType.PR_BASED.getPoType().equalsIgnoreCase(purchaseOrder.getPoType())) {
			String[] prIds = purchaseOrder.getPrId().split("\\|");
			for (int i=0; i<prIds.length; i++) {
				Long prId = Long.parseLong(prIds[i]);
				Long unusedItemCount = prItemRepository.findUnprocessedItemsCountByPo(prId);

				Optional<PurchaseRequisition> purchaseRequisition = prRepository.findByIdAndIsDeleted(prId, false);
				if (unusedItemCount == 0) {
					purchaseRequisition.get().setPrStatus(TransactionStatus.PROCESSED.getTransactionStatus());
				} else {
					purchaseRequisition.get().setPrStatus(TransactionStatus.PARTIALLY_PROCESSED.getTransactionStatus());
				}
				prRepository.save(purchaseRequisition.get());
			}
		}
		
		return purchaseOrderSaved;
	}

	/**
	 * To validate the Manual PO
	 * @param purchaseOrder
	 */
	private void validateManualPo(PurchaseOrder purchaseOrder) {
		log.info("Validate Manual PO is started.");
		/**
		 * If PO type is PR Based then PR Number is required. 
		 * If PO type is Non PR Based then PR Number is NOT required
		 */
		if (PoType.PR_BASED.getPoType().equalsIgnoreCase(purchaseOrder.getPoType()) 
				&& StringUtils.isEmpty(purchaseOrder.getPrId())) {
			throw new CustomBadRequestException("PR Number is required for PR Based PO Type.");
		}
		log.info("Validate Manual PO is Finished.");
	}

	/**
	 * This method save the data in history table
	 * Add entry as a Insert if Item is new 
	 * Add entry as a Update if Item is exists
	 * @param purchaseOrder
	 * @param oldPurchaseOrder
	 */
	private void updatePurchaseOrderHistory(PurchaseOrder purchaseOrder, Optional<PurchaseOrder> oldPurchaseOrder) {
		log.info("PO History is started.");
		if (oldPurchaseOrder.isPresent()) {
			// insert the updated fields in history table
			List<PurchaseOrderHistory> PurchaseOrderHistories = new ArrayList<PurchaseOrderHistory>();
			try {
				PurchaseOrderHistories = oldPurchaseOrder.get().compareFields(purchaseOrder);
				if (CollectionUtils.isNotEmpty(PurchaseOrderHistories)) {
					this.purchaseOrderHistoryRepository.saveAll(PurchaseOrderHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("Purchase Order History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT 
			this.purchaseOrderHistoryRepository.save(this.preparePurchaseOrderHistory(purchaseOrder.getPoNumber(), null, AppConstants.PURCHASE_ORDER, Operation.CREATE.toString(), purchaseOrder.getLastModifiedBy(), null, purchaseOrder.getPoNumber()));
		}
		log.info("PO History is Completed.");
	}
	
	/**
	 * Prepares the PO history object
	 * @param poNumber
	 * @param childId
	 * @param moduleName
	 * @param operation
	 * @param lastModifiedBy
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public PurchaseOrderHistory preparePurchaseOrderHistory(String poNumber, Long childId, String moduleName, String operation, String lastModifiedBy, String oldValue, String newValue) {
		PurchaseOrderHistory purchaseOrderHistory = new PurchaseOrderHistory();
		purchaseOrderHistory.setPoNumber(poNumber);
		purchaseOrderHistory.setChildId(childId);
		purchaseOrderHistory.setModuleName(moduleName);
		purchaseOrderHistory.setChangeType(AppConstants.UI);
		purchaseOrderHistory.setOperation(operation);
		purchaseOrderHistory.setOldValue(oldValue);
		purchaseOrderHistory.setNewValue(newValue);
		purchaseOrderHistory.setLastModifiedBy(lastModifiedBy);
		return purchaseOrderHistory;
	}
	
	/**
	 * Save the PO Item
	 * @param purchaseOrderItem
	 * @return
	 */
	public PurchaseOrderItem save(PurchaseOrderItem purchaseOrderItem) {
		log.info("PO Item save started...");
		Optional<PurchaseOrderItem> oldPurchaseOrderItem = Optional.empty();
		
		if (purchaseOrderItem.getId() == null) {
			purchaseOrderItem.setRemainQuantity(purchaseOrderItem.getQuantity());
			purchaseOrderItem.setCreatedBy(CommonUtils.getLoggedInUsername());
			purchaseOrderItem.setUnbilledQuantity(purchaseOrderItem.getQuantity());
		} else {
			// Get the existing object using the deep copy
			oldPurchaseOrderItem = this.purchaseOrderItemRepository.findByIdAndIsDeleted(purchaseOrderItem.getId(), false);
			if (oldPurchaseOrderItem.isPresent()) {
				try {
					oldPurchaseOrderItem = Optional.ofNullable((PurchaseOrderItem) oldPurchaseOrderItem.get().clone());
				} catch (CloneNotSupportedException e) {
					log.error("Error while Cloning the object. Please contact administrator.");
					throw new CustomException("Error while Cloning the object. Please contact administrator.");
				}
			}
		}
		if (purchaseOrderItem.getId() != null) {
			Optional<PurchaseOrderItem> purchaseOrderExistValue = Optional.empty();
			if(purchaseOrderExistValue.isPresent()) {
				purchaseOrderExistValue = this.purchaseOrderItemRepository.getByPoId(purchaseOrderItem.getId());
				purchaseOrderExistValue.get().getRemainQuantity();
				purchaseOrderItem.setRemainQuantity(purchaseOrderExistValue.get().getRemainQuantity());
			}
		}

		purchaseOrderItem.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		PurchaseOrderItem purchaseOrderItemSaved = this.purchaseOrderItemRepository.save(purchaseOrderItem);
		
		if (purchaseOrderItemSaved == null) {
			log.info("Error while saving the Purchase Order Item.");
			throw new CustomMessageException("Error while saving the Purchase Order Item.");
		}
		log.info("PO Item is saved :: " + purchaseOrderItemSaved.getId());
		
		// update the data in Item history table
		this.updatePurchaseOrderItemHistory(purchaseOrderItemSaved, oldPurchaseOrderItem);
		
		log.info("PO Item save Completed...");
		return purchaseOrderItemSaved;
	}
	
	private void updatePurchaseOrderItemHistory(PurchaseOrderItem purchaseOrderItem, Optional<PurchaseOrderItem> oldPurchaseOrderItem) {
		if (oldPurchaseOrderItem.isPresent()) {
			// insert the updated fields in history table
			List<PurchaseOrderHistory> PurchaseOrderHistories = new ArrayList<PurchaseOrderHistory>();
			try {
				PurchaseOrderHistories = oldPurchaseOrderItem.get().compareFields(purchaseOrderItem);
				if (CollectionUtils.isNotEmpty(PurchaseOrderHistories)) {
					this.purchaseOrderHistoryRepository.saveAll(PurchaseOrderHistories);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error while comparing the new and old objects. Please contact administrator.");
				throw new CustomException("Error while comparing the new and old objects. Please contact administrator.");
			}
			log.info("Purchase Order Item History is updated successfully");
		} else {
			// Insert in history table as Operation - INSERT 
			this.purchaseOrderHistoryRepository.save(this.preparePurchaseOrderHistory(purchaseOrderItem.getPoNumber(), purchaseOrderItem.getId(), AppConstants.PURCHASE_ORDER_ITEM, Operation.CREATE.toString(), purchaseOrderItem.getLastModifiedBy(), null, String.valueOf(purchaseOrderItem.getId())));
		}
	}
	
	@Override
	public PurchaseOrder findByPoId(Long id) {
		Optional<PurchaseOrder> purchaseOrder = Optional.empty();
		purchaseOrder = this.purchaseOrderRepository.findByIdAndIsDeleted(id, false);

		if (!purchaseOrder.isPresent()) {
			log.info("PO is not found against the provided PO-id :: " + id);
			throw new CustomMessageException("PO is not found against the provided PO-id :: " + id);
		}
		if (purchaseOrder.get().getQaId() != null) {
			Optional<QuotationAnalysis> qa = quotationAnalysisRepository.findByIdAndIsDeleted(purchaseOrder.get().getQaId(), false);
			if (qa.isPresent()) purchaseOrder.get().setQaNumber(qa.get().getQaNumber());
		}
		
		Map<Long, String> prNumbers = new TreeMap<Long, String>();		
		if (StringUtils.isNotEmpty(purchaseOrder.get().getPrId())) {
			String[] prIds = purchaseOrder.get().getPrId().split("\\|");
			for (int i=0; i < prIds.length; i++) {
				Optional<PurchaseRequisition> pr = prRepository.findByIdAndIsDeleted(Long.parseLong(prIds[i]), false);
				prNumbers.put(pr.get().getId(), pr.get().getPrNumber());
			}
		}
		purchaseOrder.get().setPrNumbers(prNumbers);
		
		log.info("PO is found against the PO-Number :: " + id);
		boolean isRoutingActive = this.findIsApprovalRoutingActive(purchaseOrder.get().getSubsidiaryId());
		if (isRoutingActive) {
			String status = purchaseOrder.get().getPoStatus();
			if (!TransactionStatus.OPEN.getTransactionStatus().equalsIgnoreCase(status) && !TransactionStatus.REJECTED.getTransactionStatus().equalsIgnoreCase(status)) {
				isRoutingActive = false;
			}
		}
		purchaseOrder.get().setApprovalRoutingActive(isRoutingActive);

		Long revision = this.purchaseOrderRepository.findRevisionById(id.intValue());
		purchaseOrder.get().setRevision(revision.intValue());
		log.info("Revision is found & updated in object : " + revision);
		
		List<PurchaseOrderItem> poItems = this.purchaseOrderItemRepository.findItemsByPoId(id);
		if (CollectionUtils.isNotEmpty(poItems)) {
			for (PurchaseOrderItem poItem : poItems) {
				Account account = this.masterServiceClient.findByAccountId(poItem.getAccountId());
				if (account != null) poItem.setAccountCode(account.getCode());
			}
		}
		purchaseOrder.get().setPurchaseOrderItems(poItems);
		log.info("All PO items found.");
		
		return purchaseOrder.get();
	}

	@Override
	public PaginationResponse findAll(PaginationRequest paginationRequest) {
		List<PurchaseOrder> purchaseOrder = new ArrayList<PurchaseOrder>();

		// preparing where clause
		String whereClause = this.prepareWhereClause(paginationRequest);

		// get list
		purchaseOrder = this.purchaseOrderDao.findAll(whereClause, paginationRequest);

		// getting count
		Long totalRecords = this.purchaseOrderDao.getCount(whereClause);

		return CommonUtils.setPaginationResponse(paginationRequest.getPageNumber(), paginationRequest.getPageSize(),
				purchaseOrder, totalRecords);
	}

	private String prepareWhereClause(PaginationRequest paginationRequest) {
		Long subsidiaryId = null;
		Long supplierId = null;
		String type = null;
		Map<String, ?> filters = paginationRequest.getFilters();

		if (filters.containsKey(FilterNames.SUBSIDIARY_ID))
			subsidiaryId = ((Number) filters.get(FilterNames.SUBSIDIARY_ID)).longValue();
		if (filters.containsKey(FilterNames.SUPPLIER_ID))
			supplierId = ((Number) filters.get(FilterNames.SUPPLIER_ID)).longValue();
		if (filters.containsKey(FilterNames.TYPE))
			type = (String) filters.get(FilterNames.TYPE);

		StringBuilder whereClause = new StringBuilder(" AND po.isDeleted is false");
		if (subsidiaryId != null && subsidiaryId != 0) {
			whereClause.append(" AND po.subsidiaryId = ").append(subsidiaryId);
		}
		if (supplierId != null && supplierId != 0) {
			whereClause.append(" AND po.supplierId = ").append(supplierId);
		}
		if (StringUtils.isNotEmpty(type)) {
			whereClause.append(" AND lower(po.poType) like lower('%").append(type).append("%')");
		}
		return whereClause.toString();
	}

	@Override
	public List<PurchaseOrderHistory> findHistoryById(String poNumber, Pageable pageable) {
		List<PurchaseOrderHistory> histories = this.purchaseOrderHistoryRepository.findByPoNumberOrderById(poNumber, pageable);
		String createdBy = histories.get(0).getLastModifiedBy();
		histories.forEach(e->{
			e.setCreatedBy(createdBy);
		});
		return histories;
	}

	/**
	 * This method will generate the PO/RFQ automatically internally.
	 * IF Module Name is PO THEN generate PO's
	 * IF Module Name is RFQ THEN generate RFQ's
	 * 
	 * In both case
	 * 	IF isCreateCommonRfqPo is true THEN create Single PO/RFQ as per criteria (PR Currency, Location & Supplier)
	 * 	ELSE create separate PO/RFQ for every record 
	 */
	@Override
	public Boolean generatePoRfq(GenerateRfqPoRequest generateRfqPoRequest) {
		// This function will validate the data and will throw Bad Request Exception if validation fails
		this.validateAutoGenerateRfqPo(generateRfqPoRequest);
		if (FormNames.PO.getFormName().equalsIgnoreCase(generateRfqPoRequest.getModuleName())) {
			this.generatePo(generateRfqPoRequest);
		} else if (FormNames.RFQ.getFormName().equalsIgnoreCase(generateRfqPoRequest.getModuleName())) {
			this.quotationService.generateQuotation(generateRfqPoRequest); 
		} else {
			throw new CustomBadRequestException("Transaction Form Name should not be Empty.");
		}
		return true;
	}

	/**
	 * This method will generate PO's
	 * @param generateRfqPoRequest
	 */
	private void generatePo(GenerateRfqPoRequest generateRfqPoRequest) {
		List<RfqPoRequest> rfqPoRequests = generateRfqPoRequest.getRfqPoRequests();
		boolean isCreateCommonPo = generateRfqPoRequest.isCreateCommonRfqPo();
		
		if (isCreateCommonPo) {
//			Map<String, List<RfqPoRequest>> groupedPr = new HashMap<String, List<RfqPoRequest>>();
			Map<String, String> groupedPr = new HashMap<String, String>();
			
			// create grouping as per criteria
			for (RfqPoRequest rfqPoRequest : rfqPoRequests) {
				String keyName = rfqPoRequest.getPrLocationId() + "_" + rfqPoRequest.getPrCurrency() + "_" + rfqPoRequest.getSupplierId();
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
				String[] prNumbers = entry.getValue().split("\\|");
				
				Optional<RfqPoRequest> rfqPoRequest = rfqPoRequests.stream().filter(e -> e.getPrId() == Long.parseLong(prNumbers[0])).findFirst();
				if (rfqPoRequest.isPresent()) this.preparePo(rfqPoRequest.get(), entry.getValue());
			}
		} else {
			this.saveSeparatePo(rfqPoRequests);
		}
	}

	/**
	 * save the separate PO for every row
	 * @param rfqPoRequests
	 */
	private void saveSeparatePo(List<RfqPoRequest> rfqPoRequests) {
		for (RfqPoRequest rfqPoRequest : rfqPoRequests) {
			this.preparePo(rfqPoRequest, String.valueOf(rfqPoRequest.getPrId()));
		}
	}

	/**
	 * This method will prepare the Object for Common or Separate PO and save it with items & History
	 * @param rfqPoRequest
	 * @param prNumbersPipeSeprated
	 */
	private void preparePo(RfqPoRequest rfqPoRequest, String prNumbersPipeSeprated) {
//		String transactionalDate = CommonUtils.convertDateToFormattedString(rfqPoRequest.getTransactionalDate());
//		String documentSequenceNumber = this.setupServiceClient.getDocumentSequenceNumber(transactionalDate , rfqPoRequest.getSubsidiaryId(), FormNames.PO.getFormName(), false);
		
		PurchaseOrder purchaseOrder = new PurchaseOrder();

//		purchaseOrder.setPoNumber(documentSequenceNumber);
		purchaseOrder.setSubsidiaryId(rfqPoRequest.getSubsidiaryId());
		purchaseOrder.setPoType(PoType.PR_BASED.getPoType());
		purchaseOrder.setLocation(rfqPoRequest.getPrLocation());
		purchaseOrder.setLocationId(rfqPoRequest.getPrLocationId());
		purchaseOrder.setPrId(prNumbersPipeSeprated);
		
		purchaseOrder.setSupplierId(rfqPoRequest.getSupplierId());
		purchaseOrder.setPoDate(rfqPoRequest.getTransactionalDate());
		purchaseOrder.setPaymentTerm(PaymentTerm.DAYS_30.getPaymentTerm());
		purchaseOrder.setMatchType(MatchType.WAY_3.getMatchType());
		purchaseOrder.setCurrency(rfqPoRequest.getSupplierCurrency());
		purchaseOrder.setExchangeRate(1.00);
		purchaseOrder.setPoStatus(TransactionStatus.OPEN.getTransactionStatus());
		
		purchaseOrder.setCreatedBy(CommonUtils.getLoggedInUsername());
		purchaseOrder.setLastModifiedBy(CommonUtils.getLoggedInUsername());

		// iterate over all the PR's and fetch the items and set all to the PO
		List<PurchaseOrderItem> poItems = new ArrayList<PurchaseOrderItem>();
		Double lineAmount = 0.0;
		String[] prNumbers = prNumbersPipeSeprated.split("\\|");
		for (int i=0; i < prNumbers.length; i++) {
			Long prId = Long.parseLong(prNumbers[i]);
			log.info("Fetching the PR-Items for the PR-Number :: " + prId);
			List<PrItem> prItems = purchaseRequisitionService.findUnprocessedItemsByPrId(prId, FormNames.PO.getFormName());
			
			if (CollectionUtils.isNotEmpty(prItems)) {
				for (PrItem prItem : prItems) {
					lineAmount = 0.0;
					PurchaseOrderItem purchaseOrderItem = new PurchaseOrderItem();
					purchaseOrderItem.setItemId(prItem.getItemId());
					purchaseOrderItem.setQuantity(Double.valueOf(prItem.getQuantity()));
					purchaseOrderItem.setRemainQuantity(purchaseOrderItem.getQuantity());
					purchaseOrderItem.setRate(Double.valueOf(prItem.getRate()));
					// Added 01 as per comment in the file
					lineAmount = purchaseOrderItem.getQuantity() * purchaseOrderItem.getRate();
					purchaseOrderItem.setAmount(lineAmount);
					Double totalAmount = purchaseOrder.getAmount();
					if (totalAmount == null) {
						totalAmount = lineAmount;
					}
					purchaseOrder.setAmount(totalAmount);
					purchaseOrder.setTotalAmount(totalAmount);
					purchaseOrderItem.setReceivedByDate(prItem.getReceivedDate());
					purchaseOrderItem.setPrId(prId);
					purchaseOrderItem.setShipToLocationId(rfqPoRequest.getPrLocationId());
					purchaseOrderItem.setShipToLocation(rfqPoRequest.getPrLocation());
					purchaseOrderItem.setDepartment(prItem.getDepartment());
					poItems.add(purchaseOrderItem);
				}
			}
		}

		log.info("Going to save the PO...");
		purchaseOrder.setPurchaseOrderItems(poItems);
		this.save(purchaseOrder);
		
		log.info("PO is saved successfully.");
	}

	/**
	 * Validation of auto generate Requests for PO & RFQ
	 * This function will validate the data and will throw Bad Request Exception if validation fails
	 * @param generateRfqPoRequest
	 */
	private void validateAutoGenerateRfqPo(GenerateRfqPoRequest generateRfqPoRequest) {
		List<RfqPoRequest> rfqPoRequests = generateRfqPoRequest.getRfqPoRequests();
		for (RfqPoRequest rfqPoRequest : rfqPoRequests) {
			if (rfqPoRequest.getSubsidiaryId() == null) { 
				throw new CustomBadRequestException("Subsidiary should not be empty.");
			}
			if (rfqPoRequest.getPrId() == null) { 
				throw new CustomBadRequestException("PR Number should not be empty.");
			}
			if (rfqPoRequest.getPrDate() == null) { 
				throw new CustomBadRequestException("PR Date should not be empty.");
			}
			if (StringUtils.isEmpty(rfqPoRequest.getPrCurrency())) { 
				throw new CustomBadRequestException("PR Currency should not be empty.");
			}
			if (rfqPoRequest.getPrLocationId() == null || StringUtils.isEmpty(rfqPoRequest.getPrLocation())) { 
				throw new CustomBadRequestException("PR Location should not be empty.");
			}
			if (FormNames.RFQ.getFormName().equalsIgnoreCase(generateRfqPoRequest.getModuleName()) && StringUtils.isEmpty(rfqPoRequest.getBidType())) { 
				throw new CustomBadRequestException("Bid Type should not be empty.");
			}
			if (FormNames.PO.getFormName().equalsIgnoreCase(generateRfqPoRequest.getModuleName()) && rfqPoRequest.getSupplierId() == null) { 
				throw new CustomBadRequestException("Supplier should not be empty.");
			}
			if (rfqPoRequest.getTransactionalDate() == null) { 
				throw new CustomBadRequestException("Transactional Date should not be empty.");
			}
		}
	}

	@Override
	public List<PurchaseOrder> generatePoFromQa(QuotationAnalysis quotationAnalysis) {
		List<PurchaseOrder> purchaseOrders = new ArrayList<PurchaseOrder>();
		PurchaseOrder purchaseOrder = null;
		
		log.info("QA is going to save from PO.");
		try {
			quotationAnalysis = this.quotationAnalysisService.save(quotationAnalysis);
			log.info("QA is saved from PO.");
		} catch (Exception e) {
			log.error("Exception while saving the QA from PO.");
			e.printStackTrace();
			throw new CustomException("Exception while saving the QA from PO.");
		}
		
		List<QuotationAnalysisItem> qaItems = quotationAnalysis.getQuotationAnalysisItems();
		Map<String, String> groupedPr = new HashMap<String, String>();
		
		Long subsidiaryId = quotationAnalysis.getSubsidiaryId();
		
		try {
			if (CollectionUtils.isNotEmpty(qaItems)) {
				// create grouping as per criteria
				for (QuotationAnalysisItem qaItem : qaItems) {
					// if awarded and Processed PO is true then only we are creating the PO against the record
					if (qaItem.isProcessedPo() && qaItem.isAwarded() && StringUtils.isEmpty(qaItem.getPoRef())) {
						String keyName = qaItem.getPrLocationId() + "_" + qaItem.getCurrency() + "_" + qaItem.getApprovedSupplier();
						String qaItemsId = null;
						if (groupedPr.containsKey(keyName)) {
							qaItemsId = groupedPr.get(keyName) + "|" + qaItem.getId();
						} else {
							qaItemsId = String.valueOf(qaItem.getId());					
						}
						groupedPr.put(keyName, qaItemsId);
					}
				}
				
				// iterate over the grouped pr which will belongs to common PO
				for (Map.Entry<String, String> entry : groupedPr.entrySet()) {
					// fetch the data only for the first PR. which is common for other which is in same PO
					String[] qaItemsId = entry.getValue().split("\\|");
					
					purchaseOrder = new PurchaseOrder();
					List<PurchaseOrderItem> poItems = new ArrayList<PurchaseOrderItem>();
					
//					String transactionalDate = CommonUtils.convertDateToFormattedString(new Date());
//					String poNumber = this.setupServiceClient.getDocumentSequenceNumber(transactionalDate, subsidiaryId, FormNames.PO.getFormName(), false);
					
					// Prepare Data for the PO
//					purchaseOrder.setPoNumber(poNumber);
					purchaseOrder.setSubsidiaryId(subsidiaryId);
					purchaseOrder.setPoType(PoType.QA_BASED.getPoType());
					purchaseOrder.setQaId(quotationAnalysis.getId());
					purchaseOrder.setPoDate(new Date());
					purchaseOrder.setPaymentTerm(PaymentTerm.DAYS_30.getPaymentTerm());
					purchaseOrder.setMatchType(MatchType.WAY_3.getMatchType());
					purchaseOrder.setPoStatus(TransactionStatus.OPEN.getTransactionStatus());
					// Memo, NetsuiteId not getting from UI
					
					// To store the Common value form the list
					Long locationId = null;
					String location = null;
					Set<String> prIds = new TreeSet<String>();
					Long supplierId = null;
					String currency = null;
					Double exchangeRate = null;
					
					// Iterate over the QA Items
					if (CollectionUtils.isNotEmpty(qaItems)) {
						Double amount = 0.0;
						for (int i=0; i < qaItemsId.length; i++) {
							String qaId = qaItemsId[i];
							if (StringUtils.isNotEmpty(qaId)) {
								Optional<QuotationAnalysisItem> quotationAnalysisItem = qaItems.stream().filter(q -> q.getId() == Long.parseLong(qaId)).findFirst();
								
								if (quotationAnalysisItem.isPresent()) {
									QuotationAnalysisItem qaItem = quotationAnalysisItem.get();
									PurchaseOrderItem poItem = new PurchaseOrderItem();
									poItem.setItemId(qaItem.getItemId());
									if (qaItem.getQuantity() != null) {
										poItem.setQuantity(Double.valueOf(qaItem.getQuantity()));
									}
									poItem.setRate(qaItem.getRatePerUnit());
									poItem.setAmount(qaItem.getActualRate());
									amount += qaItem.getActualRate();
									poItem.setPrId(qaItem.getPrId());
									prIds.add(String.valueOf(qaItem.getPrId()));
									poItem.setShipToLocationId(qaItem.getPrLocationId());
									poItem.setShipToLocation(qaItem.getPrLocation());
									poItem.setReceivedByDate(CommonUtils.convertOffsetDateToDate(qaItem.getRecievedDate()));
			
									poItems.add(poItem);
									purchaseOrder.setPurchaseOrderItems(poItems);
									
									locationId = qaItem.getPrLocationId();
									location = qaItem.getPrLocation();
									supplierId = qaItem.getApprovedSupplier();
									currency = qaItem.getCurrency();
									exchangeRate = qaItem.getExchangeRate();
								}							
							}
						}
						
						purchaseOrder.setLocationId(locationId);
						purchaseOrder.setLocation(location);
						if (prIds != null) purchaseOrder.setPrId(String.join("|", prIds));
						purchaseOrder.setSupplierId(supplierId);
						purchaseOrder.setCurrency(currency);
						purchaseOrder.setExchangeRate(exchangeRate);
						purchaseOrder.setAmount(amount);
						purchaseOrder.setTotalAmount(amount);
						
						purchaseOrders.add(purchaseOrder);
					}
				}
				log.info("PO Object is prepared successfully from the QA.");
				for (PurchaseOrder po : purchaseOrders) {
					po = this.save(po);				
				}
				log.info("PO Object is Saved successfully from the QA.");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Error while creating PO from QA");
			throw new CustomException("Error while creating PO from QA");
		}	
		return purchaseOrders;
	}
	
	@Override
	public List<PurchaseOrder> getPoApproval(String userId) {
		List<PurchaseOrder> puchaseOrders = new ArrayList<PurchaseOrder>();
		List<String> status = new ArrayList<String>();
		status.add(TransactionStatus.PENDING_APPROVAL.getTransactionStatus());
		status.add(TransactionStatus.PARTIALLY_APPROVED.getTransactionStatus());
		puchaseOrders = this.purchaseOrderRepository.findAllByPoStatus(status, userId);
		log.info("purchase order are for approval process " + puchaseOrders);
		for (PurchaseOrder purchaseOrder : puchaseOrders) {
			Double totalAmount = 0.0;
			totalAmount = this.purchaseOrderItemRepository.findByPoIdForApproval(purchaseOrder.getId());
			log.info("Total amount for po " + totalAmount);
			purchaseOrder.setTotalValue(totalAmount);
		}
		
		return puchaseOrders;
	}

	@Override
	public List<PurchaseOrder> findByLocation(Long locationId, Long subsidiaryId, List<String> poStatus) {
		List<PurchaseOrder> poNumbers = new ArrayList<PurchaseOrder>();
		poNumbers = this.purchaseOrderRepository
				.getAllPoByLocationIdAndSubsidiaryIdAndPoStatusAndIsDeleted(locationId, subsidiaryId, poStatus, false);
		log.info("Get all purchase order number by subsidary id, location id and status ." + poNumbers);
		return poNumbers;
	}

	@Override
	public List<PurchaseOrder> findSupplierAndCurrencyByPoId(Long poId) {
		List<PurchaseOrder> supplierAndCurrencyByPo = new ArrayList<PurchaseOrder>();
		supplierAndCurrencyByPo = this.purchaseOrderRepository.getAllSupplierAndCurrencyByPoAndIsDeleted(poId, false);
		log.info("Get all supplier and currecny by purchase order number ." + supplierAndCurrencyByPo);
		return supplierAndCurrencyByPo;
	}

	@Override
	public List<PurchaseOrderItem> findByPoIdForItem(Long poId, String itemNature) {
		List<PurchaseOrderItem> itemyByPo = new ArrayList<PurchaseOrderItem>();
		itemyByPo = this.purchaseOrderItemRepository.getAllItemByPoAndIsDeleted(poId, itemNature, false);
		log.info("Get all purchase order items by po number ." + itemyByPo);
		return itemyByPo;
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
			Optional<PurchaseOrder> oldPurchaseOrder = this.findOldDeepCopiedPO(id);

			Optional<PurchaseOrder> purchaseOrder = Optional.empty();
			purchaseOrder = this.findById(id);

			/**
			 * Check routing is active or not
			 */
			boolean isRoutingActive = purchaseOrder.get().isApprovalRoutingActive();
			if (!isRoutingActive) {
				log.error("Routing is not active for the Purchase Order : " + id + ". Please update your configuration. ");
				throw new CustomMessageException("Routing is not active for the Purchase Order : " + id + ". Please update your configuration. ");
			}
			
			Double transactionalAmount = this.purchaseOrderItemRepository.findTotalEstimatedAmountForPo(id);
			log.info("Total estimated transaction amount for PO is :: " + transactionalAmount);
			
			// if amount is null then throw error
			if (transactionalAmount == null || transactionalAmount == 0.0) {
				log.error("There is no available Approval Process for this transaction.");
				throw new CustomMessageException("There is no available Approval Process for this transaction.");
			}
			
			ApprovalRequest approvalRequest = new ApprovalRequest();
			approvalRequest.setSubsidiaryId(purchaseOrder.get().getSubsidiaryId());
			approvalRequest.setFormName(FormNames.PO.getFormName());
			approvalRequest.setTransactionAmount(transactionalAmount);
			approvalRequest.setLocationId(purchaseOrder.get().getLocationId());
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
			
			purchaseOrder.get().setApproverSequenceId(sequenceId);
			purchaseOrder.get().setApproverMaxLevel(level);
			purchaseOrder.get().setApproverPreferenceId(approverPreferenceId);
			
			String levelToFindRole = "L1";
			if (AppConstants.APPROVAL_TYPE_INDIVIDUAL.equals(approvalPreference.getApprovalType())) {
				levelToFindRole = level;
			}
			approvalRequest = this.masterServiceClient.findApproverByLevelAndSequence(approverPreferenceId, levelToFindRole, sequenceId);

			this.updateApproverDetailsInPo(purchaseOrder, approvalRequest);
			purchaseOrder.get().setPoStatus(TransactionStatus.PENDING_APPROVAL.getTransactionStatus());
			log.info("Approver is found and details is updated for PO :: " + purchaseOrder.get());
			
			this.savePoForApproval(purchaseOrder.get(), oldPurchaseOrder);
			log.info("PO is saved successfully with Approver details.");

			this.masterServiceClient.sendEmailByApproverId(purchaseOrder.get().getNextApprover(), FormNames.PO.getFormName());
			
			isSentForApproval = true;
		} catch (Exception e) {
			log.error("Error while sending PR for approval for id - " + id);
			e.printStackTrace();
			throw new CustomMessageException("Error while sending PO for approval for id - " + id + ", Message : " + e.getLocalizedMessage());
		}
		
		return isSentForApproval;
	}
	
	/**
	 * Save PO after the approval details change
	 * @param purchaseOrder
	 */
	private void savePoForApproval(PurchaseOrder purchaseOrder, Optional<PurchaseOrder> oldPurchaseOrder) {
		purchaseOrder.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		purchaseOrder = this.purchaseOrderRepository.save(purchaseOrder);
		
		if (purchaseOrder == null) {
			log.info("Error while saving the Purchase Order after the Approval.");
			throw new CustomMessageException("Error while saving the Purchase Order after the Approval.");
		}
		log.info("PO saved successfully :: " + purchaseOrder.getPoNumber());
		
		// update the data in PR history table
		this.updatePurchaseOrderHistory(purchaseOrder, oldPurchaseOrder);
		log.info("PO history is updated");		
	}

	/**
	 * Set/Prepares the approver details in the PO object
	 * 
	 * @param purchaseRequisition
	 * @param approvalRequest
	 */
	private void updateApproverDetailsInPo(Optional<PurchaseOrder> purchaseOrder, ApprovalRequest approvalRequest) {
		purchaseOrder.get().setApprovedBy(purchaseOrder.get().getNextApprover());
		purchaseOrder.get().setNextApprover(approvalRequest.getNextApprover());
		purchaseOrder.get().setNextApproverRole(approvalRequest.getNextApproverRole());
		purchaseOrder.get().setNextApproverLevel(approvalRequest.getNextApproverLevel());
	}

	private Optional<PurchaseOrder> findOldDeepCopiedPO(Long id) {
		Optional<PurchaseOrder> oldPurchaseOrder = this.purchaseOrderRepository.findByIdAndIsDeleted(id, false);
		if (oldPurchaseOrder.isPresent()) {
			try {
				oldPurchaseOrder = Optional.ofNullable((PurchaseOrder) oldPurchaseOrder.get().clone());
				log.info("Existing PO is copied.");
			} catch (CloneNotSupportedException e) {
				log.error("Error while Cloning the object. Please contact administrator.");
				throw new CustomException("Error while Cloning the object. Please contact administrator.");
			}
		}
		return oldPurchaseOrder;
	}
	
	/**
	 * Find PO by it's ID
	 * @param id
	 * @return
	 */
	public Optional<PurchaseOrder> findById(Long id) {
		Optional<PurchaseOrder> purchaseOrder = Optional.empty();
		purchaseOrder = this.purchaseOrderRepository.findByIdAndIsDeleted(id, false);

		if (!purchaseOrder.isPresent()) {
			log.info("PO is not found against the provided PO-ID :: " + id);
			throw new CustomMessageException("PO is not found against the provided PO-ID :: " + id);
		}
		purchaseOrder.get().setApprovalRoutingActive(this.findIsApprovalRoutingActive(purchaseOrder.get().getSubsidiaryId()));
		
		log.info("PO is found against the PO-ID :: " + id);
		return purchaseOrder;
	}

	@Override
	public Boolean approveAllPos(List<Long> poIds) {
		Boolean isAllPoApproved = false;
		try {
			for (Long poId : poIds) {
				log.info("Approval Process is started for po-id :: " + poId);

				/**
				 * Due to single transaction we are getting updated value when we find from repo after the update
				 * hence finding old one first
				 */
				// Get the existing object using the deep copy
				Optional<PurchaseOrder> oldPurchaseOrder = this.findOldDeepCopiedPO(poId);

				Optional<PurchaseOrder> purchaseOrder = Optional.empty();
				purchaseOrder = this.findById(poId);

				/**
				 * Check routing is active or not
				 */
				boolean isRoutingActive = purchaseOrder.get().isApprovalRoutingActive();
				if (!isRoutingActive) {
					log.error("Routing is not active for the Purchase Order : " + poId + ". Please update your configuration. ");
					throw new CustomMessageException("Routing is not active for the Purchase Order : " + poId + ". Please update your configuration. ");
				}
				
				// meta data
				Long approvalPreferenceId = purchaseOrder.get().getApproverPreferenceId();
				Long sequenceId = purchaseOrder.get().getApproverSequenceId();
				String maxLevel = purchaseOrder.get().getApproverMaxLevel();
				
				ApprovalRequest approvalRequest = new ApprovalRequest();
				
				if (!maxLevel.equals(purchaseOrder.get().getNextApproverLevel())) {
					Long currentLevelNumber = Long.parseLong(purchaseOrder.get().getNextApproverLevel().replaceFirst("L", "")) + 1;
					String currentLevel = "L" + currentLevelNumber;
					approvalRequest = this.masterServiceClient.findApproverByLevelAndSequence(approvalPreferenceId, currentLevel, sequenceId);
					purchaseOrder.get().setPoStatus(TransactionStatus.PARTIALLY_APPROVED.getTransactionStatus());
				} else {
					purchaseOrder.get().setPoStatus(TransactionStatus.APPROVED.getTransactionStatus());
				}
				log.info("Approval Request is found for PO :: " + approvalRequest.toString());

				this.updateApproverDetailsInPo(purchaseOrder, approvalRequest);
				log.info("Approver is found and details is updated :: " + purchaseOrder.get());
				
				this.savePoForApproval(purchaseOrder.get(), oldPurchaseOrder);
				log.info("PO is saved successfully with Approver details.");

				masterServiceClient.sendEmailByApproverId(purchaseOrder.get().getNextApprover(), FormNames.PO.getFormName());
				log.info("Approval Process is Finished for po :: " + purchaseOrder.get().getId());
			}
			
			isAllPoApproved = true;
		} catch (Exception e) {
			log.error("Error while approving the PO.");
			e.printStackTrace();
			throw new CustomMessageException("Error while approving the PO. Message : " + e.getLocalizedMessage());
		}
		return isAllPoApproved;
	}
	
	private boolean findIsApprovalRoutingActive(Long subsidiaryId) {
		return this.masterServiceClient.findIsApprovalRoutingActive(subsidiaryId, FormNames.PO.getFormName());
	}

	@Override
	public PurchaseOrder getByPoId(Long poId) {

		PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdAndIsDeleted(poId, false).get();		
		List<PurchaseOrderItem> purchaseOrderItems = purchaseOrderItemRepository.findByPoIdAndIsDeleted(poId, false);
		purchaseOrderItems.forEach(c -> {
			Item item = this.masterServiceClient.findByItemId(c.getItemId());
			c.setItemName(item.getName());
		});
		purchaseOrder.setPurchaseOrderItems(purchaseOrderItems);
	/*	List<Grn> grns = grnRepository.findByPoIdAndIsDeleted(poId, false);
		purchaseOrder.setGrns(grns);*/

		return purchaseOrder;
	}

	@Override
	public PurchaseOrderItem getByPoItemId(Long poId, Long itemId) {

		PurchaseOrderItem purchaseOrderItem = purchaseOrderItemRepository.findByPoIdAndItemIdAndIsDeleted(poId, itemId, false);
		Item item = this.masterServiceClient.findByItemId(purchaseOrderItem.getItemId());
		purchaseOrderItem.setItemDescription(item.getDescription());
		purchaseOrderItem.setItemUom(item.getUom());
		
		return purchaseOrderItem;
	}

	@Override
	public List<PurchaseOrder> getBySupplierSubsidiary(Long supplierId, Long subsidiaryId) {
		List<PurchaseOrder> purchaseOrders;
		log.info("Get POs by Supp, Sub started.");
		purchaseOrders = purchaseOrderRepository.findBySupplierIdAndSubsidiaryIdAndIsDeletedAndPoStatus(supplierId, subsidiaryId, false, "Approved");
		log.info("Get POs by Supp, Sub: "+purchaseOrders);
		return purchaseOrders;
	}

	@Override
	public String findPoItemsByQaAndItem(Long qaId, Long itemId) {
		List<PurchaseOrderItem> items = this.purchaseOrderItemRepository.findItemByQaIdAndItem(qaId, itemId);
		String message = null;
		if (CollectionUtils.isNotEmpty(items)) {
			PurchaseOrderItem poItem = items.get(0);
			message = "This item already added to another PO : " + poItem.getPoNumber() + ".";
		}
		return message;
	}

	@Override
	public byte[] upload(MultipartFile file) {
		try {
			return this.importPoFromExcel(file);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("Something went wrong. Please Contact Administrator. Error : " + e.getLocalizedMessage());
		}
	}
	
	public byte[] importPoFromExcel(MultipartFile inputFile) {
		try {
			InputStream inputStream = inputFile.getInputStream();
			@SuppressWarnings("resource")
			Workbook workbook = new XSSFWorkbook(inputStream);
			Sheet sheet = workbook.getSheet("Purchase Order");
			Iterator<Row> rows = sheet.iterator();

			int statusColumnNumber = 0;
			int rowNumber = 0;

			boolean isError = false;
			StringBuilder errorMessage = new StringBuilder();

			Map<String, PurchaseOrder> poMapping = new TreeMap<String, PurchaseOrder>();

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

				PurchaseOrder po = new PurchaseOrder();
				String externalId = null;
				// External ID - REQUIRED
				try {
					if (inputCurrentRow.getCell(0) != null) {
//						externalId = inputCurrentRow.getCell(0).getStringCellValue();
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
				// ----------------- supplier header fields STARTED -----------------------
				if (poMapping.containsKey(externalId)) {
					po = poMapping.get(externalId);
				}
				po.setExternalId(externalId);
				
				try {
					if (inputCurrentRow.getCell(1) != null) {
						String subsidiaryName = inputCurrentRow.getCell(1).getStringCellValue();
						po.setSubsidiaryName(subsidiaryName);
						Long subsidiaryId = this.setupServiceClient.getSubsidiaryIdByName(subsidiaryName);
						if (subsidiaryId == null) {
							errorMessage.append(errorCount + ") Subsidiary : " + subsidiaryName + " is not found Please enter the valid Subsidiary Name. ");
							log.error("Subsidiary : " + subsidiaryName + " is not found. Please enter the valid Subsidiary Name. ");
							isError = true;
							errorCount++;
						}
						po.setSubsidiaryId(subsidiaryId);
					} else {
						errorMessage.append(errorCount + ") Subsidiary is required. ");
						log.error("Subsidiary is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception subsidiary " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Subsidiary Name is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Vendor Name - REQUIRED
				Long supplierId = null;
				try {
					if (inputCurrentRow.getCell(2) != null) {
						String vendorName = inputCurrentRow.getCell(2).getStringCellValue();
						po.setSupplierName(vendorName);
						Supplier supplier = this.masterServiceClient.findBySupplierName(vendorName);
						if (supplier == null) {
							errorMessage.append(errorCount + ") Vendor Name is not exist. ");
							log.error("Vendor Name is not exist.");
							isError = true;
							errorCount++;
						} else {
							supplierId = supplier.getId();
							po.setSupplierId(supplierId);
						}
					} else {
						errorMessage.append(errorCount + ") Vendor Name is required. ");
						log.error("Vendor Name is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Vendor Name " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Vendor Name is invalid.");
					isError = true;
					errorCount++;
				}

				// PO Date
				try {
					if (inputCurrentRow.getCell(3) != null) {
						po.setPoDate(inputCurrentRow.getCell(3).getDateCellValue());
					} else {
						errorMessage.append(errorCount + ") PO Date is required. ");
						log.error("PO Date is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception PO Date " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of PO Date is invalid.");
					isError = true;
					errorCount++;
				}

				// PO Type - REQUIRED
				String poType = null;
				try {
					if (inputCurrentRow.getCell(4) != null) {
						poType = inputCurrentRow.getCell(4).getStringCellValue();
						if (PoType.findByAbbr(poType) == null) {
							errorMessage.append(errorCount + ") PO Type is Invalid.");
							log.error("PO Type is Invalid.");
							isError = true;
							errorCount++;
						} else po.setPoType(poType);
					} else {
						errorMessage.append(errorCount + ") PO Type is required.");
						log.error("PO Type is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Vendor Type " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of PO Type is invalid.");
					isError = true;
					errorCount++;
				}

				// TODO Compute pr Id for the pr number
				// PR Numbers
				String prNumber = null;
				try {
					if (inputCurrentRow.getCell(5) != null) {
						prNumber = inputCurrentRow.getCell(5).getStringCellValue();
						if (PoType.PR_BASED.toString().equalsIgnoreCase(poType)) {
							if (StringUtils.isNotEmpty(prNumber)) {
								String prIds = po.getPrId();
								if (StringUtils.isNotEmpty(prIds)) {
									// // TODO validate the existence of PR
									prIds += "|" + prNumber;
								} else {
									prIds = prNumber;
								}
								po.setPrId(prIds);	
							} else {
								errorMessage.append(errorCount + ") PR Number is Required.");
								log.error("PR Number is Required.");
								isError = true;
								errorCount++;
							}
						}
					}
				} catch (Exception e) {
					log.error("Exception PR Number " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of PR Number is invalid.");
					isError = true;
					errorCount++;
				}
				
				// QA Number
				String qaNumber = null;
				try {
					if (inputCurrentRow.getCell(6) != null) {
						qaNumber = inputCurrentRow.getCell(6).getStringCellValue();
						if (PoType.QA_BASED.toString().equalsIgnoreCase(poType)) {
							if (StringUtils.isNotEmpty(qaNumber)) {
								Optional<QuotationAnalysis> qa = this.quotationAnalysisRepository.findByQaNumberAndIsDeleted(qaNumber, false);
								if (!qa.isPresent()) {
									errorMessage.append(errorCount + ") QA Number is not exist.");
									log.error("QA Number is not exist.");
									isError = true;
									errorCount++;
								}
								po.setQaId(qa.get().getId());							
							} else {
								errorMessage.append(errorCount + ") QA Number is Required.");
								log.error("QA Number is Required.");
								isError = true;
								errorCount++;
							}
						}
					}
				} catch (Exception e) {
					log.error("Exception QA Number " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of QA Number is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Ship to location
				try {
					if (inputCurrentRow.getCell(7) != null) {
						String location = inputCurrentRow.getCell(7).getStringCellValue();
						po.setLocationName(location);
					}
				} catch (Exception e) {
					log.error("Exception Location Name " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Location Name is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Payment Term
				try {
					if (inputCurrentRow.getCell(8) != null) {
						String paymentTerm = inputCurrentRow.getCell(8).getStringCellValue();
						if (PaymentTerm.findByAbbr(paymentTerm) == null) {
							errorMessage.append(errorCount + ") Value of Payment Term is invalid.");
							isError = true;
							errorCount++;
						}
						po.setPaymentTerm(paymentTerm);
					}
				} catch (Exception e) {
					log.error("Exception Payment Term " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Payment Term is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Match Type
				try {
					if (inputCurrentRow.getCell(9) != null) {
						String matchType = inputCurrentRow.getCell(9).getStringCellValue();
						if (MatchType.findByAbbr(matchType) == null) {
							errorMessage.append(errorCount + ") Value of Match Type is invalid.");
							isError = true;
							errorCount++;
						}
						po.setPaymentTerm(matchType);
					}
				} catch (Exception e) {
					log.error("Exception Match Type " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Match Type is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Exchange Rate
				try {
					if (inputCurrentRow.getCell(11) != null) {
						double exchangeRate = inputCurrentRow.getCell(11).getNumericCellValue();
						po.setExchangeRate(exchangeRate);
					}
				} catch (Exception e) {
					log.error("Exception Exchange Rate " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Exchange Rate is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Memo
				try {
					if (inputCurrentRow.getCell(12) != null) {
						String memo = inputCurrentRow.getCell(12).getStringCellValue();
						po.setMemo(memo);
					}
				} catch (Exception e) {
					log.error("Exception Memo " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Memo is invalid.");
					isError = true;
					errorCount++;
				}

				// ----------- PO Item started -----------------------
				PurchaseOrderItem poItem = new PurchaseOrderItem();
				
				// Item code - REQUIRED
				try {
					if (inputCurrentRow.getCell(13) != null) {
						String itemName = inputCurrentRow.getCell(13).getStringCellValue();
						poItem.setItemName(itemName);
						
						Item item = this.masterServiceClient.findByName(itemName);
						if (item != null) {
							poItem.setItemId(item.getId());
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
					if (inputCurrentRow.getCell(14) != null) {
						double quantity = inputCurrentRow.getCell(14).getNumericCellValue();
						poItem.setQuantity(quantity);
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
					if (inputCurrentRow.getCell(15) != null) {
						double rate = inputCurrentRow.getCell(15).getNumericCellValue();
						poItem.setRate(rate);
					} else {
						errorMessage.append(errorCount + ") Item rate is required.");
						log.error("Item rate is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception rate " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of rate is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Tax Group
				try {
					if (inputCurrentRow.getCell(16) != null) {
						String taxGroupName = inputCurrentRow.getCell(16).getStringCellValue();
						
						TaxGroup taxGroup = this.setupServiceClient.findByTaxGroupName(taxGroupName);
						if (taxGroup != null) {
							poItem.setTaxGroupId(taxGroup.getId());
						} else {
							errorMessage.append(errorCount + ") Tax Group is not exist.");
							log.error("Tax Group is not exist");
							isError = true;
							errorCount++;
						}
					} else {
						errorMessage.append(errorCount + ") Tax Group is required.");
						log.error("Tax Group is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Tax Group " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Tax Group is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Received By Date
				try {
					if (inputCurrentRow.getCell(17) != null) {
						poItem.setReceivedByDate(inputCurrentRow.getCell(17).getDateCellValue());
					} else {
						errorMessage.append(errorCount + ") PO Date is required. ");
						log.error("PO Date is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception PO Date " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Received By Date is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Memo
				try {
					if (inputCurrentRow.getCell(18) != null) {
						String memo = inputCurrentRow.getCell(18).getStringCellValue();
						poItem.setMemo(memo);
					}
				} catch (Exception e) {
					log.error("Exception PO Item Memo " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of PO Item Memo is invalid.");
					isError = true;
					errorCount++;
				}
				
				List<PurchaseOrderItem> poItems = new ArrayList<PurchaseOrderItem>();
				poItems = po.getPurchaseOrderItems();
				if (CollectionUtils.isEmpty(poItems))
					poItems = new ArrayList<PurchaseOrderItem>();

				poItems.add(poItem );
				poItems = poItems.stream().distinct().collect(Collectors.toList());
				
				po.setPurchaseOrderItems(poItems);
				// ----------- PO Item Finished -----------------------
				
				// Billing Address
				try {
					if (inputCurrentRow.getCell(19) != null) {
						String billingAddressCode = inputCurrentRow.getCell(19).getStringCellValue();
						
						List<SupplierAddress> addresses = this.masterServiceClient.findAddressBySupplierIdAndAddressCode(supplierId, billingAddressCode);
						if (CollectionUtils.isEmpty(addresses)) {
							errorMessage.append(errorCount + ") Billing Address Code is not exist. ");
							log.error("Billing Address Code is not exist.");
							isError = true;
							errorCount++;
						} else {
							Long billTo = addresses.get(0).getId();
							po.setBillTo(billTo);
						}
					} else {
						errorMessage.append(errorCount + ") Billing Address Code is required. ");
						log.error("Billing Address Code is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Billing Address Code " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Billing Address Code is invalid.");
					isError = true;
					errorCount++;
				}
				
				// Shipping Address
				try {
					if (inputCurrentRow.getCell(20) != null) {
						String shippingAddressCode = inputCurrentRow.getCell(20).getStringCellValue();
						
						List<SupplierAddress> addresses = this.masterServiceClient.findAddressBySupplierIdAndAddressCode(supplierId, shippingAddressCode);
						if (CollectionUtils.isEmpty(addresses)) {
							errorMessage.append(errorCount + ") Shipping Address Code is not exist. ");
							log.error("Shipping Address Code is not exist.");
							isError = true;
							errorCount++;
						} else {
							Long shipTo = addresses.get(0).getId();
							po.setShipTo(shipTo);
						}
					} else {
						errorMessage.append(errorCount + ") Shipping Address Code is required. ");
						log.error("Shipping Address Code is required.");
						isError = true;
						errorCount++;
					}
				} catch (Exception e) {
					log.error("Exception Shipping Address Code " + e.getLocalizedMessage());
					errorMessage.append(errorCount + ") Value of Shipping Address Code is invalid.");
					isError = true;
					errorCount++;
				}
				
				// ADDED IN MAP
				poMapping.put(externalId, po);
				// -------------- SUPPLIER ACCOUNTING IS FINISHED -------------------------------
				Cell cell = inputCurrentRow.createCell(statusColumnNumber);
				if (isError) {
					cell.setCellValue(errorMessage.toString());
					po.setHasError(true);
					continue;
				} else {
					cell.setCellValue("Imported");
				}
			}
			
			for (Map.Entry<String, PurchaseOrder> map : poMapping.entrySet()) {
			    log.info(map.getKey() + " ==== >>> " + map.getValue());
			    PurchaseOrder purchaseOrder = map.getValue();
			    if (purchaseOrder != null && !purchaseOrder.isHasError()) {
			    	Double totalAmount = purchaseOrder.getPurchaseOrderItems().stream().filter(o -> o.getAmount() > 0)
			    			.mapToDouble(o -> o.getAmount()).sum();
			    	purchaseOrder.setAmount(totalAmount);
			    	purchaseOrder.setTotalAmount(totalAmount);
					this.save(purchaseOrder);
					
					for (PurchaseOrderItem poItem : purchaseOrder.getPurchaseOrderItems()) {
						poItem.setAmount(poItem.getQuantity() * poItem.getRate());
					}
					
					log.info("PO is saved.");
			    }
			}
//
			FileOutputStream out = null;

			File outputFile = new File("po_export.xlsx");
			try {
				// Writing the workbook
				out = new FileOutputStream(outputFile);
				workbook.write(out);
				log.info("supplier_export.xlsx written successfully on disk.");
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
	public Boolean rejectAllPos(List<PurchaseOrder> pos) {
		for (PurchaseOrder po : pos) {
			String rejectComments = po.getRejectedComments();
			
			if (StringUtils.isEmpty(rejectComments)) {
				log.error("Reject Comments is required.");
				throw new CustomException("Reject Comments is required. It is missing for PO : " + po.getId());
			}
			
			Optional<PurchaseOrder> oldPurchaseOrder = this.findOldDeepCopiedPO(po.getId());

			Optional<PurchaseOrder> purchaseOrder = this.purchaseOrderRepository.findByIdAndIsDeleted(po.getId(), false);
			purchaseOrder.get().setPoStatus(TransactionStatus.REJECTED.getTransactionStatus());
			purchaseOrder.get().setRejectedComments(rejectComments);
			purchaseOrder.get().setApprovedBy(null);
			purchaseOrder.get().setNextApprover(null);
			purchaseOrder.get().setNextApproverRole(null);
			purchaseOrder.get().setNextApproverLevel(null);
			purchaseOrder.get().setApproverSequenceId(null);
			purchaseOrder.get().setApproverMaxLevel(null);
			purchaseOrder.get().setApproverPreferenceId(null);
			purchaseOrder.get().setNoteToApprover(null);
			
			log.info("Approval Fields are restored to empty. For Purchase Order : " + po);
			
			this.savePoForApproval(purchaseOrder.get(), oldPurchaseOrder);
			log.info("Purchase Order is saved successfully with restored Approver details.");

			log.info("Approval Process is Finished for PO-id :: " + po.getId());
		}
		return true;
	}

	@Override
	public Boolean updateNextApprover(Long approverId, Long poId) {
		Optional<PurchaseOrder> purchaseOrder = this.purchaseOrderRepository.findByIdAndIsDeleted(poId, false);
		
		if (!purchaseOrder.isPresent()) {
			log.error("PurchaseOrder Not Found against given PurchaseOrder id : " + poId);
			throw new CustomMessageException("Purchase Order Not Found against given Purchase Order id : " + poId);
		}
		purchaseOrder.get().setNextApprover(String.valueOf(approverId));
		purchaseOrder.get().setLastModifiedBy(CommonUtils.getLoggedInUsername());
		this.purchaseOrderRepository.save(purchaseOrder.get());
		
		return true;
	}

	@Override
	public List<PurchaseOrder> generateMultiplePoFromQa(QuotationAnalysis quotationAnalysis) {
		List<PurchaseOrder> purchaseOrders = new ArrayList<PurchaseOrder>();
		PurchaseOrder purchaseOrder = null;
		
		log.info("QA is going to save from PO.");
		try {
			quotationAnalysis = this.quotationAnalysisService.save(quotationAnalysis);
			log.info("QA is saved from PO.");
		} catch (Exception e) {
			log.error("Exception while saving the QA from PO.");
			e.printStackTrace();
			throw new CustomException("Exception while saving the QA from PO.");
		}
		
		List<QuotationAnalysisItem> qaItems = quotationAnalysis.getQuotationAnalysisItems();
		
		Long subsidiaryId = quotationAnalysis.getSubsidiaryId();
		
		try {
			if (CollectionUtils.isNotEmpty(qaItems)) {
				// create grouping as per criteria
				for (QuotationAnalysisItem qaItem : qaItems) {
					// if awarded and Processed PO is true then only we are creating the PO against the record
					if (qaItem.isProcessedPo() && qaItem.isAwarded()) {
						purchaseOrder = new PurchaseOrder();
						List<PurchaseOrderItem> poItems = new ArrayList<PurchaseOrderItem>();
						
						String transactionalDate = CommonUtils.convertDateToFormattedString(new Date());
						String poNumber = this.setupServiceClient.getDocumentSequenceNumber(transactionalDate, subsidiaryId, FormNames.PO.getFormName(), false);
						
						// Prepare Data for the PO
						purchaseOrder.setPoNumber(poNumber);
						purchaseOrder.setSubsidiaryId(subsidiaryId);
						purchaseOrder.setPoType(PoType.PR_BASED.getPoType());
						purchaseOrder.setQaId(quotationAnalysis.getId());
						purchaseOrder.setPoDate(new Date());
						purchaseOrder.setPaymentTerm(PaymentTerm.DAYS_30.getPaymentTerm());
						purchaseOrder.setMatchType(MatchType.WAY_3.getMatchType());
						purchaseOrder.setPoStatus(TransactionStatus.OPEN.getTransactionStatus());
						purchaseOrder.setLocationId(qaItem.getPrLocationId());
						purchaseOrder.setLocation(qaItem.getPrLocation());
						purchaseOrder.setPrId(String.valueOf(qaItem.getPrId()));
						purchaseOrder.setSupplierId(qaItem.getApprovedSupplier());
						purchaseOrder.setCurrency(qaItem.getCurrency());
						purchaseOrder.setExchangeRate(qaItem.getExchangeRate());
						
						// Memo, NetsuiteId not getting from UI
						PurchaseOrderItem poItem = new PurchaseOrderItem();
						poItem.setItemId(qaItem.getItemId());
						if (qaItem.getQuantity() != null) {
							poItem.setQuantity(Double.valueOf(qaItem.getQuantity()));
							poItem.setRemainQuantity(poItem.getQuantity());
						}
						poItem.setRate(qaItem.getRatePerUnit());
						poItem.setAmount(qaItem.getActualRate());
						poItem.setPrId(qaItem.getPrId());
						poItem.setShipToLocationId(qaItem.getPrLocationId());
						poItem.setShipToLocation(qaItem.getPrLocation());

						poItems.add(poItem);
						purchaseOrder.setPurchaseOrderItems(poItems);

						purchaseOrders.add(purchaseOrder);
					}
				}
					
				log.info("PO Object is prepared successfully from the QA.");
				for (PurchaseOrder po : purchaseOrders) {
					po = this.save(po);				
				}
				log.info("PO Object is Saved successfully from the QA.");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Error while creating PO from QA");
			throw new CustomException("Error while creating PO from QA");
		}	
		return purchaseOrders;
	}
	
	@Override
	public byte[] downloadTemplate() {
		DefaultResourceLoader loader = new DefaultResourceLoader();
		try {
			File is = loader.getResource("classpath:/templates/po_template.xlsx").getFile();
			return Files.readAllBytes(is.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public Boolean selfApprove(Long poId) {
		Optional<PurchaseOrder> purchaseOrder = this.purchaseOrderRepository.findByIdAndIsDeleted(poId, false);
		
		if (!purchaseOrder.isPresent()) {
			log.error("PurchaseOrder Not Found against given PurchaseOrder id : " + poId);
			throw new CustomMessageException("PurchaseOrder Not Found against given PurchaseOrder id : " + poId);
		}
		purchaseOrder.get().setPoStatus(TransactionStatus.APPROVED.getTransactionStatus());
		purchaseOrder.get().setLastModifiedBy(CommonUtils.getLoggedInUsername());
		
		if (this.purchaseOrderRepository.save(purchaseOrder.get()) != null) return true;
		else throw new CustomException("Error in self approve. Please contact System Administrator");
	}

	@Override
	public PurchaseOrderItem savePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
			log.info("PO Item save started...");
			Optional<PurchaseOrderItem> oldPurchaseOrderItem = Optional.empty();
			
			if (purchaseOrderItem.getId() == null) {
				purchaseOrderItem.setRemainQuantity(purchaseOrderItem.getQuantity());
				purchaseOrderItem.setCreatedBy(CommonUtils.getLoggedInUsername());
				purchaseOrderItem.setUnbilledQuantity(purchaseOrderItem.getQuantity());
			} else {
				// Get the existing object using the deep copy
				oldPurchaseOrderItem = this.purchaseOrderItemRepository.findByIdAndIsDeleted(purchaseOrderItem.getId(), false);
				if (oldPurchaseOrderItem.isPresent()) {
					try {
						oldPurchaseOrderItem = Optional.ofNullable((PurchaseOrderItem) oldPurchaseOrderItem.get().clone());
					} catch (CloneNotSupportedException e) {
						log.error("Error while Cloning the object. Please contact administrator.");
						throw new CustomException("Error while Cloning the object. Please contact administrator.");
					}
				}
			}
			if (purchaseOrderItem.getId() != null) {
				Optional<PurchaseOrderItem> purchaseOrderExistValue = Optional.empty();
				if(purchaseOrderExistValue.isPresent()) {
					purchaseOrderExistValue = this.purchaseOrderItemRepository.getByPoId(purchaseOrderItem.getId());
					purchaseOrderExistValue.get().getRemainQuantity();
					purchaseOrderItem.setRemainQuantity(purchaseOrderExistValue.get().getRemainQuantity());
				}
			}

			purchaseOrderItem.setLastModifiedBy(CommonUtils.getLoggedInUsername());
			PurchaseOrderItem purchaseOrderItemSaved = this.purchaseOrderItemRepository.save(purchaseOrderItem);
			
			if (purchaseOrderItemSaved == null) {
				log.info("Error while saving the Purchase Order Item.");
				throw new CustomMessageException("Error while saving the Purchase Order Item.");
			}
			log.info("PO Item is saved :: " + purchaseOrderItemSaved.getId());
			
			// update the data in Item history table
			this.updatePurchaseOrderItemHistory(purchaseOrderItemSaved, oldPurchaseOrderItem);
			
			log.info("PO Item save Completed...");
			//return purchaseOrderItemSaved;
	
		return purchaseOrderItem;
	}
}
