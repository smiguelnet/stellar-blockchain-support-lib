package net.smiguel.stellar.support.helper;

import net.smiguel.stellar.support.constants.ApplicationConstants;
import org.apache.commons.lang3.StringUtils;
import org.stellar.sdk.xdr.XdrDataInputStream;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Base64;

public class DataHelper {

    public static BigDecimal valueLumenInCbb(BigDecimal value) {
        return value.multiply(ApplicationConstants.CONVERSION_RATE_XLM).setScale(0, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal valueLumenInStrop(BigDecimal value) {
        return valueCbbInStrop(valueLumenInCbb(value));
    }

    public static Long valueEuroToStripe(String value) {
        return new BigDecimal(value)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, BigDecimal.ROUND_HALF_UP)
                .longValue();
    }

    public static Long valueEuroToStripe(BigDecimal value) {
        return value
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, BigDecimal.ROUND_HALF_UP)
                .longValue();
    }

    public static BigDecimal valueEuroInCbb(BigDecimal value) {
        return value.multiply(ApplicationConstants.CONVERSION_RATE_EUR_TO_CBB).setScale(0, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal valueCbbInEuro(BigDecimal value) {
        return value.divide(ApplicationConstants.CONVERSION_RATE_EUR_TO_CBB).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal valueEuroInXlm(BigDecimal value) {
        return value.divide(ApplicationConstants.CONVERSION_RATE_XLM, BigDecimal.ROUND_HALF_UP).setScale(ApplicationConstants.CONVERSION_STELLAR_VALUE_MAX_DECIMALS, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal valueXlmInEuro(BigDecimal value) {
        return value.multiply(ApplicationConstants.CONVERSION_RATE_XLM).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal valueCbbInStrop(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.divide(ApplicationConstants.CONVERSION_RATE_CBB_STROPS).setScale(ApplicationConstants.CONVERSION_STELLAR_VALUE_MAX_DECIMALS, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal valueCbbInStropPlaceOffer(BigDecimal value) {
        return value.divide(ApplicationConstants.CONVERSION_RATE_CBB_STROPS).setScale(ApplicationConstants.CONVERSION_STELLAR_VALUE_MAX_DECIMALS_TO_PLACE_OFFER, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal valueStropInCbb(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.multiply(ApplicationConstants.CONVERSION_RATE_CBB_STROPS).setScale(0, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal valueCbb(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return new BigDecimal(value).setScale(0, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal valueEuro(String value) {
        return new BigDecimal(value).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal valueStrop(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return new BigDecimal(value).setScale(7, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal valueXlm(String value) {
        return new BigDecimal(value).setScale(7, BigDecimal.ROUND_HALF_UP);
    }

    public static XdrDataInputStream getDataInputStreamFromBase64(String data) {
        return new XdrDataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)));
    }
}
