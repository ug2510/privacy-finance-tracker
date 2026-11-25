package com.privacyfinance.backend.repository;

import com.privacyfinance.backend.model.InvestmentHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link InvestmentHolding}.
 *
 * The composite key (isin + folioNumber) uniquely identifies a holding,
 * which is what we use for upserts in CasService.
 */
@Repository
public interface InvestmentHoldingRepository extends JpaRepository<InvestmentHolding, Long> {

    /** Used by CasService to find an existing holding before deciding to insert or update. */
    Optional<InvestmentHolding> findByIsinAndFolioNumber(String isin, String folioNumber);
}
