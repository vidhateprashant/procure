package com.monstarbill.procure.controllers;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monstarbill.procure.models.Quotation;
import com.monstarbill.procure.models.QuotationHistory;
import com.monstarbill.procure.models.QuotationItem;
import com.monstarbill.procure.models.QuotationVendors;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.PaginationResponse;
import com.monstarbill.procure.service.QuotationService;

import lombok.extern.slf4j.Slf4j;

/**
 * All WS's of the Quotation and it's child components if any
 * @author Prashant
 * 16-07-2022
 */
@Slf4j
@RestController
@RequestMapping("/quotation")
//@CrossOrigin(origins= "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false" )
public class QuotationController {

	@Autowired
	private QuotationService quotationService;
	
	/**
	 * Save the Quotation, Items & Vendors
	 * This will save the RFQ in Draft status	
	 * @param quotation
	 * @return
	 */
	@PostMapping("/save")
	public ResponseEntity<Quotation> save(@Valid @RequestBody Quotation quotation) {
		log.info("Saving the Quotation :: " + quotation.toString());
		boolean isSubmitted = quotation.isSubmitted();
		quotation = quotationService.save(quotation, isSubmitted);	
		log.info("Quotation saved successfully");
		return ResponseEntity.ok(quotation);
	}
	
	/**
	 * get Quotation based on it's id
	 * @param id
	 * @return Quotation
	 */
	@GetMapping("/get")
	public ResponseEntity<Quotation> findById(@RequestParam Long id) {
		log.info("Get Quotation for ID :: " + id);
		Quotation quotation = quotationService.findById(id);
		if (quotation == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by id Item");
		return new ResponseEntity<>(quotation, HttpStatus.OK);
	}
	
	/**
	 * Manually close RFQ. On Button Click
	 * Change the status from Open / Process to Close
	 * @param id
	 * @return
	 */
	@GetMapping("/close")
	public ResponseEntity<Quotation> closeQuotation(@RequestParam Long id) {
		log.info("Close Quotation for ID :: " + id);
		Quotation quotation = quotationService.closeQuotation(id);
		if (quotation == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Close Quotation for ID Finished ::" + id);
		return new ResponseEntity<>(quotation, HttpStatus.OK);
	}
	
	/**
	 * get only Quotation and not item's and vendor's based on it's rfq-number
	 * @param rfq-number
	 * @return Quotation
	 */
//	@GetMapping("/get-by-rfq-number")
//	public ResponseEntity<Quotation> findByRfqNumber(@RequestParam String rfqNumber) {
//		log.info("Get Quotation for rfq Number :: " + rfqNumber);
//		Quotation quotation = quotationService.findByRfqNumber(rfqNumber);
//		if (quotation == null) {
//			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//		}
//		log.info("Get Quotation find by rfq Number Finished.");
//		return new ResponseEntity<>(quotation, HttpStatus.OK);
//	}
	
	/**
	 * NOT USING THIS ONE
	 * @param quotationItem
	 * @return
	 */
	@Deprecated
	@PostMapping("/item/save")
	public ResponseEntity<QuotationItem> save(@Valid @RequestBody QuotationItem quotationItem) {
		log.info("Saving the Quotation Item :: " + quotationItem.toString());
		quotationItem = quotationService.save(quotationItem);
		log.info("Quotation Item saved successfully");
		return ResponseEntity.ok(quotationItem);
	}
	
	/**
	 * get list of RFQ's 
	 * @return
	 */
	@PostMapping("/get/all")
	public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
		log.info("Get all Quotations started.");
		PaginationResponse paginationResponse = new PaginationResponse();
		paginationResponse = quotationService.findAll(paginationRequest);
		log.info("Get all Quotations completed.");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}

	/**
	 * Find history by Item Id
	 * Supported for server side pagination
	 * @param rfqNumber
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 */
	@GetMapping("/get/history")
	public ResponseEntity<List<QuotationHistory>> findHistoryById(@RequestParam String rfqNumber, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber, @RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get Quotation History for Supplier ID :: " + rfqNumber);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<QuotationHistory> itemHistoris = this.quotationService.findHistoryById(rfqNumber, pageable);
		log.info("Returning from Quotation History by id.");
		return new ResponseEntity<>(itemHistoris, HttpStatus.OK);
	}

	/**
	 * get vendor's based on it's rfq-number
	 * @param rfq-number
	 * @return Quotation
	 */
	@GetMapping("/get-vendors-by-rfq-id")
	public ResponseEntity<List<QuotationVendors>> findVendorsByRfqNumber(@RequestParam Long rfqId) {
		log.info("Get Quotation Vendors for rfq Number :: " + rfqId);
		List<QuotationVendors> quotationVendors = quotationService.findVendorsByRfqId(rfqId);
		if (quotationVendors == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Get Quotation Vendors find by rfq Number Finished.");
		return new ResponseEntity<>(quotationVendors, HttpStatus.OK);
	}
	
	/**
	 * get items based on it's rfq-number & Vendor
	 * @param rfq-number
	 * @return Quotation
	 */
	@GetMapping("/get-items-by-vendor-and-rfq")
	public ResponseEntity<List<QuotationItem>> findQuotationItemByRfqNumberAndVendor(@RequestParam Long rfqId, @RequestParam Long vendorId) {
		log.info("Get Quotation Item for rfq Number :: " + rfqId + " and Vendor : " + vendorId);
		List<QuotationItem> quotationItems = quotationService.findQuotationItemByRfqIdAndVendor(rfqId, vendorId);
		if (quotationItems == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Get Quotation Item find by rfq Number Finished.");
		return new ResponseEntity<>(quotationItems, HttpStatus.OK);
	}
	
	@GetMapping("/get-items-by-rfq")
	public ResponseEntity<List<QuotationItem>> findQuotationItemByRfqId(@RequestParam Long rfqId) {
		log.info("Get Quotation Item for rfq Number :: " + rfqId);
		List<QuotationItem> quotationItems = quotationService.findQuotationItemByRfqId(rfqId);
		if (quotationItems == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Get Quotation Item find by rfq Number Finished.");
		return new ResponseEntity<>(quotationItems, HttpStatus.OK);
	}
	
	@GetMapping("/get-by-subsidiary")
	public ResponseEntity<List<Quotation>> findBySubsidiaryId(@RequestParam Long subsidiaryId) {
		log.info("Get Quotation for findBySubsidiaryId :: " + subsidiaryId);
		List<Quotation> quotations = quotationService.findBySubsidiaryId(subsidiaryId);
		log.info("Returning from find by findBySubsidiaryId");
		return new ResponseEntity<>(quotations, HttpStatus.OK);
	}
	
	@GetMapping("/send-mail")
	public ResponseEntity<String> sendMail(@RequestParam Long id) {
		log.info("Send Mail started for RFQ id :: " + id);
		String message = quotationService.sendMail(id);
		log.info("Send Mail Finished for RFQ id :: " + id);
		return new ResponseEntity<>(message, HttpStatus.OK);
	}
	
	@GetMapping("/send-notification")
	public ResponseEntity<String> sendNotification(@RequestParam Long id) {
		log.info("Send Notification started for RFQ id :: " + id);
		String message = quotationService.sendNotification(id);
		log.info("Send Mail Notification for RFQ id :: " + id);
		return new ResponseEntity<>(message, HttpStatus.OK);
	}
	
	/**
	 * send mail to single RFQ Vendor from 2nd tab
	 * @param mailId
	 * @return
	 */
	@GetMapping("/send-mail-to-rfq-supplier")
	public ResponseEntity<String> sendMailToRfqSupplier(@RequestParam String mailId) {
		log.info("Send Mail started for RFQ Vendor id :: " + mailId);
		String message = quotationService.sendMailToRfqSupplier(mailId);
		log.info("Send Mail Finished for RFQ Vendor id :: " + mailId);
		return new ResponseEntity<>(message, HttpStatus.OK);
	}
}
