package net.smiguel.stellar.support.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.smiguel.stellar.support.enumerator.OfferStatus;
import net.smiguel.stellar.support.enumerator.OfferType;

import java.util.Calendar;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StellarOffer {
    private Long offerId;
    private StellarAccount sourceAccount;
    private OfferType offerType;
    private OfferStatus offerStatus;
    private Calendar expiration;
    private String notificationTitle;
    private String notificationText;
    private Long notificationAssetId;
    private User userPlacedOffer;

    /**
     * Asset the offer creator is selling.
     */
    private StellarOperationItem sellingAsset;

    /**
     * Asset the offer creator is buying
     */
    private StellarOperationItem acceptingAsset;
    /**
     * Amount of selling being sold. Set to 0 if you want to delete an existing offer
     */

    private String sellingAmount;
    /**
     * Price of 1 unit of selling in terms of buying. For example, if you wanted to sell 30 XLM and buy 5 BTC, the price would be {5,30}.
     */
    private String sellingPrice;
}
