package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.PurchaseOrder;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {

	public Optional<PurchaseOrder> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public Optional<PurchaseOrder> findByPoNumber(String poNumber);
	
	@Query("select new com.monstarbill.procure.models.PurchaseOrder(p.id, p.poNumber, p.subsidiaryId,p.totalAmount, p.locationId, p.location, p.supplierId, p.poDate, p.rejectedComments, p.poStatus, p.memo, s.name as subsidiaryName, "
			+ " l.locationName as locationName, su.name as supplierName, p.approvedBy, p.nextApprover, p.nextApproverRole, e.fullName ) from PurchaseOrder p "
			+ " inner join Subsidiary s ON s.id = p.subsidiaryId inner join Location l ON l.id = p.locationId left join Employee e ON CAST(e.id as text) = p.approvedBy "
			+ " left join Supplier su ON su.id = p.supplierId where p.poStatus in :status AND p.isDeleted is false and p.nextApprover = :userId")
	public List<PurchaseOrder> findAllByPoStatus(@Param("status")List<String> status, @Param("userId")String userId);
	
	@Query("select new com.monstarbill.procure.models.PurchaseOrder(po.id,po.poNumber,po.poStatus, po.poDate) from PurchaseOrder po where (po.poStatus in :poStatus) AND po.locationId = :locationId AND po.subsidiaryId = :subsidiaryId AND po.isDeleted = :isDeleted AND po.matchType = '3 Way' ")
	public List<PurchaseOrder> getAllPoByLocationIdAndSubsidiaryIdAndPoStatusAndIsDeleted(@Param("locationId") Long locationId,@Param("subsidiaryId") Long subsidiaryId,@Param("poStatus") List<String> poStatus, @Param("isDeleted") boolean isDeleted);

	@Query("select new com.monstarbill.procure.models.PurchaseOrder(po.id,po.poNumber, po.supplierId, po.currency, su.name as supplierName) from PurchaseOrder po inner join Supplier su ON po.supplierId = su.id where po.id = :poId AND po.isDeleted = :isDeleted")
	public List<PurchaseOrder> getAllSupplierAndCurrencyByPoAndIsDeleted(@Param("poId") Long poId, @Param("isDeleted") boolean isDeleted);	
	
	@Query(value = "select count(*) from procure.purchase_order_aud where rev = :poId ", nativeQuery=true)
	public Long findRevisionById(@Param("poId") int poId);
	
	public List<PurchaseOrder> findBySupplierIdAndSubsidiaryIdAndIsDeletedAndPoStatus(Long supplierId, Long subsidiaryId, boolean isDeleted, String poStatus);

	public Optional<PurchaseOrder> findByPoNumberAndIsDeleted(String poNumber, boolean isDeleted);

}
