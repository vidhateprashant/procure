package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.QuotationVendors;

@Repository
public interface QuotationVendorRepository extends JpaRepository<QuotationVendors, String> {

	public Optional<QuotationVendors> findByIdAndIsDeleted(Long id, boolean isDeleted);

	@Query(" SELECT distinct email FROM QuotationVendors WHERE quotationId = :rfqId AND isDeleted = :isDeleted AND email is not null AND email != '' ")
	public List<String> findDistinctMailByRfqId(Long rfqId, boolean isDeleted);
	
//	public Optional<QuotationVendors> findByVendorIdAndRfqNumberAndIsDeleted(Long vendorId, String rfqNumber, boolean isDeleted);
	public Optional<QuotationVendors> findByVendorIdAndQuotationIdAndIsDeleted(Long vendorId, Long quotationId, boolean isDeleted);

	@Query(" SELECT new com.monstarbill.procure.models.QuotationVendors(qv.id, qv.quotationId, qv.rfqNumber, qv.vendorId, s.name, qv.contactName, qv.email, qv.memo) "
			+ "FROM QuotationVendors qv "
			+ "INNER JOIN Supplier s ON s.id = qv.vendorId "
			+ "WHERE qv.quotationId = :quotationId AND qv.isDeleted = :isDeleted ")
	public List<QuotationVendors> getAllVendorsByQuotationIdAndIsDeleted(@Param("quotationId") Long quotationId, @Param("isDeleted") boolean isDeleted);

}
