package net.smiguel.stellar.support.enumerator;

import lombok.ToString;

@ToString
public enum OrderType {
    CREDIT_CBB_WITH_FIAT(1, "Order with Fiat"),
    CREDIT_CBB_WITH_XLM(2, "Order with XLM"),
    PAYMENT(3, "Order for payment"),
    MANAGE_OFFER(4, "Order for manage offer"),
    AUCTION(5, "Order for auction");

    private Integer code;
    private String name;

    OrderType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static OrderType fromCode(Integer code) {
        for (OrderType OrderType : OrderType.values()) {
            if (OrderType.getCode().equals(code)) {
                return OrderType;
            }
        }
        throw new UnsupportedOperationException("Enumeration Code " + code + " is not supported");
    }
}
