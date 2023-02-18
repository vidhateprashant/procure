package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monstarbill.procure.models.Grn;

public interface GrnRepository extends JpaRepository<Grn, String> {

	public Optional<Grn> findByIdAndIsDeleted(Long id, boolean isDeleted);

	public List<Grn> findByPoIdAndIsDeleted(Long poId, boolean isDeleted);

	public Optional<Grn> findByGrnNumberAndIsDeleted(String grnNumber, boolean isDeleted);

	public List<Grn> findBySubsidiaryId(Long subsidiaryId);

	public List<Grn> findBySubsidiaryIdAndStatusNot(Long subsidiaryId, String status);

}
