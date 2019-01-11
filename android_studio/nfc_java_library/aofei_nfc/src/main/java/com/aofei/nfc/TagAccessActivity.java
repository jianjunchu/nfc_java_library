package com.aofei.nfc;

import com.aofei.nfc.TagAccessor.OnCardNearListener;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;

public class TagAccessActivity extends Activity {
	private TagAccessor	mTagAccessor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTagAccessor = new TagAccessor(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mTagAccessor.notifyResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mTagAccessor.notifyPause();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		mTagAccessor.notifyNewTagIntent(intent);
	}

	public void setOnCardNearListener(OnCardNearListener listener) {
		mTagAccessor.setOnCardNearListener(listener);
	}

	public NfcAdapter getNfcAdapter() {
		return mTagAccessor.getNfcAdapter();
	}

	public Intent getTagIntent() {
		return mTagAccessor.getTagIntent();
	}
}
