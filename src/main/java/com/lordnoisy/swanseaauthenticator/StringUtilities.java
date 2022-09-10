package com.lordnoisy.swanseaauthenticator;

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
}
