package com.awscalculator.backend.pricing;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * On-demand AWS pricing, per region. Each region's numbers are loaded at class-init from
 * {@code classpath:pricing/<region>-pricing.json} — a snapshot fetched from AWS's real, public
 * Price List Bulk API by {@code scripts/fetch_pricing.sh}, not hand-typed guesses. Re-run that
 * script and restart the app to refresh these numbers.
 */
public final class PricingCatalog {

    public static final List<String> SUPPORTED_REGIONS = List.of(
            "ap-northeast-2", "ap-northeast-1", "us-east-1", "us-west-2", "eu-central-1"
    );
    public static final String DEFAULT_REGION = "ap-northeast-2";

    /** FX rate, not AWS pricing — same across regions, kept separate from the fetched snapshots. */
    public static final BigDecimal USD_TO_KRW = BigDecimal.valueOf(1380);

    private static final Map<String, PricingSnapshot> SNAPSHOTS = loadAll();

    public static PricingSnapshot forRegion(String region) {
        PricingSnapshot snapshot = SNAPSHOTS.get(region);
        if (snapshot == null) {
            throw new IllegalArgumentException(
                    "Unsupported region: " + region + ". Supported: " + SUPPORTED_REGIONS);
        }
        return snapshot;
    }

    private static Map<String, PricingSnapshot> loadAll() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, PricingSnapshot> map = new HashMap<>();
        for (String region : SUPPORTED_REGIONS) {
            String path = "/pricing/" + region + "-pricing.json";
            try (InputStream in = PricingCatalog.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException(
                            path + " not found on classpath — run scripts/fetch_pricing.sh first");
                }
                map.put(region, mapper.readValue(in, PricingSnapshot.class));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load " + path, e);
            } catch (JacksonException e) {
                throw new IllegalStateException("Failed to parse " + path, e);
            }
        }
        return Map.copyOf(map);
    }

    private PricingCatalog() {
    }
}
