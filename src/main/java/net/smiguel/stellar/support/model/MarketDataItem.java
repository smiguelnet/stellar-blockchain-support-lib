package net.smiguel.stellar.support.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.smiguel.stellar.support.serializer.JsonDateISO8601Deserializer;
import net.smiguel.stellar.support.serializer.JsonDateISO8601Serializer;

import java.math.BigDecimal;
import java.util.Calendar;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MarketDataItem {

    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private long volume;
    private int count;

    @JsonSerialize(using = JsonDateISO8601Serializer.class)
    @JsonDeserialize(using = JsonDateISO8601Deserializer.class)
    private Calendar date;
}
