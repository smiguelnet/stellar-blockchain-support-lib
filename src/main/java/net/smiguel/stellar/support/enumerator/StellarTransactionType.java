package net.smiguel.stellar.support.enumerator;

import lombok.ToString;

@ToString
public enum StellarTransactionType {
    ACCOUNT_DEBITED("account_debited"),
    ACCOUNT_CREDITED("account_credited"),
    ACCOUNT_CREATED("account_created"),
    TRUSTLINE_CREATED("trustline_created"),
    TRUSTLINE_UPDATED("trustline_updated"),
    TRUSTLINE_REMOVED("trustline_removed"),
    SIGNER_CREATED("signer_created"),
    SIGNER_UPDATED("signer_updated"),
    ACCOUNT_THRESHOLDS_UPDATED("account_thresholds_updated"),
    OFFER("offer"),
    TRADE("trade");

    StellarTransactionType(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }

    public static StellarTransactionType fromName(String name) {
        for (StellarTransactionType StellarTransactionType : StellarTransactionType.values()) {
            if (StellarTransactionType.getName().equalsIgnoreCase(name)) {
                return StellarTransactionType;
            }
        }
        throw new UnsupportedOperationException("Enumeration Name " + name + " is not supported");
    }
}
