package com.aofei.nfc;

import com.aofei.util.HexStringUtil;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.content.Intent;

public class AofeiNfcTag extends NfcTag {

	private TagUtil	mTagUtil;
	private Intent	mTagIntent;

	public AofeiNfcTag(Intent intent) {
		mTagIntent = intent;
		try {
			mTagUtil = TagUtil.selectTag(mTagIntent, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void connect() throws Exception {
		//do nothing
	}

	@Override
	public void close() throws Exception {
		//do nothing
	}

	@Override
	public String getUid() {
		return TagUtil.getUid();
	}

	@Override
	public byte[] readPage(int pageIndex) throws Exception {
		return mTagUtil.readOnePage(mTagIntent, (byte) pageIndex, false);
	}

	@Override
	public byte[] readFourPage(int pageIndex) throws Exception {
		return mTagUtil.readFourPage(mTagIntent, (byte) pageIndex, false);
	}

	@Override
	public void writePage(int index, byte[] data) throws Exception {
		boolean res = mTagUtil.writeTag(mTagIntent, (byte) index, data, false);
		if (!res)
			throw new Exception("write failed");
	}

	public void authentication(String key) throws AuthenticationException, Exception {
		authentication(string2Bytes(key, 16));
	}

	public void authentication(byte[] key) throws AuthenticationException, Exception {
		mTagUtil.authentication(mTagIntent, HexStringUtil.bytes2HexString(key, 0, key.length), false);
	}

	public boolean setAccess(int index, int access) throws Exception {
		return mTagUtil.setAccess(mTagIntent, (byte) index, access, false);
	}

	public boolean setWriteProtection(boolean enable) throws Exception {
		return setAccess(enable ? 0x03 : 0x30, 1);
	}

	public boolean writeNewKey(String newKey) throws AuthenticationException, Exception {
		return writeNewKey(string2Bytes(newKey, 16));
	}

	public boolean writeNewKey(byte[] newKey) throws AuthenticationException, Exception {
		//		byte[] reversedNewKey = new byte[16];
		//		for (int i = 0; i < 16; i++) {
		//			reversedNewKey[i] = newKey[15 - i];
		//		}
		//		byte[] page = new byte[4];
		//		System.arraycopy(reversedNewKey, 0, page, 0, 4);
		//		writePage(0x2E, page);
		//		System.arraycopy(reversedNewKey, 4, page, 0, 4);
		//		writePage(0x2F, page);
		//		System.arraycopy(reversedNewKey, 8, page, 0, 4);
		//		writePage(0x2C, page);
		//		System.arraycopy(reversedNewKey, 12, page, 0, 4);
		//		writePage(0x2D, page);
		//		return true;
		return mTagUtil.writeNewKey(mTagIntent, HexStringUtil.bytes2HexString(newKey, 0, newKey.length), false);
	}

	private byte[] string2Bytes(String srcStr, int byteLen) {
		byte[] strBytes = srcStr.getBytes();
		byte[] tmpBytes = new byte[byteLen];
		System.arraycopy(strBytes, 0, tmpBytes, 0, Math.min(byteLen, strBytes.length));
		return tmpBytes;
	}
	
	/**
	 * 
	 * @param url: the URL should be written to NFC315 tag.
	 * @param VerifyCodeProperty:  the verify code parameter should be added to the URL.
	 * @return
	 */
	public boolean writeUrl(String url,String VerifyCodeProperty) {
		try {
			url = url + "&"+VerifyCodeProperty+"=00000000000000000000000000000000000000";
			int index;
			connect();
			NdefRecord rtdUriRecord = NdefRecord.createUri(url);
			NdefMessage msg = new NdefMessage(rtdUriRecord);
			index = writeNdefMessage(msg);
			configTag(index - 39);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	private void configTag(int index) throws Exception {
		int pageIndex = index / 4;
		int byteIndex = index % 4;
		byte[] page = new byte[4];
		page[0] = (byte) ((byteIndex << 4) & 0xFF);
		page[1] = (byte) 0x04;
		page[2] = (byte) (pageIndex & 0xFF);
		page[3] = (byte) 0xFF;
		writePage(0xF1, page);
		page[0] = (byte) 0x10;
		page[1] = (byte) 0x05;
		page[2] = (byte) 0x00;
		page[3] = (byte) 0x00;
		writePage(0xF2, page);
	}

}
