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
import org.springframework.web.bind.annotation.CrossOrigin;
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
import com.monstarbill.procure.models.PurchaseOrder;
import com.monstarbill.procure.models.PurchaseOrderHistory;
import com.monstarbill.procure.models.PurchaseOrderItem;
import com.monstarbill.procure.models.QuotationAnalysis;
import com.monstarbill.procure.payload.request.GenerateRfqPoRequest;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.PaginationResponse;
import com.monstarbill.procure.service.PurchaseOrderService;

import lombok.extern.slf4j.Slf4j;

/**
 * All WS's of the Purchase Order and it's child components if any
 * @author Prashant
 * 05-08-2022
 */
@Slf4j
@RestController
@RequestMapping("/po")
@CrossOrigin(origins= "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false" )
public class PurchaseOrderController {

	@Autowired
	private PurchaseOrderService purchaseOrderService;
	
	/**
	 * Save/update the PO	
	 * @param purchaseOrder
	 * @return
	 */
	@PostMapping("/save")
	public ResponseEntity<PurchaseOrder> save(@Valid @RequestBody PurchaseOrder purchaseOrder) {
		log.info("Saving the Purchase Order :: " + purchaseOrder.toString());
		try {
			purchaseOrder = purchaseOrderService.save(purchaseOrder);
		} catch (Exception e) {
			log.error("Error while saving the PO :: ");
			e.printStackTrace();
			throw new CustomException("Error while saving the PO " + e.toString());
		}
		log.info("Purchase Order saved successfully");
		return ResponseEntity.ok(purchaseOrder);
	}
	
	/**
	 * get Purchase Order based on po-number
	 * @param PO-Number
	 * @return PurchaseRequisition
	 */
	@GetMapping("/get")
	public ResponseEntity<PurchaseOrder> findById(@RequestParam Long id) {
		log.info("Get Purchase Order for ID :: " + id);
		PurchaseOrder purchaseOrder = purchaseOrderService.findByPoId(id);
		if (purchaseOrder == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("PO Find by PO Number is completed.");
		return new ResponseEntity<>(purchaseOrder, HttpStatus.OK);
	}
	
	@GetMapping("/self-approve")
	public ResponseEntity<Boolean> selfApprove(@RequestParam Long poId) {
		log.info("Self approve for PurchaseOrder ID :: " + poId);
		Boolean isApproved = this.purchaseOrderService.selfApprove(poId);
		log.info("Self approve for PurchaseOrder id Finished");
		return new ResponseEntity<>(isApproved, HttpStatus.OK);
	}
	
	/**
	 * get list of PO's with/without Filter 
	 * @return
	 */
	@PostMapping("/get/all")
	public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
		log.info("Get all Purchase Order started.");
		PaginationResponse paginationResponse = new PaginationResponse();
		paginationResponse = purchaseOrderService.findAll(paginationRequest);
		log.info("Get all Purchase Order completed.");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}

	/**
	 * Find history by PO number
	 * Supported for server side pagination
	 * @param poNumber
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 */
	@GetMapping("/get/history")
	public ResponseEntity<List<PurchaseOrderHistory>> findHistoryById(@RequestParam String poNumber, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber, @RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get Purchase Requisition Audit for Purchase Order ID :: " + poNumber);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<PurchaseOrderHistory> purchaseOrderHistories = this.purchaseOrderService.findHistoryById(poNumber, pageable);
		log.info("Returning from Purchase order Audit by Po Number.");
		return new ResponseEntity<>(purchaseOrderHistories, HttpStatus.OK);
	}
	
