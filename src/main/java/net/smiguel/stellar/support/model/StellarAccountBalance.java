package net.smiguel.stellar.support.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.smiguel.stellar.support.enumerator.StellarAssetType;

import java.util.Calendar;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StellarAccountBalance extends BaseModel {

    private Long stellarAccountId;
    private StellarAssetType assetType;
    private String assetTypeDescription;
    private String balance;
    private String buyingLiabilities;
    private String sellingLiabilities;
    private Calendar balanceDate;
    private String balanceAvailable;
    private String issuerAccountId;
    private Integer signers;
}
