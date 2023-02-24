package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.QuotationAnalysisItem;

@Repository
public interface QuotationAnalysisItemRepository extends JpaRepository<QuotationAnalysisItem, String> {

	public Optional<QuotationAnalysisItem> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public List<QuotationAnalysisItem> findByQaIdAndIsDeleted(Long qaId, boolean isDeleted);
	
	public List<QuotationAnalysisItem> findByQaIdAndItemIdAndApprovedSupplierAndIsDeleted(Long qaId, Long itemId, Long approvedSupplier, boolean isDeleted);

	@Query(" SELECT distinct approvedSupplier FROM QuotationAnalysisItem WHERE qaId = :qaId AND isAwarded = true ")
	public List<Long> findSupplierByQaId(Long qaId);

	@Query(" SELECT distinct prId FROM QuotationAnalysisItem WHERE qaId = :qaId ")
	public List<Long> findPrIdsByQaId(Long qaId);
	
	@Query(" SELECT distinct prLocationId FROM QuotationAnalysisItem WHERE qaId = :qaId AND approvedSupplier = :supplierId ")
	public List<Long> findLocationsByQaIdAndSupplier(Long qaId, Long supplierId);

	@Query(" select new com.monstarbill.procure.models.QuotationAnalysisItem(qai.id, qai.qaNumber, qai.itemId, i.name, i.description, i.integratedId,  " + 
			" case " + 
			"	when lower(i.category) = lower('Inventory Item') " + 
			"	then i.assetAccountId   " + 
			"	else i.expenseAccountId  " + 
			" end as accountCode,  " + 
			" qai.uom, qai.quantity, qai.ratePerUnit, qai.actualRate, qai.recievedDate, qai.poId, qai.expectedDate) " + 
			" FROM QuotationAnalysisItem qai " + 
			" INNER JOIN Item i ON qai.itemId = i.id and i.isActive is true " + 
			" WHERE qai.qaId = :qaId AND qai.isAwarded is true AND qai.approvedSupplier = :supplierId AND qai.prLocationId = :locationId ")
	public List<QuotationAnalysisItem> findItemsByQaAndSupplierAndLocation(Long qaId, Long supplierId, Long locationId);

	@Query(" select new com.monstarbill.procure.models.QuotationAnalysisItem(qa.id, qa.qaNumber, sum(qai.quantity)) from QuotationAnalysis qa "
			+ " inner join QuotationAnalysisItem qai ON qa.id = qai.qaId "
			+ " where qa.isDeleted is false AND qa.subsidiaryId = :subsidiaryId "
			+ " group by qa.id, qa.qaNumber order by qa.id ")
	public List<QuotationAnalysisItem> findQaNumbersWithQuantity(Long subsidiaryId);
}
