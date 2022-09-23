package com.lordnoisy.swanseaauthenticator;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class StringUtilities {

    /**
     * Build a random string
     * @param n the length of the string to be built
     * @return the final generated string
     */
    public static String getAlphaNumericString(int n)
    {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";
        StringBuilder stringBuilder = new StringBuilder(n);

        //Pick random characters and append them to a string
        for (int i = 0; i < n; i++) {
            int index = (int)(AlphaNumericString.length() * Math.random());
            stringBuilder.append(AlphaNumericString.charAt(index));
        }

        return stringBuilder.toString();
    }

    public static String getDateTime() {
        LocalDateTime localDateTime = LocalDateTime.now();
        String date = localDateTime.toLocalDate().toString();
        String time = localDateTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString();
        return date.concat(" | ").concat(time);
    }
}
