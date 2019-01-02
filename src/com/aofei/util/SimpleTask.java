package com.aofei.util;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.content.Context;

public abstract class SimpleTask<Params, Result> extends AsyncTask<Params, Integer, Result> {

	protected ProgressDialog	mDialogLoading;

	public SimpleTask(Context context, String msg) {
		super();
		init(context, msg);
	}

	public SimpleTask(Context context, int msgId) {
		super();
		init(context, context.getString(msgId));
	}

	@Override
	protected void onPreExecute() {
		mDialogLoading.show();
	}

	@Override
	protected void onPostExecute(Result result) {
		mDialogLoading.dismiss();
	}

	private void init(Context context, String msg) {
		mDialogLoading = new ProgressDialog(context);
		mDialogLoading.setMessage(msg);
		mDialogLoading.setCancelable(false);
	}

}
