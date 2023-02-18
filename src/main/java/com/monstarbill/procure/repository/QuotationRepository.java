package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.Quotation;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, String> {

	public Optional<Quotation> findByIdAndIsDeleted(Long id, boolean isDeleted);

//	public List<Quotation> findByRfqNumberAndIsDeleted(String rfqNumber, boolean isDeleted);
	
	@Query(" select new com.monstarbill.procure.models.Quotation(q.id, q.rfqNumber, q.rfqDate, q.bidType, q.bidOpenDate, q.bidCloseDate, s.name) "
			+ " from Quotation q  "
			+ " INNER JOIN Subsidiary s ON q.subsidiaryId = s.id WHERE q.isDeleted = :isDeleted ")
	public List<Quotation> findAllByIsDeleted(@Param("isDeleted") boolean isDeleted, Pageable pageable);
	
	@Query(" select count(q) "
			+ " from Quotation q  "
			+ " INNER JOIN Subsidiary s ON q.subsidiaryId = s.id WHERE q.isDeleted = :isDeleted ")
	public Long findCountByIsDeleted(@Param("isDeleted") boolean isDeleted);

//	public List<Quotation> findByRfqNumberAndStatusAndIsDeleted(String rfqNumber, String status, boolean isDeleted);

	@Query(" select q from Quotation q INNER JOIN QuotationPr qp ON q.id = qp.quotationId WHERE qp.prId = :prId AND q.status = :status AND q.isDeleted = :isDeleted ")
	public List<Quotation> findByPrIdAndStatusAndIsDeleted(@Param("prId") Long prId, @Param("status") String status, @Param("isDeleted") boolean isDeleted);

	public List<Quotation> findBySubsidiaryIdAndStatusNotIn(Long subsidiaryId, List<String> status);

}
