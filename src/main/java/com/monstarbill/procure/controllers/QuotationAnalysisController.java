package com.monstarbill.procure.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monstarbill.procure.commons.CustomException;
import com.monstarbill.procure.models.Location;
import com.monstarbill.procure.models.QuotationAnalysis;
import com.monstarbill.procure.models.QuotationAnalysisHistory;
import com.monstarbill.procure.models.QuotationAnalysisItem;
import com.monstarbill.procure.models.Supplier;
import com.monstarbill.procure.payload.request.MailRequest;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.IdNameResponse;
import com.monstarbill.procure.payload.response.PaginationResponse;
import com.monstarbill.procure.service.QuotationAnalysisService;

import lombok.extern.slf4j.Slf4j;

/**
 * All WS's of the Quotation Analysis(QA) and it's child components if any
 * @author Prashant
 * 22-07-2022
 */
@Slf4j
@RestController
@RequestMapping("/quotation-analysis")
@CrossOrigin(origins= "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false" )
public class QuotationAnalysisController {

	@Autowired
	private QuotationAnalysisService quotationAnalysisService;
	
	/**
	 * Save the Quotation Analysis & Items mapping
	 * @param quotationAnalysis
	 * @return
	 */
	@PostMapping("/save")
	public ResponseEntity<QuotationAnalysis> save(@Valid @RequestBody QuotationAnalysis quotationAnalysis) {
		log.info("Saving the Quotation Analysis :: " + quotationAnalysis.toString());
		try {
			quotationAnalysis = quotationAnalysisService.save(quotationAnalysis);
			log.info("Quotation Analysis saved successfully");
		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Error while saving the Quotation Analysis. Message : " + ex.getMessage());
			throw new CustomException("Error while saving the Quotation Analysis. Message : " + ex.getMessage());
		}
		return ResponseEntity.ok(quotationAnalysis);
	}
	
	/**
	 * get Quotation based on it's id
	 * @param id
	 * @return Quotation
	 */
	@GetMapping("/get")
	public ResponseEntity<QuotationAnalysis> findById(@RequestParam Long id) {
		log.info("Get Quotation Analysis for ID :: " + id);
		QuotationAnalysis quotationAnalysis = quotationAnalysisService.findById(id);
		if (quotationAnalysis == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Get Quotation Analysis By ID is Finished.");
		return new ResponseEntity<>(quotationAnalysis, HttpStatus.OK);
	}
	
	/**
	 * get list of RFQ's 
	 * @return
	 */
	@PostMapping("/get/all")
	public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
		log.info("Get all Quotations for analysis started.");
		PaginationResponse paginationResponse = new PaginationResponse();
		paginationResponse = quotationAnalysisService.findAll(paginationRequest);
		log.info("Get all Quotations for analysis completed.");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}


	/**
	 * Find history by QA Id
	 * Supported for server side pagination
	 * @param qaNumber
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 */
	@GetMapping("/get/history")
	public ResponseEntity<List<QuotationAnalysisHistory>> findHistoryById(@RequestParam String qaNumber, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber, @RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get Quotation Analysis History for Supplier ID :: " + qaNumber);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<QuotationAnalysisHistory> quotationAnalysisHistoris = this.quotationAnalysisService.findHistoryById(qaNumber, pageable);
		log.info("Returning from Quotation Analysis History by id.");
		return new ResponseEntity<>(quotationAnalysisHistoris, HttpStatus.OK);
	}

	/**
	 * Get QA-Numbers by PR-Numbers
	 * @param prNumbers
	 * @return
	 */
	@PostMapping("/get-qa-number-by-pr-ids")
	public ResponseEntity<List<QuotationAnalysis>> getQaNumberByPrIds(@RequestBody List<Long> prIds) {
		log.info("get-qa-number-by-pr-numbers started.");
		List<QuotationAnalysis> qaNumbersWithId = new ArrayList<QuotationAnalysis>();
		qaNumbersWithId = quotationAnalysisService.getQaNumberByPrIds(prIds);
		log.info("get-qa-number-by-pr-numbers completed.");
		return new ResponseEntity<>(qaNumbersWithId, HttpStatus.OK);
	}
	
