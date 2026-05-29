package me.jorlowski.repository;

import me.jorlowski.model.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SalonRepository extends JpaRepository<Salon, UUID>, JpaSpecificationExecutor<Salon> {
}
