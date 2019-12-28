package net.smiguel.stellar.support.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.smiguel.stellar.support.enumerator.StellarAssetType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StellarOperationItem {

    private StellarAssetType stellarAssetType;
    private StellarAccount stellarAssetIssuer;
    private StellarAccount stellarAssetDistribution;
    private String assetCode;
    private String amount;
}
