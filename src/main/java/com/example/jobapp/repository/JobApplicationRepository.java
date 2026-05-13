package com.example.jobapp.repository;

import com.example.jobapp.entity.JobApplication;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {

    List<JobApplication> findAllByOwnerUserId(String ownerUserId, Sort sort);

    Optional<JobApplication> findByIdAndOwnerUserId(String id, String ownerUserId);

    long deleteByIdAndOwnerUserId(String id, String ownerUserId);

    long countByOwnerUserIdAndStatus(String ownerUserId, String status);
}
