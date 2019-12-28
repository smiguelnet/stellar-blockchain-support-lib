package net.smiguel.stellar.support.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.smiguel.stellar.support.serializer.JsonDateTimeISO8601Deserializer;
import net.smiguel.stellar.support.serializer.JsonDateTimeISO8601Serializer;

import java.math.BigDecimal;
import java.util.Calendar;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StellarBalanceXlmItem {

    private String operation;
    private BigDecimal creditCustomer;
    private BigDecimal debitCustomer;
    private BigDecimal creditPortal;
    private BigDecimal debitPortal;

    @JsonSerialize(using = JsonDateTimeISO8601Serializer.class)
    @JsonDeserialize(using = JsonDateTimeISO8601Deserializer.class)
    private Calendar date;
}
