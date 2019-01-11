package com.aofei.nfc;

import com.aofei.nfc.ThreeDES;


public class Verifier {
	private byte[] ivDefault = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };// Ĭ������
	private byte[] random = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };// �����
	public byte[] getRandom() {
		return random;
	}

	public void setRandom(byte[] random) {
		this.random = random;
	}
	/**
	 * 
	 * @param command2: оƬ��һ����֤�ķ��ؽ��
	 * @param secretKeys:��Կ
	 * @return�÷������������ֽ����飬��һ���ֽ������ٴη��͸�оƬ���ڶ����ֽ�������Ϊ
	 * verifyStep2 �Ĳ�����
	 * @throws Exception
	 */
	public byte[][] verifyStep1(byte[] command2,byte[] secretKeys) throws Exception
	{
		byte[] iv = ivDefault;
		byte[] command3 = null;
		byte[] command4 = null;
		byte[] command5 = null;
		byte[] command6 = null;
		byte[] command7 = null;
		command3 = ThreeDES.decode(command2, iv, secretKeys);
		iv = command2;
		command4 = ThreeDES.encode(random, iv, secretKeys);
		iv = command4;
		command5 = new byte[8];
		System.arraycopy(command3, 1, command5, 0, 7);
		command5[7] = command3[0];
		command6 = ThreeDES.encode(command5, iv, secretKeys);
		
		command7 = new byte[16];
		System.arraycopy(command4, 0, command7, 0, 8);
		System.arraycopy(command6, 0, command7, 8, 8);
		return new byte[][]{command7,command6};
	}
	
	/**
	 * 
	 * @param command6�� verifyStep1 �����ķ��ؽ���ĵڶ����ֽ�����
	 * @param command10�� оƬ��һ����֤�ķ��ؽ��
	 * @param secretKeys�� ��Կ
	 * @return ��֤���
	 * @throws Exception
	 */
	public byte[] verifyStep2(byte[] command6,byte[] command10,byte[] secretKeys) throws Exception
	{
		byte[] iv = command6;
		byte[] command11 = ThreeDES.decode(command10, iv, secretKeys);
		return command11;
	}
}
