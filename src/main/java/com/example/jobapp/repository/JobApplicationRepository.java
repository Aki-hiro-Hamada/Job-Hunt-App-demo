package com.example.jobapp.repository;

import com.example.jobapp.entity.JobApplication;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findAllByOwnerUserId(String ownerUserId, Sort sort);

    Optional<JobApplication> findByIdAndOwnerUserId(Long id, String ownerUserId);

    long deleteByIdAndOwnerUserId(Long id, String ownerUserId);

    long countByOwnerUserIdAndStatus(String ownerUserId, String status);
}
