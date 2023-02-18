package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.QuotationAnalysis;

@Repository
public interface QuotationAnalysisRepository extends JpaRepository<QuotationAnalysis, String> {

	public Optional<QuotationAnalysis> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public Optional<QuotationAnalysis> findByRfqIdAndIsDeleted(Long rfqId, boolean isDeleted);

	@Query(" select new com.monstarbill.procure.models.QuotationAnalysis(qa.id, qa.qaNumber) from PurchaseRequisition pr "
			+ " INNER JOIN QuotationPr qp ON pr.id = qp.prId "
			+ " INNER JOIN QuotationAnalysis qa ON qp.quotationId = qa.rfqId "
			+ " where pr.id IN :prIds ")
	public List<QuotationAnalysis> getQaNumberByPrIds(@Param("prIds") List<Long> prIds);

	@Query(" select distinct approvedSupplier FROM QuotationAnalysisItem WHERE qaId IN :qaIds ")
	public List<Long> getApprovedSupplierByQaIds(@Param("qaIds") List<Long> qaId);

//	@Query(" select distinct qaNumber FROM QuotationAnalysis WHERE subsidiaryId = :subsidiaryId AND isDeleted = :isDeleted ")
//	public List<String> findQaNumbersBySubsidiaryIdAndIsDeleted(Long subsidiaryId, boolean isDeleted);

	public Optional<QuotationAnalysis> findByQaNumberAndIsDeleted(String qaNumber, boolean isDeleted);

}
