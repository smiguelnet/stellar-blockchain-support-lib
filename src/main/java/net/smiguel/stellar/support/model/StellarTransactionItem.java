package net.smiguel.stellar.support.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.smiguel.stellar.support.enumerator.OfferType;
import net.smiguel.stellar.support.enumerator.StellarTransactionType;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StellarTransactionItem {

    private String id;
    private String limit;
    private String startingBalance;
    private String amount;
    private String assetType;
    private String assetCode;
    private String assetIssuer;
    private String accountId;
    private String type;
    private String pagingToken;
    private String publicKeyForSignerOperation;

    private String tradeBoughtAmount;
    private String tradeBoughtAsset;
    private String tradeOfferId;
    private String tradeSeller;
    private String tradeSellerUsername;
    private Boolean showTradeSellerUsername;
    private String tradeSoldAmount;
    private String tradeSoldAsset;

    private String offerBuyingAsset;
    private String offerBuyingRiderName;
    private String offerBuyingTeamName;
    private Long offerBuyingSeasonYear;
    private String offerBuyingCategory;
    private String offerBuyingCollection;

    private String offerSellingAsset;
    private String offerSellingRiderName;
    private String offerSellingTeamName;
    private Long offerSellingSeasonYear;
    private String offerSellingCategory;
    private String offerSellingCollection;

    private String offerSellerAccount;
    private String offerAmount;
    private String offerPrice;

    private Date timestamp;
    private String date;
    private String time;

    private StellarTransactionType transactionType;
    private OfferType offerType;
}