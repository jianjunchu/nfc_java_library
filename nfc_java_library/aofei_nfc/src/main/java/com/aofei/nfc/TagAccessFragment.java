package com.aofei.nfc;

import android.app.Fragment;
import android.content.Intent;
import android.nfc.NfcAdapter;

public class TagAccessFragment extends Fragment {

	public static class NotBoundWithTagAccessActivity extends RuntimeException {

		private static final long	serialVersionUID	= 1L;

	}

	protected TagAccessActivity getTagAccessActivity() {
		if (getActivity() instanceof TagAccessActivity) {
			return (TagAccessActivity) getActivity();
		}
		else
			throw new NotBoundWithTagAccessActivity();
	}

	public NfcAdapter getNfcAdapter() {
		return getTagAccessActivity().getNfcAdapter();
	}

	public Intent getTagIntent() {
		return getTagAccessActivity().getTagIntent();
	}
}
