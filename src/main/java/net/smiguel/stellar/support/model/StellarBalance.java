package net.smiguel.stellar.support.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.smiguel.stellar.support.serializer.JsonDateISO8601Deserializer;
import net.smiguel.stellar.support.serializer.JsonDateISO8601Serializer;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StellarBalance {

    private long icons;
    private long boxes;
    private long packsToBuy;

    @JsonSerialize(using = JsonDateISO8601Serializer.class)
    @JsonDeserialize(using = JsonDateISO8601Deserializer.class)
    private Calendar firstBox;

    private long iconsNonDuplicated;
    private long activeOffers;
    private long activeOffersHoldCBB;
    private long trustlines;
    private long trustlinesWithBalanceZero;
    private long signers;
    private BigDecimal minXlm;
    private long currentBalanceCBB;
    private long availableBalanceCBB;
    private long forecastBalanceCBB;
    private BigDecimal currentBalanceXLM;
    private BigDecimal availableBalanceXLM;
    private BigDecimal xlmDepositedByPortal;
    private BigDecimal xlmDepositedByCustomer;

    @JsonSerialize(using = JsonDateISO8601Serializer.class)
    @JsonDeserialize(using = JsonDateISO8601Deserializer.class)
    private Calendar registration;

    private List<StellarBalanceXlmItem> balanceXlmDetail;
    private List<StellarAccountBalance> balanceIconsDetail;
    private BigDecimal availableToTrade;
    private long tradingSlots;
    private long totalValue;
}
