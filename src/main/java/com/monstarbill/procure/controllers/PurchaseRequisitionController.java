package com.monstarbill.procure.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.monstarbill.procure.commons.CustomException;
import com.monstarbill.procure.commons.CustomMessageException;
import com.monstarbill.procure.commons.ExcelHelper;
import com.monstarbill.procure.models.PrItem;
import com.monstarbill.procure.models.PurchaseRequisition;
import com.monstarbill.procure.models.PurchaseRequisitionHistory;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.IdNameResponse;
import com.monstarbill.procure.payload.response.PaginationResponse;
import com.monstarbill.procure.service.PurchaseRequisitionService;

import lombok.extern.slf4j.Slf4j;

/**
 * All WS's of the Purchase Requisition and it's child components if any
 * @author Prashant
 */
@Slf4j
@RestController
@RequestMapping("/pr")
//@CrossOrigin(origins= "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false" )
public class PurchaseRequisitionController {

	@Autowired
	private PurchaseRequisitionService purchaseRequisitionService;
	
	/**
	 * Save/update the Item	
	 * @param purchaseRequisition
	 * @return
	 */
	@PostMapping("/save")
	public ResponseEntity<PurchaseRequisition> save(@Valid @RequestBody PurchaseRequisition purchaseRequisition) {
		log.info("Saving the Purchase Requisition :: " + purchaseRequisition.toString());
		try {
			purchaseRequisition = purchaseRequisitionService.save(purchaseRequisition);
			log.info("Purchase Requisition saved successfully");
			return ResponseEntity.ok(purchaseRequisition);
		} catch (Exception e) {
			log.error("Error while saving the PR.");
			e.printStackTrace();
			throw new CustomMessageException("Error while saving the PR :: " + e.toString());
		}
	}
	
	/**
	 * get Purchase Requisition based on it's id
	 * @param id
	 * @return PurchaseRequisition
	 */
	@GetMapping("/get")
	public ResponseEntity<PurchaseRequisition> findById(@RequestParam Long id) {
		log.info("Get Purchase Requisition for ID :: " + id);
		PurchaseRequisition purchaseRequisition = purchaseRequisitionService.findById(id);
		if (purchaseRequisition == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by id Purchase Requisition");
		return new ResponseEntity<>(purchaseRequisition, HttpStatus.OK);
	}
	
	@GetMapping("/self-approve")
	public ResponseEntity<Boolean> selfApprove(@RequestParam Long prId) {
		log.info("Self approve for PR ID :: " + prId);
		Boolean isSelfApproved = this.purchaseRequisitionService.selfApprove(prId);
		log.info("Self approve for PR id Finished");
		return new ResponseEntity<>(isSelfApproved, HttpStatus.OK);
	}
	
	/**
	 * Get all the PR-Numbers for which RFQ is not created
	 * @return
	 */
	@GetMapping("/get-unprocessed-distinct-pr-numbers")
	public ResponseEntity<List<IdNameResponse>> findDistinctPrNumbers(/* @RequestParam Long subsidiaryId */) {
		log.info("Get Purchase Requisition Numbers Started...");
		List<IdNameResponse> prNumbers = purchaseRequisitionService.findDistinctPrNumbers();
		log.info("Get Purchase Requisition Numbers Finished...");
		return new ResponseEntity<>(prNumbers, HttpStatus.OK);
	}
	
	/**
	 * Get the details based on PR-Number
	 * @param prNumber
	 * @return
	 */
//	@GetMapping("/get-by-pr-number")
//	public ResponseEntity<PurchaseRequisition> findByPrNumber(@RequestParam String prNumber) {
//		log.info("Get Purchase Requisition for prNumber :: " + prNumber);
//		PurchaseRequisition purchaseRequisition = purchaseRequisitionService.findByPrNumber(prNumber);
//		if (purchaseRequisition == null) {
//			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//		}
//		log.info("Returning from find by pr-Number Purchase Requisition");
//		return new ResponseEntity<>(purchaseRequisition, HttpStatus.OK);
//	}
	
	/**
	 * get list of Items with/without Filter 
	 * @return
	 */
	@PostMapping("/get/all")
	public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
		log.info("Get all Purchase Requisition started.");
		PaginationResponse paginationResponse = new PaginationResponse();
		paginationResponse = purchaseRequisitionService.findAll(paginationRequest);
		log.info("Get all Purchase Requisition completed.");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}