	/**
	 * Get Supplier by QA Numbers
	 * @param prNumbers
	 * @return
	 */
	@PostMapping("/get-supplier-by-qa-ids")
	public ResponseEntity<List<Long>> getSuppliersByQaNumbers(@RequestBody List<Long> qaIds) {
		log.info("get-supplier-by-qa-numbers started.");
		List<Long> suppliers = new ArrayList<Long>();
		suppliers = quotationAnalysisService.getSuppliersByQaIds(qaIds);
		log.info("get-supplier-by-qa-numbers completed.");
		return new ResponseEntity<>(suppliers, HttpStatus.OK);
	}
	
	@GetMapping("/get-qa-number-by-subsidiary")
	public ResponseEntity<List<IdNameResponse>> findQaNumbersBySubsidiaryId(@RequestParam Long subsidiaryId) {
		log.info("Get QA Numbers for subsdiary :: " + subsidiaryId);
		List<IdNameResponse> qaNumbers = this.quotationAnalysisService.findQaNumbersBySubsidiaryId(subsidiaryId);
		log.info("Get QA Numbers for subsdiary is Finished.");
		return new ResponseEntity<>(qaNumbers, HttpStatus.OK);
	}
	
	@GetMapping("/get-suppliers-by-qa-id")
	public ResponseEntity<List<Supplier>> findSupplierByQaId(@RequestParam Long qaId) {
		log.info("Get Suppliers by QA Number :: " + qaId);
		List<Supplier> qaNumbers = this.quotationAnalysisService.findSupplierByQaId(qaId);
		log.info("Get Suppliers by QA Number is Finished.");
		return new ResponseEntity<>(qaNumbers, HttpStatus.OK);
	}
	
	@GetMapping("/get-pr-number-by-qa-id")
	public ResponseEntity<List<Long>> findPrNumbersByQaId(@RequestParam Long qaId) {
		log.info(" get-pr-number-by-qa-number :: " + qaId);
		List<Long> prNumbers = this.quotationAnalysisService.findPrIdsByQaId(qaId);
		log.info("get-pr-number-by-qa-number is Finished.");
		return new ResponseEntity<>(prNumbers, HttpStatus.OK);
	}
	
	@GetMapping("/get-locations-by-qa-and-supplier")
	public ResponseEntity<List<Location>> findLocationsByQaIdAndSupplier(@RequestParam Long qaId, @RequestParam Long supplierId) {
		log.info(" get-locations-by-qa-and-supplier :: " + qaId);
		List<Location> prLocations = this.quotationAnalysisService.findLocationsByQaIdAndSupplier(qaId, supplierId);
		log.info("get-locations-by-qa-and-supplier is Finished.");
		return new ResponseEntity<>(prLocations, HttpStatus.OK);
	}
	
	@GetMapping("/get-items-by-qa-supplier-location")
	public ResponseEntity<List<QuotationAnalysisItem>> findItemsByQaAndSupplierAndLocation(@RequestParam Long qaId, @RequestParam Long supplierId, @RequestParam Long locationId) {
		log.info(" get-items-by-qa-supplier-location :: " + qaId);
		List<QuotationAnalysisItem> prLocations = this.quotationAnalysisService.findItemsByQaAndSupplierAndLocation(qaId, supplierId, locationId);
		log.info("get-items-by-qa-supplier-location is Finished.");
		return new ResponseEntity<>(prLocations, HttpStatus.OK);
	}
	
	/**
	 * call on button click
	 * @param id
	 * @return
	 */
	@PostMapping("/send-mail")
	public ResponseEntity<String> sendMail(@RequestBody MailRequest mailRequest) {
		log.info("Send Mail started for QA.");
		String message = this.quotationAnalysisService.sendMail(mailRequest);
		log.info("Send Mail Finished for QA.");
		return new ResponseEntity<>(message, HttpStatus.OK);
	}
}
