package net.smiguel.stellar.support.enumerator;

import lombok.ToString;

@ToString
public enum OfferType {
    SELL(2, "sell", "Buy Opportunities", "dropdown.offer.type.sell"),
    BUY(1, "buy", "Sell Opportunities", "dropdown.offer.type.buy"),
    TRADE(3, "swap", "Swap Opportunities", "dropdown.offer.type.swap");

    private Integer code;
    private String name;
    private String nameToShow;
    private String i18n;

    OfferType(Integer code, String name, String nameToShow, String i18n) {
        this.code = code;
        this.name = name;
        this.nameToShow = nameToShow;
        this.i18n = i18n;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getNameToShow() {
        return nameToShow;
    }

    public String getI18n() {
        return i18n;
    }

    public static OfferType fromCode(Integer code) {
        for (OfferType OfferType : OfferType.values()) {
            if (OfferType.getCode().equals(code)) {
                return OfferType;
            }
        }
        throw new UnsupportedOperationException("Enumeration Code " + code + " is not supported");
    }
}
