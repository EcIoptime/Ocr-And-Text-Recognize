package com.example.ocr.utilities;

import java.util.regex.Pattern;

public enum CardType {
    UNKNOWN(-1),
    VISA(3),
    MASTER_CARD(3),
    AMERICAN_EXPRESS(4),
    DISCOVER(3),
    DINERS_CLUB(3),
    JCB(4),
    ;



    private final int cvcLength;

    CardType(int cvcLength) {
        this.cvcLength = cvcLength;
    }

    public int getCvcLength() {
        return cvcLength;
    }

    public static CardType fromNumber(String number) {
        if (regVisa.matcher(number).matches()) {
            return VISA;
        } else if (regMasterCard.matcher(number).matches()) {
            return MASTER_CARD;
        } else if (regAmericanExpress.matcher(number).matches()) {
            return AMERICAN_EXPRESS;
        }else if (regDiscover.matcher(number).matches()) {
            return DISCOVER;
        }else if (regDinnerClub.matcher(number).matches()) {
            return DINERS_CLUB;
        }else if (regJcb.matcher(number).matches()) {
            return JCB;
        }
        return UNKNOWN;
    }

    private static Pattern regVisa =Pattern.compile("^4[0-9]{12}(?:[0-9]{3})?$");
    private static Pattern regMasterCard = Pattern.compile("^5[1-5][0-9]{14}$");
    private static Pattern regAmericanExpress = Pattern.compile("^3[47][0-9]{13}$");
    private static Pattern regDiscover = Pattern.compile("^(?:6(?:011|5[0-9][0-9])[0-9]{12})$");
    private static Pattern regDinnerClub = Pattern.compile("^(?:3(?:0[0-5]|[68][0-9])[0-9]{11})$");
    private static Pattern regJcb = Pattern.compile("^(?:(?:2131|1800|35\\d{3})\\d{11})$");



}
