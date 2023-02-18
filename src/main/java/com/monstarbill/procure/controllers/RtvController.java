package com.monstarbill.procure.controllers;

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

import com.monstarbill.procure.models.Rtv;
import com.monstarbill.procure.models.RtvHistory;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.PaginationResponse;
import com.monstarbill.procure.service.RtvService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/rtv")
@CrossOrigin(origins= "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false" )
public class RtvController {
	
	@Autowired
	private RtvService rtvService;
	
	@PostMapping("/save")
	public ResponseEntity<Rtv> saveRtv(@Valid @RequestBody Rtv rtv) {
		log.info("Saving the Rtv :: " + rtv.toString());
		rtv = this.rtvService.save(rtv);
		log.info("Rtv saved successfully");
		return ResponseEntity.ok(rtv);
	}
	
	@GetMapping("/get")
	public ResponseEntity<Rtv> findById(@RequestParam Long id) {
		log.info("Get Rtv for ID :: " + id);
		Rtv rtv = this.rtvService.getRtvById(id);
		if (rtv == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by id rtv");
		return new ResponseEntity<>(rtv, HttpStatus.OK);
	}
	
	@GetMapping("/self-approve")
	public ResponseEntity<Boolean> selfApprove(@RequestParam Long rtvId) {
		log.info("Self approve for Rtv ID :: " + rtvId);
		Boolean isApproved = this.rtvService.selfApprove(rtvId);
		log.info("Self approve for Rtv id Finished");
		return new ResponseEntity<>(isApproved, HttpStatus.OK);
	}
	
	/**
	 * get all rtv for the table with pagination
	 * @param 
	 * @return
	 */
	@PostMapping("/get/all")
	public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
		log.info("Get All rtvs started");
		PaginationResponse paginationResponse = new PaginationResponse();
		paginationResponse = this.rtvService.findAll(paginationRequest);
		log.info("Get All rtvs Finished");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}

	@GetMapping("/get/history")
	public ResponseEntity<List<RtvHistory>> findHistory(@RequestParam String rtvNumber, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber, @RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get RTV Audit for Subsidiary ID :: " + rtvNumber);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<RtvHistory> subsidiaryHistoris = this.rtvService.findHistoryByRtvNumber(rtvNumber, pageable);
		log.info("Returning from RTV Audit by RTV Number.");
		return new ResponseEntity<>(subsidiaryHistoris, HttpStatus.OK);
	}
	
	/**
	 * Send RTV for the approval for the first time
	 * @param id
	 * @return
	 */
	@GetMapping("/send-for-approval")
	public ResponseEntity<Boolean> sendForApproval(@RequestParam Long id) {
		log.info("Send for approval started for RTV ID :: " + id);
		Boolean isSentForApproval = this.rtvService.sendForApproval(id);
		log.info("Send for approval Finished for RTV ID :: " + id);
		return new ResponseEntity<>(isSentForApproval, HttpStatus.OK);
	}

	/**
	 * Approve all the selected RTV's from the Approval For RTV
	 * 
	 * @param rtvIds
	 * @return
	 */
	@PostMapping("/approve-all-rtv")
	public ResponseEntity<Boolean> approveAllRtvs(@RequestBody List<Long> rtvIds) {
		log.info("Approve all RTV's is started...");
		Boolean isAllApproved = this.rtvService.approveAllRtvs(rtvIds);
		log.info("Approve all RTV's is Finished...");
		return new ResponseEntity<>(isAllApproved, HttpStatus.OK);
	}
	
	@PostMapping("/reject-all-rtv")
	public ResponseEntity<Boolean> rejectAllRtvs(@RequestBody List<Rtv> rtvs) {
		log.info("Reject all RTV's is started...");
		Boolean isAllRejected = this.rtvService.rejectAllRtvs(rtvs);
		log.info("Reject all RTV's is Finished...");
		return new ResponseEntity<>(isAllRejected, HttpStatus.OK);
	}
	
	/*
	 * For LINE LEVEL next approver change
	 */
	@GetMapping("/update-next-approver")
	public ResponseEntity<Boolean> updateNextApproverByLine(@RequestParam Long approverId, @RequestParam Long rtvId) {
		return new ResponseEntity<>(this.rtvService.updateNextApprover(approverId, rtvId), HttpStatus.OK);
	}
}
