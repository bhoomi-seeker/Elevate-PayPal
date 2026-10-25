package com.paybridge.service;

import com.paybridge.dto.EvidenceItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of documented provider capabilities and bilateral agreements.
 *
 * IMPORTANT LIMITATIONS:
 * - A test fixture does not establish real-world provider support.
 * - Compatibility evaluations reflect documented technical and regulatory capabilities only.
 */
@Component
public class EvidenceStore {

    public record ProviderEvidenceRecord(
            String providerKey,
            String supportedCountry, // uppercase 2-letter ISO country code or "*" for all
            boolean coversQrPaymentScenario,
            boolean activeAndValid,
            boolean conflicting,
            String conditions, // null if fully compatible
            List<EvidenceItem> evidenceItems
    ) {
    }

    private final Map<String, ProviderEvidenceRecord> store = new ConcurrentHashMap<>();

    public EvidenceStore() {
        loadDefaultDocumentedFixtures();
    }

    private void loadDefaultDocumentedFixtures() {
        // COMP-01: Documented bilateral linkage with Singapore (SG) for upi-global
        registerRecord(new ProviderEvidenceRecord(
                "upi-global",
                "SG",
                true,
                true,
                false,
                null,
                List.of(new EvidenceItem(
                        "NPCI International - PayNow Bilateral Agreement 2023",
                        "Direct bilateral linkage confirms routing support between Singapore and Indian UPI merchants.",
                        "SUPPORTED",
                        true
                ))
        ));

        // COMP-02: Conditional support requiring acquiring bank enablement (France / FR)
        registerRecord(new ProviderEvidenceRecord(
                "rupay-global",
                "FR",
                true,
                true,
                false,
                "Merchant terminal acquiring bank must have international UPI acceptance enabled; payer bank must permit cross-border debit.",
                List.of(new EvidenceItem(
                        "Lyra Network & NPCI International Agreement",
                        "Cross-border acceptance enabled conditionally for enrolled merchant acquiring terminals.",
                        "CONDITIONAL",
                        true
                ))
        ));

        // COMP-04: Historical document that is expired / outdated
        registerRecord(new ProviderEvidenceRecord(
                "legacy-provider",
                "*",
                true,
                false,
                false,
                null,
                List.of(new EvidenceItem(
                        "Pilot Cross-Border Memorandum 2021",
                        "Memorandum of understanding expired in 2023. No current bilateral routing agreement active.",
                        "UNSUPPORTED",
                        false
                ))
        ));

        // COMP-05: Provider capability covers e-commerce only, not in-person UPI QR payments
        registerRecord(new ProviderEvidenceRecord(
                "ecommerce-only-provider",
                "*",
                false,
                true,
                false,
                null,
                List.of(new EvidenceItem(
                        "Digital Wallet E-Commerce Gateway Guidelines",
                        "Provider supports web checkout redirects, but does not support in-person UPI QR payment routing.",
                        "UNSUPPORTED",
                        true
                ))
        ));

        // COMP-06: Conflicting evidence sources
        registerRecord(new ProviderEvidenceRecord(
                "conflict-provider",
                "*",
                true,
                true,
                true,
                null,
                List.of(
                        new EvidenceItem(
                                "Bilateral Routing Technical Specification 2024",
                                "Technical protocol specification defined for merchant settlement.",
                                "SUPPORTED",
                                true
                        ),
                        new EvidenceItem(
                                "Foreign Exchange Regulatory Advisory 2025",
                                "Cross-border merchant settlement suspended pending compliance review.",
                                "UNSUPPORTED",
                                true
                        )
                )
        ));
    }

    public void registerRecord(ProviderEvidenceRecord record) {
        String key = buildKey(record.providerKey(), record.supportedCountry());
        store.put(key, record);
    }

    public Optional<ProviderEvidenceRecord> findEvidence(String provider, String country) {
        if (provider == null) {
            return Optional.empty();
        }
        String normalizedProvider = provider.trim().toLowerCase();
        String normalizedCountry = country != null ? country.trim().toUpperCase() : "";

        // First check exact provider + country match
        String exactKey = buildKey(normalizedProvider, normalizedCountry);
        if (store.containsKey(exactKey)) {
            return Optional.of(store.get(exactKey));
        }

        // Fallback to wildcard country match
        String wildcardKey = buildKey(normalizedProvider, "*");
        if (store.containsKey(wildcardKey)) {
            return Optional.of(store.get(wildcardKey));
        }

        return Optional.empty();
    }

    private String buildKey(String provider, String country) {
        return provider.trim().toLowerCase() + "::" + country.trim().toUpperCase();
    }

    public void resetToDefaults() {
        store.clear();
        loadDefaultDocumentedFixtures();
    }
}
