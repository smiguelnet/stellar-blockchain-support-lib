package net.smiguel.stellar.support.enumerator;

import lombok.ToString;

@ToString
public enum StellarAccountType {
    SOURCE_ACCOUNT(1, "Source Account", true),
    USER_ACCOUNT(2, "User Account", false),
    INFLATION_ACCOUNT(3, "Inflation Account", true),
    ASSET_DISTRIBUTION_ACCOUNT(4, "Asset Distribution Account", false),
    ASSET_ISSUER_ACCOUNT(5, "Asset Issuer Account", false),
    CBB_DISTRIBUTION_ACCOUNT(6, "CBB Distribution Account", true),
    CBB_ISSUER_ACCOUNT(7, "CBB Issuer Account", true),
    CBB_ESCROW_ACCOUNT(8, "CBB Escrow Account", true),
    CHANNEL_ACCOUNT(9, "Channel Account", false);

    private Integer code;
    private String name;
    private boolean unique;

    StellarAccountType(Integer code, String name, boolean unique) {
        this.code = code;
        this.name = name;
        this.unique = unique;
    }

    public Integer getCode() {
        return code;
    }

    public boolean isUnique() {
        return unique;
    }

    public String getName() {
        return name;
    }

    public static StellarAccountType fromCode(Integer code) {
        for (StellarAccountType StellarAccountType : StellarAccountType.values()) {
            if (StellarAccountType.getCode().equals(code)) {
                return StellarAccountType;
            }
        }
        throw new UnsupportedOperationException("Enumeration Code " + code + " is not supported");
    }
}
