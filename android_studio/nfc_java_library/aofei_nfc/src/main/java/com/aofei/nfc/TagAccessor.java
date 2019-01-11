package com.aofei.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;

public class TagAccessor {

	public interface OnCardNearListener {
		public void onCardNear(TagAccessor accessor);
	}

	private Activity			mActivity;
	private NfcAdapter			mNfcAdapter;
	private PendingIntent		mPendingIntent;
	private Intent				mTagIntent;

	private OnCardNearListener	mListener;

	public TagAccessor(Activity activity) {
		mActivity = activity;
		mNfcAdapter = NfcAdapter.getDefaultAdapter(mActivity);
		mPendingIntent = PendingIntent.getActivity(mActivity, 0, new Intent(mActivity, mActivity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}

	public void notifyResume() {
		if (mNfcAdapter != null)
			mNfcAdapter.enableForegroundDispatch(mActivity, mPendingIntent, null, null);
	}

	public void notifyPause() {
		if (mNfcAdapter != null)
			mNfcAdapter.disableForegroundDispatch(mActivity);
	}

	public void notifyNewTagIntent(Intent intent) {
		mTagIntent = intent;
		if (mListener != null)
			mListener.onCardNear(this);
	}

	public void setOnCardNearListener(OnCardNearListener listener) {
		mListener = listener;
	}

	public NfcAdapter getNfcAdapter() {
		return mNfcAdapter;
	}

	public Intent getTagIntent() {
		return mTagIntent;
	}

}
