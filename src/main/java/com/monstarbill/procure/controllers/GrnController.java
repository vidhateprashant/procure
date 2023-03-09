package com.monstarbill.procure.controllers;

import java.util.List;

import javax.validation.Valid;

import org.apache.commons.collections4.CollectionUtils;
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

import com.monstarbill.procure.commons.CustomException;
import com.monstarbill.procure.models.Grn;
import com.monstarbill.procure.models.GrnHistory;
import com.monstarbill.procure.models.GrnItem;
import com.monstarbill.procure.payload.request.PaginationRequest;
import com.monstarbill.procure.payload.response.PaginationResponse;
import com.monstarbill.procure.service.GrnService;

import lombok.extern.slf4j.Slf4j;

/**
 * All WS's of the Purchase Order and it's child components if any
 * 
 * @author Prithwish 10-09-2022
 */
@Slf4j
@RestController
@RequestMapping("/grn")
//@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 4800, allowCredentials = "false")
public class GrnController {

	@Autowired
	private GrnService grnService;

	/**
	 * Save/update the GRN
	 * 
	 * @param grn
	 * @return
	 */
	@PostMapping("/save")
	public ResponseEntity<List<Grn>> save(@Valid @RequestBody List<Grn> grns) {
		log.info("Saving the GRN :: " + grns.toString());
		try {
			grns = grnService.save(grns);
		} catch (Exception e) {
			log.error("Error while saving the GRN :: ");
			e.printStackTrace();
			throw new CustomException("Error while saving the GRN " + e.toString());
		}
		log.info("GRN saved successfully");
		return ResponseEntity.ok(grns);
	}

	/**
	 * get the all values for GRN 
	 * @return
	 */
	@PostMapping("/get/all")
	public ResponseEntity<PaginationResponse> findAll(@RequestBody PaginationRequest paginationRequest) {
		log.info("Get all GRN started.");
		PaginationResponse paginationResponse = new PaginationResponse();
		paginationResponse = grnService.findAll(paginationRequest);
		log.info("Get all GRN completed.");
		return new ResponseEntity<>(paginationResponse, HttpStatus.OK);
	}
	
