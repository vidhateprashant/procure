package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.PurchaseOrderItem;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, String> {

	public Optional<PurchaseOrderItem> findByIdAndIsDeleted(Long id, boolean isDeleted);

	
	@Query(" select "
			+ "new com.monstarbill.procure.models.PurchaseOrderItem(poi.id, poi.poNumber, poi.poId, poi.itemId, poi.quantity, poi.rate, "
			+ " case  "
			+ " 	when lower(i.category) = lower('Inventory Item')  "
			+ " 	then i.assetAccountId  "
			+ " 	else i.expenseAccountId "
			+ " end as accountCode, "
			+ " poi.amount, poi.taxGroupId, poi.taxAmount, poi.receivedByDate, poi.prId, poi.shipToLocationId, poi.shipToLocation, poi.department, poi.memo, i.name, "
			+ " case  "
			+ " 	when lower(po.poType) = lower('PR Based')  "
			+ " 	then pritem.itemDescription  "
			+ " 	else i.description "
			+ " end as itemDescription, "
			+ " i.uom, i.integratedId, pritem.prNumber, poi.remainQuantity, pritem.remainedQuantity) "
			+ " FROM PurchaseOrderItem poi "
			+ " INNER JOIN PurchaseOrder po ON po.id = poi.poId "
			+ " INNER JOIN Item i ON i.id = poi.itemId "
			+ " LEFT JOIN PrItem pritem ON poi.prId = pritem.prId AND poi.itemId = pritem.itemId "
			+ " WHERE poi.poId = :poId AND poi.isDeleted = false ")
	public List<PurchaseOrderItem> findItemsByPoId(@Param("poId") Long poId);

	@Query("SELECT SUM(pi.amount) FROM PurchaseOrderItem pi WHERE pi.isDeleted is false AND pi.poId = :poId")
	public Double findByPoIdForApproval(@Param("poId") Long poId);

	@Query("select new com.monstarbill.procure.models.PurchaseOrderItem(poi.id, poi.poNumber, poi.taxGroupId, poi.poId, poi.itemId,poi.remainQuantity, poi.quantity, poi.rate, i.name as itemName, poi.itemDescription as itemDescription, i.uom as itemUom) "
			+ " from PurchaseOrderItem poi inner join Item i ON i.id = poi.itemId WHERE poi.poId = :poId AND i.natureOfItem = :itemNature AND (poi.remainQuantity is null or poi.remainQuantity != 0.0) AND poi.isDeleted = :isDeleted")
	public List<PurchaseOrderItem> getAllItemByPoAndIsDeleted(@Param("poId") Long poId, @Param("itemNature") String itemNature, @Param("isDeleted") boolean isDeleted);

	@Query("SELECT SUM(totalAmount) FROM PurchaseOrderItem WHERE poId = :poId ")
	public Double findTotalEstimatedAmountForPo(Long poId);
	
	public List<PurchaseOrderItem> findByPoIdAndIsDeleted(Long poId, boolean isDeleted);
	
	@Query("select poi FROM PurchaseOrderItem poi where poi.poId = :poId and poi.itemId = :itemId and "
			+ " poi.isDeleted = :isDeleted and poi.unbilledQuantity != 0 ")
	public PurchaseOrderItem findByPoIdAndItemIdAndIsDeleted(@Param("poId")Long poId, @Param("itemId")Long itemId, @Param("isDeleted")boolean isDeleted);

	@Query(" SELECT sum(poi.quantity) "
			+ " FROM PurchaseOrder po "
			+ " LEFT JOIN PurchaseOrderItem poi ON po.id = poi.poId " 
			+ " WHERE po.qaId = :qaId GROUP BY po.qaId ")
	public Double findQuantityByQaId(Long qaId);

	@Query(" select new com.monstarbill.procure.models.PurchaseOrderItem(po.id, po.poNumber, po.qaId, poi.quantity) " + 
			" FROM PurchaseOrder po " + 
			" inner join PurchaseOrderItem poi ON po.id = poi.poId " + 
			" where po.qaId = :qaId and poi.itemId = :itemId ")
	public List<PurchaseOrderItem> findItemByQaIdAndItem(Long qaId, Long itemId);

	public Optional<PurchaseOrderItem> getByPoId(Long id);

	//public PurchaseOrderItem findByPoIdAndItemId(Long poId, Long itemId, Long poiId);

	@Query(" SELECT (poi.remainQuantity) FROM PurchaseOrderItem poi where poi.poId = :poId and poi.itemId = :itemId ")
	public Double findRemainQuantityByPoIdAndItemId(Long poId, Long itemId);

	public PurchaseOrderItem findByPoIdAndItemIdAndId(Long poId, Long itemId, Long poiId);

	public PurchaseOrderItem findById(Long poiId);

	public Double findReciveQuantityByPoIdAndItemId(Long poId, Long itemId);
	
	public List<PurchaseOrderItem> findByPoIdAndPrId(Long poId, Long prId);

	@Query("select new com.monstarbill.procure.models.PurchaseOrderItem(poi.id, poi.poNumber, poi.taxGroupId, poi.poId, poi.itemId,poi.remainQuantity, poi.quantity, poi.rate, i.name as itemName, poi.itemDescription as itemDescription, i.uom as itemUom) "
			+ " from PurchaseOrderItem poi inner join Item i ON i.id = poi.itemId WHERE poi.poId = :poId AND (poi.remainQuantity is null or poi.remainQuantity != 0.0) AND poi.isDeleted = :isDeleted")
	public List<PurchaseOrderItem> getAllItemByPoAndIsDeleted(@Param("poId") Long poId, @Param("isDeleted") boolean isDeleted);
}
