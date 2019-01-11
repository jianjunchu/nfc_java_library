package com.aofei.ui;

import android.app.AlertDialog;
import android.content.Context;

public class AlertUtil {

	public static void toastMsg(Context context, int msgId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(msgId).setNeutralButton(android.R.string.ok, null).show();
	}

	public static void toastMsg(Context context, String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(msg).setNeutralButton(android.R.string.ok, null).show();
	}

	public static void toastMsgWithTitle(Context context, String title, String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title).setMessage(msg).setNeutralButton(android.R.string.ok, null).show();
	}
}
