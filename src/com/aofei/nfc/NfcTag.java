package com.aofei.nfc;

import android.nfc.NdefMessage;

public abstract class NfcTag {

	public abstract void connect() throws Exception;

	public abstract void close() throws Exception;

	public abstract String getUid();

	public abstract byte[] readPage(int pageIndex) throws Exception;

	public abstract byte[] readFourPage(int pageIndex) throws Exception;

	public abstract void writePage(int pageIndex, byte[] data) throws Exception;

	public byte readByte(int index) throws Exception {
		byte[] page = readPage(index / 4);
		return page[index % 4];
	}

	public byte[] readBytes(int index, int length) throws Exception {
		byte[] result = new byte[length];
		int read = 0;
		int pageIndex;
		int byteIndex;
		int needRead;
		byte[] page;
		while (read < length) {
			pageIndex = (read + index) / 4;
			byteIndex = (read + index) % 4;
			needRead = Math.min(4 - byteIndex, length - read);
			page = readPage(pageIndex);
			System.arraycopy(page, byteIndex, result, read, needRead);
			read += needRead;
		}
		return result;
	}

	public int writeBytes(int index, byte[] data) throws Exception {
		int written = 0;
		int pageIndex;
		int byteIndex;
		int needWrite;
		byte[] page;
		while (written < data.length) {
			pageIndex = (written + index) / 4;
			byteIndex = (written + index) % 4;
			needWrite = Math.min(4 - byteIndex, data.length - written);
			if (byteIndex > 0 || needWrite < 4)
				page = readPage(pageIndex);
			else
				page = new byte[4];
			System.arraycopy(data, written, page, byteIndex, needWrite);
			writePage(pageIndex, page);
			written += needWrite;
		}
		return index + data.length;
	}

	public int writeNdefMessage(NdefMessage msg) throws Exception {
		int dataCapicity = verifyCCAndGetDataCapicity();
		int index = getNdefMessageStartIndex(dataCapicity);
		checkDateSize(dataCapicity, index, msg.getByteArrayLength());
		index = writeNdefMessageTLV(index, msg);
		index = writeTerminatorTLV(index);
		return index;
	}

	protected int verifyCCAndGetDataCapicity() throws Exception {
		byte[] page;
		page = readBytes(12, 4);
		assertTrue(page[0] == (byte) 0xE1, "no NFC Forum defined data");
		assertTrue(page[1] == (byte) 0x10, "unsupport version: " + String.format("%02X", page[1]));
		return unsignedByte2Int(page[2]) * 8;
	}

	protected int getNdefMessageStartIndex(int dataCapicity) throws Exception {
		int index = 16;
		byte type;
		int length;
		byte tmp;
		while (index < dataCapicity) {
			type = readByte(index);
			if (type == (byte) 0x03)
				return index;
			index++;
			tmp = readByte(index++);
			if (tmp != (byte) 0xFF) {
				length = unsignedByte2Int(tmp);
			}
			else {
				length = 0;
				tmp = readByte(index++);
				length |= tmp & 0xFF;
				length <<= 8;
				tmp = readByte(index++);
				length |= tmp & 0xFF;
			}
			index += length;
		}
		return 16;
	}

	protected int writeNdefMessageTLV(int index, NdefMessage msg) throws Exception {
		byte[] TL;
		int ndefMsgLength = msg.getByteArrayLength();
		if (ndefMsgLength > 254) {
			TL = new byte[4];
			TL[0] = (byte) 0x03;
			TL[1] = (byte) 0xFF;
			TL[2] = (byte) ((ndefMsgLength >> 8) & 0xFF);
			TL[3] = (byte) (ndefMsgLength & 0xFF);
		}
		else {
			TL = new byte[2];
			TL[0] = (byte) 0x03;
			TL[1] = (byte) ndefMsgLength;
		}
		index = writeBytes(index, TL);
		index = writeBytes(index, msg.toByteArray());
		return index;
	}

	protected int writeTerminatorTLV(int index) throws Exception {
		byte[] data = new byte[] { (byte) 0xFE };
		return writeBytes(index, data);
	}

	protected void checkDateSize(int dataCapicity, int ndefMsgSIndex, int ndefMsgLength) throws Exception {
		int ndefMsgTLVLength = ndefMsgLength + (ndefMsgLength > 254 ? 4 : 2);
		int controlTLVLength = ndefMsgSIndex - 16;
		int terminatorTLVLength = 1;
		int dataSize = controlTLVLength + ndefMsgTLVLength + terminatorTLVLength;
		assertTrue(dataSize <= dataCapicity, "data too large");
	}

	protected int unsignedByte2Int(byte b) {
		return b > 0 ? b : 256 + b;
	}

	protected void assertTrue(boolean value, String errMsg) throws Exception {
		if (!value)
			throw new Exception(errMsg);
	}

}
