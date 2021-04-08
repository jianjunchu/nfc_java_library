package com.aofei.websitetag.fragment;

import com.aofei.nfc.AofeiNfcTag;
import com.aofei.nfc.TagAccessFragment;
import com.aofei.ui.AlertUtil;
import com.aofei.util.HexStringUtil;
import com.aofei.util.SimpleTask;
import com.aofei.util.URLUtils;
import com.example.nfclibaray.R;
import com.aofei.webpagetag.util.LastDataHolder;
import com.aofei.webpagetag.util.TagHelper;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MainFragment extends TagAccessFragment implements OnClickListener, OnCheckedChangeListener {

	private EditText		mETWebAddress;
	private CheckBox		mCBEnableSecretKey;
	private EditText		mETSecretKey;
	private LinearLayout	mLLWriteProtection;
	private RadioGroup		mRGWriteProtection;
	private Button			mBTWriteTag;
	private Button			mBTReadTag;
	private Button			mBTModifySecretKey;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		mETWebAddress = (EditText) rootView.findViewById(R.id.edit_web_address);
		mCBEnableSecretKey = (CheckBox) rootView.findViewById(R.id.check_secret_key_enbale);
		mETSecretKey = (EditText) rootView.findViewById(R.id.edit_secret_key);
		mLLWriteProtection = (LinearLayout) rootView.findViewById(R.id.ll_write_protection);
		mRGWriteProtection = (RadioGroup) rootView.findViewById(R.id.rgroup_write_protection);
		mBTWriteTag = (Button) rootView.findViewById(R.id.button_write_tag);
		mBTReadTag = (Button) rootView.findViewById(R.id.button_read_tag);
		mBTModifySecretKey = (Button) rootView.findViewById(R.id.button_modify_secret_key);

		mCBEnableSecretKey.setOnCheckedChangeListener(this);
		mBTWriteTag.setOnClickListener(this);
		mBTReadTag.setOnClickListener(this);
		mBTModifySecretKey.setOnClickListener(this);

		mETSecretKey.setVisibility(View.GONE);
		mLLWriteProtection.setVisibility(View.GONE);

		restoreData();
		return rootView;
	}

	@Override
	public void onClick(View v) {
		if (v == mBTWriteTag) {
			writeTag();
		}
		else if (v == mBTReadTag) {
			readTag();
		}
		else if (v == mBTModifySecretKey) {
			modifySecretKey();
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (buttonView == mCBEnableSecretKey) {
			mETSecretKey.setVisibility(isChecked ? View.VISIBLE : View.GONE);
			mLLWriteProtection.setVisibility(isChecked ? View.VISIBLE : View.GONE);
		}
	}

	private void writeTag() {
		if (!checkInput())
			return;
		String url = mETWebAddress.getText().toString();
		boolean enableProtection = mRGWriteProtection.getCheckedRadioButtonId() == R.id.radio_protection_enable;
		boolean enableSecretKey = mCBEnableSecretKey.isChecked();
		String secretKey = enableSecretKey ? mETSecretKey.getText().toString() : null;
		new SimpleTask<Object, Boolean>(getActivity(), R.string.writing_tag) {

			@Override
			protected Boolean doInBackground(Object... params) {
				String url = (String) params[0];
				boolean protection = (Boolean) params[1];
				String secretKey = (String) params[2];
				if (writeTagInBg(url, protection, secretKey)) {
					LastDataHolder.save(getActivity(), url, secretKey, protection);
					return true;
				}
				return false;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				super.onPostExecute(result);
				AlertUtil.toastMsg(getActivity(), result ? R.string.write_tag_success : R.string.write_tag_failure);
			}

		}.execute(url, enableProtection, secretKey);
	}

	private boolean checkInput() {
		String url = mETWebAddress.getText().toString();
		if (!URLUtils.checkValidURL(url)) {
			AlertUtil.toastMsg(getActivity(), R.string.invalid_web_address);
			return false;
		}

		boolean enableSecretKey = mCBEnableSecretKey.isChecked();
		String secretKey = enableSecretKey ? mETSecretKey.getText().toString() : null;
		if (enableSecretKey && TextUtils.isEmpty(secretKey)) {
			AlertUtil.toastMsg(getActivity(), R.string.empty_secret_key);
			return false;
		}

		return true;
	}

	private boolean writeTagInBg(String url, boolean protection, String secretKey) {
		Intent tagIntent = getTagAccessActivity().getTagIntent();
		if (tagIntent == null)
			return false;
		AofeiNfcTag tag = new AofeiNfcTag(tagIntent);
		return TagHelper.writeTag(tag, url, protection, secretKey);
	}

	private void readTag() {
		new SimpleTask<Object, NdefMessage>(getActivity(), R.string.reading_tag) {

			@Override
			protected NdefMessage doInBackground(Object... params) {
				return readTagInBg();
			}

			@Override
			protected void onPostExecute(NdefMessage result) {
				super.onPostExecute(result);
				if (result != null) {
					String msg = null;
					try {
						msg = result.getRecords()[0].toUri().toString();
					} catch (Exception e) {
						msg = HexStringUtil.bytes2HexString(result.toByteArray(), 0, result.getByteArrayLength());
					}
					AlertUtil.toastMsgWithTitle(getActivity(), getString(R.string.read_tag_success), msg);
				}
				else {
					AlertUtil.toastMsg(getActivity(), R.string.read_tag_failure);
				}

			}
		}.execute();
	}

	private NdefMessage readTagInBg() {
		Intent tagIntent = getTagAccessActivity().getTagIntent();
		if (tagIntent == null)
			return null;
		Tag tag = tagIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		Ndef ndef = Ndef.get(tag);
		return TagHelper.readTag(ndef);
	}

	private void modifySecretKey() {
		getFragmentManager().beginTransaction().replace(R.id.container, new ModifySecretKeyFragment()).addToBackStack(null).commit();
	}

	private void restoreData() {
		Object[] lastData = LastDataHolder.load(getActivity());
		String lastUrl = (String) lastData[0];
		boolean enableSecretKey = (Boolean) lastData[1];
		String secretKey = (String) lastData[2];
		boolean lastProtection = (Boolean) lastData[3];
		mETWebAddress.setText(lastUrl);
		mCBEnableSecretKey.setChecked(enableSecretKey);
		mETSecretKey.setText(secretKey);
		mRGWriteProtection.check(lastProtection ? R.id.radio_protection_enable : R.id.radio_protection_disable);
	}

}
