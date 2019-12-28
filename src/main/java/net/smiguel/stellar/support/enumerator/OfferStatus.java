package net.smiguel.stellar.support.enumerator;

import lombok.ToString;

@ToString
public enum OfferStatus {
    IN_PROGRESS(3, "In Progress", "dropdown.offer.status.inprogress"),
    ACCEPTED(1, "Accepted", "dropdown.offer.status.accepted"),
    DISCARDED(2, "Discarded", "dropdown.offer.status.discarded");

    private Integer code;
    private String nameToShow;
    private String i18n;

    OfferStatus(Integer code, String nameToShow, String i18n) {
        this.code = code;
        this.nameToShow = nameToShow;
        this.i18n = i18n;
    }

    public Integer getCode() {
        return code;
    }

    public String getNameToShow() {
        return nameToShow;
    }

    public String getI18n() {
        return i18n;
    }

    public static OfferStatus fromCode(Integer code) {
        for (OfferStatus OfferStatus : OfferStatus.values()) {
            if (OfferStatus.getCode().equals(code)) {
                return OfferStatus;
            }
        }
        throw new UnsupportedOperationException("Enumeration Code " + code + " is not supported");
    }
}
