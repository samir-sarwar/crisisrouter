package com.crisisrouter.crisisRouter.repository;

import com.crisisrouter.crisisRouter.model.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;


public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    List<Claim> findByUserId(UUID volunteerId);

}
