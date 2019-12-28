package net.smiguel.stellar.support.constants;

import java.math.BigDecimal;

public class ApplicationConstants {

    public static final String GENERIC_ERROR = "an error occurred while processing your request, please try again";
    public static final String GENERIC_HEALTH_ERROR = "an error occurred while processing health check";

    public static final String SECURITY_GENERIC_ERROR = "User credentials not found";
    public static final String SECURITY_INVALID_TOKEN_ERROR = "Invalid token";
    public static final String SECURITY_INVALID_PARAMETERS_ERROR = "Invalid parameters";
    public static final String SECURITY_INVALID_PASSWORD_ERROR = "Invalid password";

    public static final String STELLAR_ACCOUNT_ERROR = "Customer without a valid Stellar account";
    public static final String STELLAR_GENERIC_ERROR = "Stellar Operation Error";
    public static final String STELLAR_INSUFFICIENT_BALANCE_ERROR = "There is no enough balance to place this offer";
    public static final String STELLAR_TIMEOUT_ERROR = "Stellar Network is temporarily out of service, please try again in a few minutes (timeout)";
    public static final int STELLAR_API_SEARCH_LIMIT = 50;


    public static final BigDecimal WALLET_MIN_DEPOSIT_CBB = BigDecimal.valueOf(10);
    public static final BigDecimal CONVERSION_RATE_EUR_TO_CBB = BigDecimal.valueOf(100);
    public static final BigDecimal CONVERSION_RATE_CBB_STROPS = BigDecimal.valueOf(10000000);
    public static final String CURRENCY_DEFAULT_SYMBOL = "EUR";

    public static final int CONVERSION_STELLAR_VALUE_MAX_DECIMALS = 7;
    public static final int CONVERSION_STELLAR_VALUE_MAX_DECIMALS_TO_PLACE_OFFER = 30;

    //Fixed rate only for demonstration purpose
    public static BigDecimal CONVERSION_RATE_XLM = BigDecimal.valueOf(0.04);

    public static final String APP_USERNAME = "smiguelnet";

    public static class Network {
        public static final long HTTP_CONN_SECONDS_TIMEOUT = 180;
        public static final long HTTP_READ_SECONDS_TIMEOUT = 60;
        public static final long HTTP_WRITE_SECONDS_TIMEOUT = 60;
    }
}
