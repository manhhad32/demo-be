package com.example.demo.repository;

import com.example.demo.entity.TypeJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeJobRepository extends JpaRepository<TypeJob, Long> {
    TypeJob findByType(String type);
}
