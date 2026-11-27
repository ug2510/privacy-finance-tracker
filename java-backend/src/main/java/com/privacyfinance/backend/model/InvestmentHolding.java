package com.privacyfinance.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "investment_holdings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"isin", "folio_number"}))
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

    @Column(name = "asset_type", nullable = false)
    private String assetType;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    // No-arg constructor required by JPA
    public InvestmentHolding() {}

    public InvestmentHolding(Long id, String isin, String schemeName, String folioNumber,
                             BigDecimal units, BigDecimal nav, BigDecimal currentValue,
                             String assetType, LocalDateTime lastUpdated) {
        this.id = id;
        this.isin = isin;
        this.schemeName = schemeName;
        this.folioNumber = folioNumber;
        this.units = units;
        this.nav = nav;
        this.currentValue = currentValue;
        this.assetType = assetType;
        this.lastUpdated = lastUpdated;
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }
    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }
    public String getFolioNumber() { return folioNumber; }
    public void setFolioNumber(String folioNumber) { this.folioNumber = folioNumber; }
    public BigDecimal getUnits() { return units; }
    public void setUnits(BigDecimal units) { this.units = units; }
    public BigDecimal getNav() { return nav; }
    public void setNav(BigDecimal nav) { this.nav = nav; }
    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}