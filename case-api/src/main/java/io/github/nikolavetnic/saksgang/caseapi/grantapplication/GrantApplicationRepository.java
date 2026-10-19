package io.github.nikolavetnic.saksgang.caseapi.grantapplication;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrantApplicationRepository extends JpaRepository<GrantApplication, UUID> {

	Page<GrantApplication> findByStatus(GrantApplicationStatus status, Pageable pageable);

}
