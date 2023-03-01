package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.monstarbill.procure.models.GrnItem;

public interface GrnItemRepository extends JpaRepository<GrnItem, String> {

//	@Query("select new com.monster.bill.models.GrnItem(gi.id, gi.grnId, gi.itemId, gi.grnNumber, "
//			+ " gi.reciveQuantity, gi.remainQuantity, gi.lotNumber, gi.rate, gi.rtvQuantity, gi.invoiceNumber, "
//			+ " i.name as itemName, i.description as itemDescription, i.uom as itemUom, poi.quantity as quantity, gi.taxGroupId) FROM GrnItem gi "
//			+ " inner join Item i ON i.id = gi.itemId left join PurchaseOrderItem poi ON poi.poNumber = gi.poNumber where gi.grnId = :grnId ")
	List<GrnItem> findByGrnId(Long grnId);

	public Optional<GrnItem> findByIdAndIsDeleted(Long id, boolean isDeleted);

//	@Query("select new com.monster.bill.models.GrnItem(gi.id, gi.grnId, gi.itemId, gi.grnNumber, "
//			+ " gi.reciveQuantity, gi.remainQuantity, gi.lotNumber, gi.rate, gi.rtvQuantity, gi.invoiceNumber, "
//			+ " i.name as itemName, i.description as description, i.uom as uom, poi.quantity as orderQuantity) FROM GrnItem gi "
//			+ " inner join Item i ON i.id = gi.itemId left join PurchaseOrderItem poi ON poi.poNumber = gi.poNumber ")
//	List<GrnItem> findItemsByGrnNumber(@Param("grnNumber") String grnNumber);
	
	public List<GrnItem> findByGrnIdAndIsDeleted(Long grnId, boolean isDeleted);
	
	@Query("select gi FROM GrnItem gi where gi.grnId = :grnId and gi.isDeleted = :isDeleted and gi.reciveQuantity > gi.rtvQuantity ")
	public List<GrnItem> findByGrnIdWithQuantity(Long grnId, boolean isDeleted);
	
	@Query("select gi FROM GrnItem gi where gi.grnId = :grnId and gi.itemId = :itemId and gi.isDeleted = :isDeleted and gi.unbilledQuantity != 0 ")
	public GrnItem findByGrnIdAndItemIdAndIsDeleted(@Param("grnId")Long grnId, @Param("itemId")Long itemId, @Param("isDeleted") boolean isDeleted);

	List<GrnItem> findItemsByGrnNumber(String grnNumber);

	Double findReciveQuantityByPoIdAndItemId(Long poId, Long itemId);

	public List<GrnItem> getByGrnIdAndItemId(Long grnId, Long itemId);

	List<GrnItem> findByGrnIdAndItemId(Long grnId, Long itemId);


	
}
