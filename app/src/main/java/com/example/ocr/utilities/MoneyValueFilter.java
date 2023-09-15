package com.example.ocr.utilities;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.DigitsKeyListener;

public class MoneyValueFilter extends DigitsKeyListener {
    public MoneyValueFilter() {
        super(false, true);
    }

    private int digits = 2;
    private int maxLength = 2;

    public MoneyValueFilter setDigits(int d) {
        digits = d;
        return this;
    }

    public MoneyValueFilter setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;

    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        CharSequence out = super.filter(source, start, end, dest, dstart, dend);

        // if changed, replace the source
        if (out != null) {
            source = out;
            start = 0;
            end = out.length();
        }

        int len = end - start;

        // if deleting, source is empty
        // and deleting can't break anything
        if (len == 0) {
            return source;
        }

        if (dest.length() == maxLength){
            return "";
        }

        //Log.i("test" ," dest : "+ dest);
        //Log.i("test" ," end : " + end + " start : " +start +" dstart: "+ dstart +" dend: "+ dend);

        int dlen = dest.length();

        // Find the position of the decimal .
        for (int i = 0; i < dstart; i++) {
            if (dest.charAt(i) == '.') {
                // being here means, that a number has
                // been inserted after the dot
                // check if the amount of digits is right
                return (dlen-(i+1) + len > digits) ?
                        "" :
                        new SpannableStringBuilder(source, start, end);
            }
        }

        for (int i = start; i < end; ++i) {
            if (source.charAt(i) == '.') {
                // being here means, dot has been inserted
                // check if the amount of digits is right
                if ((dlen-dend) + (end-(i + 1)) > digits)
                    return "";
                else
                    break;  // return new SpannableStringBuilder(source, start, end);
            }
        }

        // if the dot is after the inserted part,
        // nothing can break
        return new SpannableStringBuilder(source, start, end);
    }
}
