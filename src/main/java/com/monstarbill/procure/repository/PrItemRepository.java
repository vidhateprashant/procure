package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.PrItem;

@Repository
public interface PrItemRepository extends JpaRepository<PrItem, String> {

	public Optional<PrItem> findByIdAndIsDeleted(Long id, boolean isDeleted);
	
	public List<PrItem> findAllByIsDeleted(boolean isDeleted);

	@Query("select new com.monstarbill.procure.models.PrItem(pi.id, pi.itemId, pi.prId, pi.prNumber, i.name, pi.itemDescription, i.uom, pi.quantity, pi.rate, pi.estimatedAmount, pi.receivedDate, pi.memo, pi.remainedQuantity) from PrItem pi INNER JOIN Item i ON i.id = pi.itemId WHERE pi.isDeleted is false AND pi.prId = :prId ")
	public List<PrItem> findByPrId(@Param("prId") Long prId);
	
	@Query("SELECT SUM(pi.estimatedAmount) FROM PrItem pi WHERE pi.isDeleted is false AND pi.prId = :prId")
	public Double findEstimatedAmountForPr(@Param("prId") Long prId);

	@Query(" select count(1) from PrItem WHERE remainedQuantity > 0 AND prId = :prId ")
	public Long findUnprocessedItemsCountForRfq(Long prId);
	
//	@Query("SELECT pi FROM PrItem pi INNER JOIN PurchaseRequisition pr ON pr.id = pi.prId WHERE pi.prId = prId AND pi.itemId = itemId AND prStatus != 'Draft' ")
	public Optional<PrItem> findByPrIdAndItemIdAndIsDeleted(Long prId, Long itemId, boolean isDeleted);

	@Query(" select count(1) from PrItem WHERE poId is null AND prId = :prId ")
	public Long findUnprocessedItemsCountByPo(Long prId);
	
	@Query(" select count(1) from PrItem WHERE rfqId != null AND prId = :prId ")
	public Long isRfqCreatedForPrId(Long prId);
	
	@Query(" select count(1) from PrItem WHERE poId != null AND prId = :prId ")
	public Long isPoCreatedForPrId(Long prId);

	@Query("SELECT count(1) FROM PrItem WHERE prId = :prId AND isDeleted != true AND quantity != remainedQuantity ")
	public Long findProcessedItemCountByPrId(Long prId);
	
	public List<PrItem> findByPrIdAndPoId(Long prId, Long poId);

	@Query(" select count(1) from PrItem WHERE isDeleted != true AND prId = :prId ")
	public Long findCountByPrId(Long prId);
}