	/**
	 * soft delete the Item by it's id
	 * @param id
	 * @return
	 */
	@GetMapping("/delete")
	public ResponseEntity<Boolean> deleteById(@RequestParam Long id) {
		log.info("Delete Purchase Requisition by ID :: " + id);
		boolean isDeleted = false;
		isDeleted = purchaseRequisitionService.deleteById(id);
		log.info("Delete Purchase Requisition by ID Completed.");
		return new ResponseEntity<>(isDeleted, HttpStatus.OK);
	}
	
	/**
	 * Find history by Item Id
	 * Supported for server side pagination
	 * @param prNumber
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 */
	@GetMapping("/get/history")
	public ResponseEntity<List<PurchaseRequisitionHistory>> findHistoryById(@RequestParam String prNumber, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber, @RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get Purchase Requisition Audit for PR number :: " + prNumber);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<PurchaseRequisitionHistory> purchaseRequisitionHistoris = this.purchaseRequisitionService.findHistoryById(prNumber, pageable);
		log.info("Returning from Purchase Requisition Audit by PR number.");
		return new ResponseEntity<>(purchaseRequisitionHistoris, HttpStatus.OK);
	}

	/**
	 * Save the PR-Item mapping in the Table
	 * @param prItem
	 * @return
	 */
	@PostMapping("/item/save")
	public ResponseEntity<PrItem> save(@Valid @RequestBody PrItem prItem) {
		log.info("Saving the PR-Item :: " + prItem.toString());
		prItem = purchaseRequisitionService.save(prItem);
		log.info("PR-Item saved successfully");
		return ResponseEntity.ok(prItem);
	}
	
	/**
	 * Save the PR-Item mapping in the Table
	 * @param prItem
	 * @return
	 */
	@GetMapping("/item/delete")
	public ResponseEntity<Boolean> save(@RequestParam Long id) {
		boolean isDeleted = false;
		log.info("Delete the PR-Item for ID :: " + id);
		isDeleted = purchaseRequisitionService.deletePrItemMapping(id);
		log.info("PR-Item Deleted successfully");
		return ResponseEntity.ok(isDeleted);
	}
	
