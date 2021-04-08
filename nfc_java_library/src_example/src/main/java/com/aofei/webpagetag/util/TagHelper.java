package com.aofei.webpagetag.util;

import java.io.IOException;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.Ndef;

import com.aofei.nfc.AofeiNfcTag;
import com.aofei.util.HexStringUtil;

public class TagHelper {
	public static boolean writeTag(AofeiNfcTag tag, String url, boolean protection, String secretKey) {
		try {
			tag.connect();
			if (secretKey != null) {
				authentication(tag, secretKey);
			}
			NdefRecord rtdUriRecord = NdefRecord.createUri(url);
			NdefMessage msg = new NdefMessage(rtdUriRecord);
			tag.writeNdefMessage(msg);
			if (secretKey != null) {
				tag.setWriteProtection(protection);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				tag.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public static boolean modifySecretKey(AofeiNfcTag tag, String oldSecretKey, String newSecretKey) {
		try {
			tag.connect();
			authentication(tag, oldSecretKey);
			return writeNewKey(tag, newSecretKey);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				tag.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public static NdefMessage readTag(Ndef ndef) {
		try {
			ndef.connect();
			return ndef.getNdefMessage();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ndef.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static void authentication(AofeiNfcTag tag, String secretKey) throws Exception {
		if (secretKey.length() > 16)
			tag.authentication(HexStringUtil.hexString2Bytes(secretKey));
		else
			tag.authentication(secretKey);
	}

	private static boolean writeNewKey(AofeiNfcTag tag, String newSecretKey) throws Exception {
		if (newSecretKey.length() > 16)
			return tag.writeNewKey(HexStringUtil.hexString2Bytes(newSecretKey));
		else
			return tag.writeNewKey(newSecretKey);
	}
}
