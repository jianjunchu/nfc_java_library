/**
 * 
 */
package com.aofei.util;

import java.net.URL;

import android.text.TextUtils;

public class URLUtils {

	public static boolean checkValidURL(String urlStr) {
		boolean rslt = false;
		try {
			if (!TextUtils.isEmpty(urlStr)) {
				new URL(urlStr);
				rslt = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rslt;
	}
}