	/**
	 * Get all the Items from PR for those PO is not created
	 * @param prNumber
	 * @return
	 */
	@GetMapping("/get-unprocessed-items-by-pr-id")
	public ResponseEntity<List<PrItem>> findUnprocessedItemsByPrId(@RequestParam Long prId, @RequestParam String formName) {
		log.info("Get Purchase Requisition for prNumber :: " + prId);
		List<PrItem> prItems = purchaseRequisitionService.findUnprocessedItemsByPrId(prId, formName);
		if (prItems == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Find Items by PR Number is finished.");
		return new ResponseEntity<>(prItems, HttpStatus.OK);
	}
	
	/**
	 * Get PR-numbers for which PO is not created.
	 * Will return PR-Number if PO is not created for ALL items
	 * @param prNumber
	 * @return
	 */
	@GetMapping("/get-pending-pr-for-po")
	public ResponseEntity<List<IdNameResponse>> findPendingPrForPo(@RequestParam Long subsidiaryId, @RequestParam Long locationId) {
		log.info("Get Purchase Requisition for PO.");
		List<IdNameResponse> prNumbers = purchaseRequisitionService.findPendingPrForPo(subsidiaryId, locationId);
		log.info("Find Items by PR Number for PO finished.");
		return new ResponseEntity<>(prNumbers, HttpStatus.OK);
	}
	
	/**
	 * get the all values for Purchase requisition approval process
	 * 
	 * @return
	 */
	@GetMapping("/get-pr-appoval")
	public ResponseEntity<List<PurchaseRequisition>> getPrApproval(@RequestParam String userId) {
		List<PurchaseRequisition> purchaseRequisition = new ArrayList<PurchaseRequisition>();
		try {
			 purchaseRequisition =	purchaseRequisitionService.getPrApproval(userId);
			log.info("Getting the Purchase Requisition for approval " + purchaseRequisition);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("Exception while getting the approval process for the Purchase requisition :: " + e.toString());
		}
		return ResponseEntity.ok(purchaseRequisition);
	}
	
	/**
	 * get the all values for Purchase requisition Approved with/without Filter 
	 * @return
	 */
	@PostMapping("/get-approved-pr")
	public ResponseEntity<PaginationResponse> findAllApproved(@RequestBody PaginationRequest paginationRequest, @RequestParam Long subsidiaryId) {
		log.info("Get all Purchase Requisition approved started.");
		PaginationResponse paginationResponse = new PaginationResponse();
		paginationResponse = purchaseRequisitionService.findAllApprovedPr(paginationRequest, subsidiaryId);
		log.info("Get all Purchase Requisition approved completed.");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}
	
	/**
	 * Send's the PR for approval
	 * @param id
	 * @return
	 */
	@GetMapping("/send-for-approval")
	public ResponseEntity<Boolean> sendForApproval(@RequestParam Long id) {
		log.info("Send for approval started for ID :: " + id);
		Boolean isSentForApproval = purchaseRequisitionService.sendForApproval(id);
		log.info("Send for approval Finished for ID :: " + id);
		return new ResponseEntity<>(isSentForApproval, HttpStatus.OK);
	}

	/**
	 * Approve all the selected PR's
	 * 
	 * @param prIds
	 * @return
	 */
	@PostMapping("/approve-all-prs")
	public ResponseEntity<Boolean> approveAllPrs(@RequestBody List<Long> prIds) {
		log.info("Approve all PR's is started...");
		Boolean isAllApproved = purchaseRequisitionService.approveAllPrs(prIds);
		log.info("Approve all PR's is Finished...");
		return new ResponseEntity<>(isAllApproved, HttpStatus.OK);
	}

	/**
	 * Reject all the selected PR's
	 * @param prs
	 * @return
	 */
	@PostMapping("/reject-all-prs")
	public ResponseEntity<Boolean> rejectAllPrs(@RequestBody List<PurchaseRequisition> prs) {
		log.info("Reject all PR's is started...");
		Boolean isAllApproved = purchaseRequisitionService.rejectAllPrs(prs);
		log.info("Reject all PR's is Finished...");
		return new ResponseEntity<>(isAllApproved, HttpStatus.OK);
	}
	
	@PostMapping("/get-unprocessed-items-by-pr-ids")
	public ResponseEntity<List<PrItem>> findUnprocessedItemsByPrIds(@RequestBody List<Long> prIds, @RequestParam String formName) {
		log.info("Get Purchase Requisition for prNumbers :: " + prIds);
		List<PrItem> prItems = purchaseRequisitionService.findUnprocessedItemsByPrIds(prIds, formName);
		log.info("Find Items by PR Numbers is finished.");
		return new ResponseEntity<>(prItems, HttpStatus.OK);
	}
	
	@GetMapping("/get-prs-by-subsidiary")
	public ResponseEntity<List<IdNameResponse>> findApprovedPrsBySubsidiary(@RequestParam Long subsidiaryId) {
		log.info("Get Purchase Requisition By subsidiary.");
		List<IdNameResponse> prNumbers = purchaseRequisitionService.findApprovedPrsBySubsidiary(subsidiaryId);
		log.info("Find Items by PR Number for PO finished.");
		return new ResponseEntity<>(prNumbers, HttpStatus.OK);
	}
	
	/*
	 * For LINE LEVEL next approver change
	 */
	@GetMapping("/update-next-approver")
	public ResponseEntity<Boolean> updateNextApproverByLine(@RequestParam Long approverId, @RequestParam Long prId) {
		return new ResponseEntity<>(this.purchaseRequisitionService.updateNextApprover(approverId, prId), HttpStatus.OK);
	}
	
	@GetMapping("/download-template")
	public HttpEntity<ByteArrayResource> downloadTemplate() {
		try {
			byte[] excelContent = this.purchaseRequisitionService.downloadTemplate();

			HttpHeaders header = new HttpHeaders();
			header.setContentType(new MediaType("application", "force-download"));
			header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pr_template.xlsx");

			return new HttpEntity<>(new ByteArrayResource(excelContent), header);
		} catch (Exception e) {
			log.error("Something went wrong while downloading the Template. Please contact Administrator. Message : " + e.getLocalizedMessage());
			throw new CustomMessageException("Something went wrong while downloading the Template. Please contact Administrator. Message : " + e.getLocalizedMessage());
		}
	}
	
	@PostMapping("/upload")
	public HttpEntity<ByteArrayResource> uploadFile(@RequestParam("file") MultipartFile file) {
		if (ExcelHelper.hasExcelFormat(file)) {
			try {
				byte[] excelContent = this.purchaseRequisitionService.upload(file);

				HttpHeaders header = new HttpHeaders();
				header.setContentType(new MediaType("application", "force-download"));
				header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pr_import_status.xlsx");

				return new HttpEntity<>(new ByteArrayResource(excelContent), header);
			} catch (Exception e) {
				String message = "Could not upload the file: " + file.getOriginalFilename() + "!";
				throw new CustomMessageException(message + ", Message : " + e.getLocalizedMessage());
			}
		}
		return null;
	}
}
