package com.aofei.webpagetag.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.example.nfclibaray.R;

public class LastDataHolder {
	private static final String	KEY_URL					= "url";
	private static final String	KEY_ENABLE_SECRET_KEY	= "enable_secret_key";
	private static final String	KEY_SECRET_KEY			= "secret_key";
	private static final String	KEY_PROTECTION			= "protection";

	public static void save(Context context, String url, String secretKey, boolean protection) {
		SharedPreferences pre = PreferenceManager.getDefaultSharedPreferences(context);
		pre.edit().putString(KEY_URL, url).putBoolean(KEY_PROTECTION, protection).commit();
		if (secretKey != null)
			pre.edit().putBoolean(KEY_ENABLE_SECRET_KEY, true).putString(KEY_SECRET_KEY, secretKey).commit();
		else
			pre.edit().putBoolean(KEY_ENABLE_SECRET_KEY, false).commit();
	}

	public static Object[] load(Context context) {
		SharedPreferences pre = PreferenceManager.getDefaultSharedPreferences(context);
		Object[] result = new Object[4];
		result[0] = pre.getString(KEY_URL, context.getString(R.string.default_url));
		result[1] = pre.getBoolean(KEY_ENABLE_SECRET_KEY, false);
		result[2] = pre.getString(KEY_SECRET_KEY, context.getString(R.string.default_secret_key));
		result[3] = pre.getBoolean(KEY_PROTECTION, true);
		return result;

	}
}
