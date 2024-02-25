package com.aofei.websitetag.fragment;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.aofei.nfc.AofeiNfcTag;
import com.aofei.nfc.TagAccessFragment;
import com.aofei.ui.AlertUtil;
import com.aofei.util.SimpleTask;
import com.example.nfclibaray.R;
import com.aofei.webpagetag.util.TagHelper;

public class ModifySecretKeyFragment extends TagAccessFragment implements OnClickListener {

	private EditText	mETOldSecretKey;
	private EditText	mETNewSecretKey;
	private EditText	mETComfirmSecretKey;
	private Button		mBTModifySecretKey;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_modify_secret_key, container, false);
		mETOldSecretKey = (EditText) rootView.findViewById(R.id.edit_old_secret_key);
		mETNewSecretKey = (EditText) rootView.findViewById(R.id.edit_new_secret_key);
		mETComfirmSecretKey = (EditText) rootView.findViewById(R.id.edit_comfirm_secret_key);
		mBTModifySecretKey = (Button) rootView.findViewById(R.id.button_modify_secret_key);

		mBTModifySecretKey.setOnClickListener(this);
		return rootView;
	}

	@Override
	public void onClick(View v) {
		if (v == mBTModifySecretKey) {
			modifySecretKey();
		}
	}

	private void modifySecretKey() {
		if (!checkInput())
			return;
		String oldSecretKey = mETOldSecretKey.getText().toString();
		String newSecretKey = mETNewSecretKey.getText().toString();
		new SimpleTask<String, Boolean>(getActivity(), R.string.modifying_secret_key) {

			@Override
			protected Boolean doInBackground(String... params) {
				return modifySecretKeyInBg(params[0], params[1]);
			}

			@Override
			protected void onPostExecute(Boolean result) {
				super.onPostExecute(result);
				AlertUtil.toastMsg(getActivity(), result ? R.string.modify_secret_key_success : R.string.modify_secret_key_failure);
			}

		}.execute(oldSecretKey, newSecretKey);
	}

	private boolean checkInput() {
		String oldSecretKey = mETOldSecretKey.getText().toString();
		String newSecretKey = mETNewSecretKey.getText().toString();
		String comfirmSecretKey = mETComfirmSecretKey.getText().toString();
		if (TextUtils.isEmpty(oldSecretKey)) {
			AlertUtil.toastMsg(getActivity(), R.string.empty_old_secret_key);
			return false;
		}
		if (TextUtils.isEmpty(newSecretKey)) {
			AlertUtil.toastMsg(getActivity(), R.string.empty_new_secret_key);
			return false;
		}
		if (newSecretKey.getBytes().length > 16) {
			AlertUtil.toastMsg(getActivity(), R.string.new_secret_key_too_long);
			return false;
		}
		if (TextUtils.isEmpty(comfirmSecretKey)) {
			AlertUtil.toastMsg(getActivity(), R.string.empty_comfirm_secret_key);
			return false;
		}
		if (!newSecretKey.equals(comfirmSecretKey)) {
			AlertUtil.toastMsg(getActivity(), R.string.new_secret_key_conflict);
			return false;
		}
		return true;
	}

	private boolean modifySecretKeyInBg(String oldSecretKy, String newSecretKey) {
		Intent tagIntent = getTagAccessActivity().getTagIntent();
		if (tagIntent == null)
			return false;
		AofeiNfcTag tag = new AofeiNfcTag(tagIntent);
		return TagHelper.modifySecretKey(tag, oldSecretKy, newSecretKey);
	}
}
