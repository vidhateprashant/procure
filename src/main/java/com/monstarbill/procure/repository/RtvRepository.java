package com.monstarbill.procure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monstarbill.procure.models.Rtv;

public interface RtvRepository extends JpaRepository<Rtv, String>{
	public Optional<Rtv> findByIdAndIsDeleted(Long id, boolean isDeleted);
}
