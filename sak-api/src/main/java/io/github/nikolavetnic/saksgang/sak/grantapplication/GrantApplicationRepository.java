package io.github.nikolavetnic.saksgang.sak.grantapplication;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrantApplicationRepository extends JpaRepository<GrantApplication, UUID> {
}