	/**
	 * This will generate automatically PO or RFQ Based on Input
	 * @param rfqPoRequest
	 * @return
	 */
	@PostMapping("/generate-po-rfq")
	public ResponseEntity<Boolean> generatePoRfq(@RequestBody GenerateRfqPoRequest rfqPoRequest) {
		log.info("Generate auto PO-RFQ is started for Module :: "+ rfqPoRequest.getModuleName());
		Boolean isGenerated = false;
		try {
			isGenerated = purchaseOrderService.generatePoRfq(rfqPoRequest);
			log.info("Generate auto PO-RFQ is Finished for Module :: "+ rfqPoRequest.getModuleName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(isGenerated, HttpStatus.OK);
	}
	
	/**
	 * This API will save QA and generate SINGLE PO for valid items
	 * @param QuotationAnalysis
	 * @return
	 */
	@PostMapping("/generate-po-by-qa")
	public ResponseEntity<List<PurchaseOrder>> generatePoFromQa(@RequestBody QuotationAnalysis QuotationAnalysis) {
		log.info("Generate PO based on QA is started for QA-id :: "+ QuotationAnalysis.getId());
		List<PurchaseOrder> purchaseOrders = new ArrayList<PurchaseOrder>();
		try {
			purchaseOrders = purchaseOrderService.generatePoFromQa(QuotationAnalysis);
			log.info("Generate PO based on QA is Completed for QA-id :: "+ QuotationAnalysis);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("Error while saving QA & generating PO.");
		}
		return new ResponseEntity<>(purchaseOrders, HttpStatus.OK);
	}
	
	/**
	 * This API will save QA and generate MULTIPLE PO for valid items
	 * @param QuotationAnalysis
	 * @return
	 */
	@PostMapping("/generate-multiple-po-by-qa")
	public ResponseEntity<List<PurchaseOrder>> generateMultiplePoFromQa(@RequestBody QuotationAnalysis QuotationAnalysis) {
		log.info("Generate PO based on QA is started for QA-id :: "+ QuotationAnalysis.getId());
		List<PurchaseOrder> purchaseOrders = new ArrayList<PurchaseOrder>();
		try {
			purchaseOrders = purchaseOrderService.generateMultiplePoFromQa(QuotationAnalysis);
			log.info("Generate PO based on QA is Completed for QA-id :: "+ QuotationAnalysis);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("Error while saving QA & generating PO.");
		}
		return new ResponseEntity<>(purchaseOrders, HttpStatus.OK);
	}

	
	/**
	 * get the all values for Purchase Order approval process
	 * 
	 * @return
	 */
	@GetMapping("/get-po-appoval")
	public ResponseEntity<List<PurchaseOrder>> getPoApproval(@RequestParam String userId) {
		List<PurchaseOrder> purchaseOrder = new ArrayList<PurchaseOrder>();
		try {
			purchaseOrder = purchaseOrderService.getPoApproval(userId);
			log.info("Getting the Purchase Requisition for approval " + purchaseOrder);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(
					"Exception while getting the approval process for the Purchase Order :: " + e.toString());
		}
		return ResponseEntity.ok(purchaseOrder);
	}

	/**
	 * get only PO number by Location , subsidiary , type
	 * @param locationId,subsidiaryId,type
	 * @return PO number
	 */
	@GetMapping("/get-po-by-location-number")
	public ResponseEntity<List<PurchaseOrder>> findByLocation(@RequestParam Long locationId, @RequestParam Long subsidiaryId, @RequestParam List<String> poStatus ) {
		List<PurchaseOrder> purchaseOrders = new ArrayList<PurchaseOrder>();
		try {
			purchaseOrders = purchaseOrderService.findByLocation(locationId, subsidiaryId, poStatus);
			log.info("Getting the PO Number by Location " + purchaseOrders);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(
					"Exception while getting the  PO Number by Location  :: " + e.toString());
		}
		return ResponseEntity.ok(purchaseOrders);
	}

	/**
	 * get only currency and supplier by PO number
	 * @param poId
	 * @return supplier and currency
	 */
	@GetMapping("/get-supplier-currency-by-po")
	public ResponseEntity<List<PurchaseOrder>> findByPoNumber(@RequestParam Long poId) {
		List<PurchaseOrder> purchaseOrders = new ArrayList<PurchaseOrder>();
		try {
			purchaseOrders = purchaseOrderService.findSupplierAndCurrencyByPoId(poId);
			log.info("Getting the supplier and currency by po number " + purchaseOrders);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(
					"Exception while getting the supplier and currency by po number for GRN :: " + e.toString());
		}
		return ResponseEntity.ok(purchaseOrders);
	}
	
	/**
	 * get only item details by PO number
	 * @param poId
	 * @return item name, description, UOM, order quantity
	 */
	@GetMapping("/get-item-by-po")
	public ResponseEntity<List<PurchaseOrderItem>> findByPoIdForItem(@RequestParam Long poId, @RequestParam String itemNature) {
		List<PurchaseOrderItem> purchaseOrderItems = new ArrayList<PurchaseOrderItem>();
		try {
			purchaseOrderItems = purchaseOrderService.findByPoIdForItem(poId, itemNature);
			log.info("Getting the item name, description, UOM, order quantity by po number " + purchaseOrderItems);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(
					"Exception while getting the item name, description, UOM, order quantity by po number for GRN :: " + e.toString());
		}
		return ResponseEntity.ok(purchaseOrderItems);
	}
	
	/**
	 * Send's the PO for approval
	 * 
	 * @param id
	 * @return
	 */
	@GetMapping("/send-for-approval")
	public ResponseEntity<Boolean> sendForApproval(@RequestParam Long id) {
		log.info("Send for approval started for PO ID :: " + id);
		Boolean isSentForApproval = this.purchaseOrderService.sendForApproval(id);
		log.info("Send for approval Finished for PO ID :: " + id);
		return new ResponseEntity<>(isSentForApproval, HttpStatus.OK);
	}

	/**
	 * Approve all the selected PO's from the Approval For PO
	 * 
	 * @param poIds
	 * @return
	 */
	@PostMapping("/approve-all-po")
	public ResponseEntity<Boolean> approveAllPos(@RequestBody List<Long> poIds) {
		log.info("Approve all PO's is started...");
		Boolean isAllApproved = this.purchaseOrderService.approveAllPos(poIds);
		log.info("Approve all PO's is Finished...");
		return new ResponseEntity<>(isAllApproved, HttpStatus.OK);
	}
	
	@PostMapping("/reject-all-pos")
	public ResponseEntity<Boolean> rejectAllSuppliers(@RequestBody List<PurchaseOrder> pos) {
		log.info("Reject all PurchaseOrder's is started...");
		Boolean isAllRejected = this.purchaseOrderService.rejectAllPos(pos);
		log.info("Reject all PurchaseOrder's is Finished...");
		return new ResponseEntity<>(isAllRejected, HttpStatus.OK);
	}
	
	/**
	 * Get PO with Items
	 * 
	 * @param poId
	 * @return PO
	 */
	@GetMapping("/getByPoId")
    public PurchaseOrder getByPoId(@RequestParam Long poId) {
        return purchaseOrderService.getByPoId(poId);
    }
	
	@GetMapping("/getByPoItemId")
    public PurchaseOrderItem getByPoItemId(@RequestParam Long poId, @RequestParam Long itemId) {
        return purchaseOrderService.getByPoItemId(poId, itemId);
    }
	/**
	 * Get POs
	 * 
	 * @param supplierId Supplier
	 * @param subsidiaryId Subsidiary
	 * @return POs
	 */
	@GetMapping("/getBySupplierSubsidiary")
    public List<PurchaseOrder> getBySupplierSubsidiary(@RequestParam Long supplierId, @RequestParam Long subsidiaryId) {
        return purchaseOrderService.getBySupplierSubsidiary(supplierId, subsidiaryId);
    }
	
	@GetMapping("/find-poitems-in-po-by-qa-item")
    public String findPoItemsByQaAndItem(@RequestParam Long qaId, @RequestParam Long itemId) {
        return purchaseOrderService.findPoItemsByQaAndItem(qaId, itemId);
    }
	
//	@GetMapping("/download-template")
//	public HttpEntity<ByteArrayResource> downloadTemplate() {
//		try {
//			byte[] excelContent = this.supplierService.downloadTemplate();
//
//			HttpHeaders header = new HttpHeaders();
//			header.setContentType(new MediaType("application", "force-download"));
//			header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=supplier_template.xlsx");
//
//			return new HttpEntity<>(new ByteArrayResource(excelContent), header);
//		} catch (Exception e) {
//			log.error("Something went wrong while downloading the Template. Please contact Administrator. Message : " + e.getLocalizedMessage());
//			throw new CustomMessageException("Something went wrong while downloading the Template. Please contact Administrator. Message : " + e.getLocalizedMessage());
//		}
//	}
	
	@PostMapping("/upload")
	public HttpEntity<ByteArrayResource> uploadFile(@RequestParam("file") MultipartFile file) {
		if (ExcelHelper.hasExcelFormat(file)) {
			try {
				byte[] excelContent = this.purchaseOrderService.upload(file);

				HttpHeaders header = new HttpHeaders();
				header.setContentType(new MediaType("application", "force-download"));
				header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=po_import_status.xlsx");

				return new HttpEntity<>(new ByteArrayResource(excelContent), header);
			} catch (Exception e) {
				String message = "Could not upload the file: " + file.getOriginalFilename() + "!";
				throw new CustomMessageException(message + ", Message : " + e.getLocalizedMessage());
			}
		}
		return null;
	}

	@GetMapping("/download-template")
	public HttpEntity<ByteArrayResource> downloadTemplate() {
		try {
			byte[] excelContent = this.purchaseOrderService.downloadTemplate();

			HttpHeaders header = new HttpHeaders();
			header.setContentType(new MediaType("application", "force-download"));
			header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=po_template.xlsx");

			return new HttpEntity<>(new ByteArrayResource(excelContent), header);
		} catch (Exception e) {
			log.error("Something went wrong while downloading the Template. Please contact Administrator. Message : " + e.getLocalizedMessage());
			throw new CustomMessageException("Something went wrong while downloading the Template. Please contact Administrator. Message : " + e.getLocalizedMessage());
		}
	}
	
	/*
	 * For LINE LEVEL next approver change
	 */
	@GetMapping("/update-next-approver")
	public ResponseEntity<Boolean> updateNextApproverByLine(@RequestParam Long approverId, @RequestParam Long poId) {
		return new ResponseEntity<>(this.purchaseOrderService.updateNextApprover(approverId, poId), HttpStatus.OK);
	}
	
	/**
	 * Save/update the PO list	
	 * @param purchaseOrder
	 * @return
	 */
	@PostMapping("/save-po-item")
	public ResponseEntity<PurchaseOrderItem> save(@Valid @RequestBody PurchaseOrderItem purchaseOrderItem) {
		log.info("Saving the Purchase Order item :: " + purchaseOrderItem.toString());
		try {
			purchaseOrderItem = purchaseOrderService.savePurchaseOrderItem(purchaseOrderItem);
		} catch (Exception e) {
			log.error("Error while saving the PO :: ");
			e.printStackTrace();
			throw new CustomException("Error while saving the PO " + e.toString());
		}
		log.info("Purchase Order saved successfully");
		return ResponseEntity.ok(purchaseOrderItem);
	}
}
