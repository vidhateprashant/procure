package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.QuotationItem;

@Repository
public interface QuotationItemRepository extends JpaRepository<QuotationItem, String> {

	public Optional<QuotationItem> findByIdAndIsDeleted(Long id, boolean isDeleted);

	@Query(" SELECT new com.monstarbill.procure.models.QuotationItem(qi.id, qi.quotationId, qi.rfqNumber, qi.itemId, i.name, i.description, i.uom, qi.quantity, qi.receivedDate, qi.remarks, "
			+ " qi.prNumber, qi.prLocation, l.locationName, qi.prId) "
			+ " FROM Item i "
			+ " INNER JOIN QuotationItem qi ON i.id = qi.itemId "
			+ " LEFT JOIN Location l ON l.id = qi.prLocation "
			+ " WHERE qi.quotationId = :quotationId AND qi.isDeleted = false ")
	public List<QuotationItem> getItemWithDetails(@Param("quotationId") Long quotationId);

	@Query(" select new com.monstarbill.procure.models.QuotationItem(qi.rfqNumber, qi.quotationId, qi.itemId, i.name, qi.quantity, i.uom, qi.prNumber, qi.prLocation, l.locationName, qi.prId) "
			+ " FROM QuotationItem qi "
			+ " INNER JOIN Item i ON qi.itemId = i.id "
			+ " INNER JOIN QuotationItemVendor qim ON qim.itemId = qi.itemId AND qi.quotationId = qim.quotationId "
			+ " LEFT JOIN Location l ON l.id = qi.prLocation "
			+ " WHERE qim.vendorId = :vendorId AND qim.quotationId = :quotationId AND qim.isDeleted = :isDeleted AND qi.isDeleted = :isDeleted ")
	public List<QuotationItem> findByRfqIdAndVendorIdAndIsDeleted(@Param("vendorId") Long vendorId, @Param("quotationId") Long quotationId, @Param("isDeleted") boolean isDeleted);

	@Query(" select new com.monstarbill.procure.models.QuotationItem(qi.rfqNumber, qi.quotationId, qi.itemId, i.name, qi.quantity, i.uom, qi.prNumber, qi.prLocation, l.locationName, qi.prId) "
			+ " FROM QuotationItem qi "
			+ " INNER JOIN Item i ON qi.itemId = i.id "
			+ " LEFT JOIN Location l ON l.id = qi.prLocation "
			+ " WHERE qi.quotationId = :quotationId AND qi.isDeleted = :isDeleted ")
	public List<QuotationItem> findByRfqIdAndIsDeleted(Long quotationId, boolean isDeleted);

	@Query(" SELECT sum(quantity) FROM QuotationItem where isDeleted = false AND prId = :prId AND itemId = :itemId ")
	public Double findTotalQuantityByPrIdAndItem(Long prId, Long itemId);
}
