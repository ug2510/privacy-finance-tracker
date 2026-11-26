package com.privacyfinance.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "investment_holdings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"isin", "folio_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String isin;

    @Column(name = "scheme_name", nullable = false)
    private String schemeName;

    @Column(name = "folio_number", nullable = false)
    private String folioNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal units;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal nav;

    @Column(name = "current_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}
