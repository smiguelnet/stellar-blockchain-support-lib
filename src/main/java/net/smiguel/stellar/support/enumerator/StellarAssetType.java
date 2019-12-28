package net.smiguel.stellar.support.enumerator;

import lombok.ToString;

@ToString
public enum StellarAssetType {
    XLM(1, "native", "XLM", "1000000"),
    CBB(2, "cbb", "CBB", "1000000"),
    CRYPTOBILIA(3, "Cryptobilia", "CBL", "1000000");

    private Integer code;
    private String name;
    private String symbol;
    private String limit;

    StellarAssetType(Integer code, String name, String symbol, String limit) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.limit = limit;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getLimit() {
        return limit;
    }

    public static StellarAssetType fromCode(Integer code) {
        for (StellarAssetType StellarAssetType : StellarAssetType.values()) {
            if (StellarAssetType.getCode().equals(code)) {
                return StellarAssetType;
            }
        }
        throw new UnsupportedOperationException("Enumeration Code " + code + " is not supported");
    }

    public static StellarAssetType fromName(String name) {
        for (StellarAssetType StellarAssetType : StellarAssetType.values()) {
            if (StellarAssetType.getName().equalsIgnoreCase(name)) {
                return StellarAssetType;
            }
        }
        throw new UnsupportedOperationException("Enumeration Name " + name + " is not supported");
    }

    public static StellarAssetType fromSymbol(String symbol) {
        for (StellarAssetType StellarAssetType : StellarAssetType.values()) {
            if (StellarAssetType.getSymbol().equalsIgnoreCase(symbol)) {
                return StellarAssetType;
            }
        }
        throw new UnsupportedOperationException("Enumeration Symbol " + symbol + " is not supported");
    }
}
