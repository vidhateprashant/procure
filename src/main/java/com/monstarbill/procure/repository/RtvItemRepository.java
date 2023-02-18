package com.monstarbill.procure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.monstarbill.procure.models.RtvItem;

public interface RtvItemRepository extends JpaRepository<RtvItem, String>{
	
public List<RtvItem> findByRtvId(Long rtvId);
	
	public Optional<RtvItem> findById(Long id);

	public List<RtvItem> findByRtvIdAndIsDeleted(Long rtvId, boolean isDeleted);

	public Optional<RtvItem> findByIdAndIsDeleted(Long id, boolean isDeleted);

	@Query("SELECT SUM(amount) FROM RtvItem WHERE rtvId = :rtvId ")
	public Double findTotalEstimatedAmountForRtv(@Param("rtvId") Long rtvId);

}
