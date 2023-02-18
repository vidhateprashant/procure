package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.QuotationGenaralInfo;

@Repository
public interface QuotationGeneralInfoRepository extends JpaRepository<QuotationGenaralInfo, String> {

	public List<QuotationGenaralInfo> findAllByQuotationIdAndIsDeleted(Long quotationId, boolean isDeleted);

//	public List<QuotationGenaralInfo> findAllByQuotationNumberAndIsDeleted(String quotationNumber, boolean isDeleted);

	public Optional<QuotationGenaralInfo> findByIdAndIsDeleted(Long id, boolean isDeleted);

}
