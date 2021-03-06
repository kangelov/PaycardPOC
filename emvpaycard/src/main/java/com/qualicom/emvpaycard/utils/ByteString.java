package com.qualicom.emvpaycard.utils;

/**
 * Created by kangelov on 2015-10-20.
 */
public class ByteString {

    /**
     * Use for display purposes only. Use byteArrayToHexString for everything else.
     * @param stream
     * @return
     */
    public static String printByteStream(byte[] stream) {
        StringBuffer buffer = new StringBuffer();
        if (stream != null) {
            for (byte b : stream) {
                buffer.append(byteToHexString(b) + " ");
            }
        }
        return buffer.toString().trim();
    }

    public static String byteArrayToHexString(byte[] stream) {
        StringBuffer buffer = new StringBuffer();
        if (stream != null) {
            for (byte b : stream) {
                buffer.append(byteToHexString(b));
            }
        } else
            return null;
        return buffer.toString();
    }

    public static String byteArrayToUTF8String(byte[] stream) {
        if (stream != null)
            return new String(stream);
        else
            return null;
    }

    public static String byteToHexString(byte b) {
        String hexString = Integer.toHexString(0xff & b); //Take care of the sign bit.
        if (hexString.length() == 1) hexString = "0" + hexString;
        return hexString.toUpperCase();
    }

    public static byte hexStringToByte(String hexString) {
        hexString = hexString.replaceAll("\\s", ""); //strip any whitespaces that may have been added for readability.
        return (byte)(Integer.parseInt(hexString, 16) & 0xff);
    }

    public static byte[] hexStringToByteArray(String hexString) {
        hexString = hexString.replaceAll("\\s", ""); //strip any whitespaces that may have been added for readability.
        byte[] array = new byte[hexString.length() / 2];
        for(int i=0; i<hexString.length() / 2; i++) {
            array[i] = hexStringToByte(hexString.substring(2 * i, 2 * i + 2));
        }
        return array;
    }
}
