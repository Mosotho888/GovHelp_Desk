package za.gov.helpdesk.asset.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import za.gov.helpdesk.users.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An IT asset tracked by the help desk: a laptop, desktop, printer, monitor, piece of networking
 * equipment, or software license. Assets can be linked to the tickets raised against them (see
 * {@link TicketAsset}) to give technicians device history, and carry warranty and ownership details
 * so that information doesn't need to live in a separate spreadsheet.
 */
@Setter
@Getter
@Entity
@Table(name = "ASSETS")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable inventory tag (e.g. "AST-000042"). Auto-generated from the id after first save
     * when not supplied by the caller, but can be set explicitly to match a pre-existing physical
     * barcode/tag scheme.
     */
    @Column(name = "asset_tag", nullable = false, unique = true, length = 50)
    private String assetTag;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AssetStatus status = AssetStatus.IN_STORAGE;

    @Column(name = "serial_number", unique = true, length = 100)
    private String serialNumber;

    @Column(length = 100)
    private String manufacturer;

    @Column(length = 100)
    private String model;

    /** The person currently using/responsible for this asset - "ownership" for reporting. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @Column(length = 150)
    private String location;

    @Column(length = 150)
    private String vendor;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_cost", precision = 12, scale = 2)
    private BigDecimal purchaseCost;

    @Column(name = "warranty_expiry_date")
    private LocalDate warrantyExpiryDate;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
