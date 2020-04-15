/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class WmtsTileMatrixSet {

    private final String identifier;
    private final String supportedCrs;
    private final List<WmtsTileMatrix> tileMatrixes;

    public WmtsTileMatrixSet(String identifier, String supportedCrs, Collection<WmtsTileMatrix> tileMatrixes) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("identifier must not be null or empty.");
        }

        if (supportedCrs == null || supportedCrs.isEmpty()) {
            throw new IllegalArgumentException("supportedCrs must not be null or empty.");
        }

        if (tileMatrixes == null || tileMatrixes.isEmpty()) {
            throw new IllegalArgumentException("tileMatrixes must not be null or an empty collection.");
        }

        this.identifier = identifier;
        this.supportedCrs = supportedCrs;
        this.tileMatrixes = tileMatrixes.stream()
                .sorted((m1, m2) -> m1.getScale() < m2.getScale() ? -1 : 1)
                .collect(Collectors.toList());
    }

    public final String getIdentifier() {
        return identifier;
    }

    public final String getSupportedCrs() {
        return supportedCrs;
    }

    public final List<WmtsTileMatrix> getTileMatrixes() {
        return tileMatrixes;
    }
}
