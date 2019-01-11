package com.aofei.util;

public class HexStringUtil {

	public static String bytes2HexString(byte[] data, int start, int len) {
		StringBuffer sb = new StringBuffer();
		for (int i = start; i < start + len; i++)
			sb.append(String.format("%02X", data[i]));
		return sb.toString();
	}

	public static byte[] hexString2Bytes(String str) {
		int size = str.length();
		if (size % 2 != 0 || size <= 0)
			return null;

		byte[] bytes = new byte[size / 2];
		int p;
		for (int i = 0; i < bytes.length; i++) {
			p = i * 2;
			bytes[i] = Short.decode("0x" + str.substring(p, p + 2)).byteValue();
		}
		return bytes;
	}
}
