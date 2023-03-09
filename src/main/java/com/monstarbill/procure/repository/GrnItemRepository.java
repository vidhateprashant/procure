package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.monstarbill.procure.models.GrnItem;

public interface GrnItemRepository extends JpaRepository<GrnItem, String> {

	List<GrnItem> findByGrnId(Long grnId);

	public Optional<GrnItem> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public List<GrnItem> findByGrnIdAndIsDeleted(Long grnId, boolean isDeleted);
	
	@Query("select gi FROM GrnItem gi where gi.grnId = :grnId and gi.isDeleted = :isDeleted and gi.reciveQuantity > gi.rtvQuantity ")
	public List<GrnItem> findByGrnIdWithQuantity(Long grnId, boolean isDeleted);
	
	@Query("select gi FROM GrnItem gi where gi.grnId = :grnId and gi.itemId = :itemId and gi.isDeleted = :isDeleted and gi.unbilledQuantity != 0 ")
	public GrnItem findByGrnIdAndItemIdAndIsDeleted(@Param("grnId")Long grnId, @Param("itemId")Long itemId, @Param("isDeleted") boolean isDeleted);

	List<GrnItem> findItemsByGrnNumber(String grnNumber);

	Double findReciveQuantityByPoIdAndItemId(Long poId, Long itemId);

	public List<GrnItem> getByGrnIdAndItemId(Long grnId, Long itemId);

	List<GrnItem> findByGrnIdAndItemId(Long grnId, Long itemId);

	@Query(" select count(1) from GrnItem WHERE unbilledQuantity > 0 AND grnId = :grnId and isDeleted = false ")
	public Long findUnprocessedItemsCountForGrn(Long grnId);
	
}