	/**
	 * get the GRN by id
	 * @param id
	 * @return
	 */
	@GetMapping("/get")
	public ResponseEntity<Grn> findById(@RequestParam Long id) {
		log.info("Get GRN for ID :: " + id);
		Grn grn = grnService.getGrnById(id);
		if (grn == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by id GRN");
		return new ResponseEntity<>(grn, HttpStatus.OK);
	}
	
	/**
	 * delete the id by GRN
	 * 
	 * @param id
	 * @return
	 */
	@GetMapping("/delete")
	public ResponseEntity<Boolean> deleteById(@RequestParam Long id) {
		log.info("Delete GRN by ID :: " + id);
		boolean isDeleted = false;
		isDeleted = grnService.deleteById(id);
		log.info("GRN by ID Completed.");
		return new ResponseEntity<>(isDeleted, HttpStatus.OK);
	}
	
	/**
	 * Find history by GRN number
	 * Supported for server side pagination
	 * @param grnNumber
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 */
	@GetMapping("/get/history")
	public ResponseEntity<List<GrnHistory>> findHistoryById(@RequestParam String grnNumber, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(defaultValue = "0") int pageNumber, @RequestParam(defaultValue = "id") String sortColumn) {
		log.info("Get GRN Audit for grn ID :: " + grnNumber);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortColumn));
		List<GrnHistory> grnHistories = this.grnService.findHistoryById(grnNumber, pageable);
		log.info("Returning from grn Audit by GRN Number.");
		return new ResponseEntity<>(grnHistories, HttpStatus.OK);
	}
	
	/**
	 * Find all the GRN Items by the GRN Number
	 * @param grnNumber
	 * @return
	 */
	@GetMapping("/find-grn-items")
	public ResponseEntity<List<GrnItem> > findItemsByGrnNumber(@RequestParam String grnNumber) {
		log.info("Get GRN Items for GrnNumber :: " + grnNumber);
		List<GrnItem> grnItems = grnService.findGrnItemsByGrnNumber(grnNumber);
		if (CollectionUtils.isEmpty(grnItems)) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Find GRN Items by GRN Number completed.");
		return new ResponseEntity<>(grnItems, HttpStatus.OK);
	}
	
	
	@GetMapping("/getByGrnId")
    public Grn getByGrnId(@RequestParam Long grnId)
    {
        return grnService.getByGrnId(grnId);
    }
	
	@GetMapping("/getByGrnItemId")
    public GrnItem getByGrnItemId(@RequestParam Long grnId, @RequestParam Long itemId)
    {
        return grnService.getByGrnItemId(grnId, itemId);
    }
	/**
	 * Get GRNs
	 * 
	 * @param poId
	 * @return GRNs
	 */
	@GetMapping("/getByPoId")
    public List<Grn> getByPoId(@RequestParam Long poId)
    {
        return grnService.getByPoId(poId);
    }
	
	
	/**
	 * get the GRN by subsidiary id
	 * @param subsidairy id
	 * @return
	 */
	@GetMapping("/get-by-subsidiary-id")
	public ResponseEntity<List<Grn>> findBySubsidiaryId(@RequestParam Long subsidiaryId) {
		log.info("Get GRN for ID :: " +  subsidiaryId);
		List<Grn> grn = grnService.getGrnBySubsidiaryId(subsidiaryId);
		if (grn == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by subsidairy id GRN");
		return new ResponseEntity<>(grn, HttpStatus.OK);
	}
	
	/**
	 * get the GRN item by grn id
	 * @param subsidairy id
	 * @return
	 */
	@GetMapping("/get-by-grn-id")
	public ResponseEntity<List<GrnItem>> findByGrnId(@RequestParam Long grnId) {
		log.info("Get GRN for ID :: " +  grnId);
		List<GrnItem> grnItem = grnService.getGrnItemByGrnId(grnId);
		if (grnItem == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by grn id GRN item");
		return new ResponseEntity<>(grnItem, HttpStatus.OK);
	}
	
	/**
	 * get the GRN item by grn id and item id
	 * @param subsidairy id
	 * @return
	 */
	@GetMapping("/get-by-grn-id-and-item-id")
	public ResponseEntity<List<GrnItem>> findByGrnIdAndItemId(@RequestParam Long grnId, @RequestParam Long itemId) {
		log.info("Get GRN for ID :: " +  grnId);
		List<GrnItem> grnItem = grnService.getGrnItemByGrnIdAndItemId(grnId, itemId);
		if (grnItem == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		log.info("Returning from find by grn id GRN item");
		return new ResponseEntity<>(grnItem, HttpStatus.OK);
	}
	
	/**
	 * Save/update the GRN item
	 * 
	 * @param grn
	 * @return
	 */
	@PostMapping("/save-grn-item")
	public ResponseEntity<List<GrnItem>> saveGrnItem(@Valid @RequestBody List<GrnItem> grnItems) {
		log.info("Saving the GRN :: " + grnItems.toString());
		try {
			grnItems = grnService.saveGrnItem(grnItems);
		} catch (Exception e) {
			log.error("Error while saving the GRN :: ");
			e.printStackTrace();
			throw new CustomException("Error while saving the GRN items" + e.toString());
		}
		log.info("GRN saved successfully");
		return ResponseEntity.ok(grnItems);
	}
	
	/**
	 * Save/update the GRN item object
	 * 
	 * @param grn
	 * @return
	 */
	@PostMapping("/save-grn-item-object")
	public ResponseEntity<GrnItem> saveGrnItemObject(@Valid @RequestBody GrnItem grnItem) {
		log.info("Saving the GRN :: " + grnItem.toString());
		try {
			grnItem = grnService.saveGrnItemObject(grnItem);
		} catch (Exception e) {
			log.error("Error while saving the GRN :: ");
			e.printStackTrace();
			throw new CustomException("Error while saving the GRN items" + e.toString());
		}
		log.info("GRN saved successfully");
		return ResponseEntity.ok(grnItem);
	}

	/**
	 * If unbilled quantiyt of all grn items is 0 then it is processed then return true
	 * @param grnId
	 * @return
	 */
	@GetMapping("/is-grn-fully-processed")
	public ResponseEntity<Boolean> isGrnFullyProcessed(@RequestParam Long grnId) {
		log.info("is-grn-fully-processed for ID :: " +  grnId);
		Boolean isProcessed = grnService.isGrnFullyProcessed(grnId);
		log.info("is-grn-fully-processed Finished");
		return new ResponseEntity<>(isProcessed, HttpStatus.OK);
	}
	
}
