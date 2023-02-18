package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.procure.models.QuotationItemVendor;

@Repository
public interface QuotationItemVendorRepository extends JpaRepository<QuotationItemVendor, String> {

	public Optional<QuotationItemVendor> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public Optional<QuotationItemVendor> findByVendorIdAndQuotationIdAndIsDeleted(Long vendorId, Long quotationId, boolean isDeleted);

	public List<QuotationItemVendor> findByItemIdAndQuotationIdAndIsDeleted(Long itemId, Long quotationId, boolean isDeleted);

}
