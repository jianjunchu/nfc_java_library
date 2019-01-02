package com.aofei.nfc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;

import com.aofei.mifare.Converter;
import com.aofei.mifare.MifareBlock;
import com.aofei.mifare.MifareClassCard;
import com.aofei.mifare.MifareSector;

import jonelo.jacksum.JacksumAPI;
import jonelo.jacksum.algorithm.AbstractChecksum;
import jonelo.jacksum.ui.ExitStatus;
import jonelo.sugar.util.ExitException;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.util.Log;


/**
* ����һ������  NFC оƬ��13.56Mhz�� 14443A ����  Android SDK. ���������   NFC оƬ���ж���д����������֤��������Կ�Ȳ���.
* Ŀǰ��SDK ֧�ֵ�оƬ���Ͱ���  FJ8018��  NXP203, NXP216 ��.
* �������ʹ���������⣬���������ϵ��
* ����֧������   support@nfcsolution.cnaddress
* 
* ���蹺�� SDK ֧�ֵ�NFC оƬ/��ǩ/���� �뵽��
* http://shop70712385.taobao.com/
* 
* @date 2014-05-16
* @author ������������������޹�˾   
*         www.nfcsolution.cn   
*         www.nfc315.com  
* 
*/
public class TagUtil {

	private static final int TAGUTIL_TYPE_ULTRALIGHT = 1;
	private static final int TAGUTIL_TYPE_CLASSIC = 2;
	private static final int TAGUTIL_NfcA = 3;
	private static final byte PAGE_ADDR_AUTH0 = 42;
	private static final byte PAGE_ADDR_AUTH1 = 43;
	
	//	private static android.nfc.Tag tag;	
	
	private static String uid;
	private static String finalPage;

	private int tagType;
	
	private byte[] secretKeyDefault;
	private byte[] ivDefault = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };// Ĭ������
	private byte[] random = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };// �����
	public byte[] getRandom() {
		return random;
	}

	public void setRandom(byte[] random) {
		this.random = random;
	}

	private boolean authorised = true;
	private String ERR_MSG;
	
	private static NfcA nfcA=null;
	private static MifareUltralight ultralight = null;
	
	public TagUtil(String u,int type)
	{
		uid=u;
		tagType = type;
	}
	
	/**
	 * ��ȡ��ǩ����, ���û�б�ǩ����Null�� ����ǲ�֧�ֵı�ǩ�����׳��쳣
	 * @param intent
	 * @param isCheckSUM: �Ƿ�����У��λ (�Բ��� MTK ���ֻ���Ҫ���� У��λ ����Ϊtrue).  
	 * �жϷ�����  ʹ��  getprop �������Կ���һ�����������ѡ��
     * ro.mediatek.gemini_support
     * ��������������  true���ͱ�ʾ���ֻ���  mtk оƬ�� ��Ҫ���������Լ�����Ҫִ�е����з����� �ò���������Ϊ true��
	 * @return
	 * @throws Exception:  will throw this exception for unsupported chips 
	 */
	public static TagUtil selectTag(Intent intent,boolean isCheckSUM) throws Exception
	{
		String action = intent.getAction();
		int type=0;
		// �õ��Ƿ��⵽ACTION_TECH_DISCOVERED����
		if (isSupportedAction(action)) {
			// ȡ����װ��intent�е�TAG
			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			String[] tagTypes = tagFromIntent.getTechList();// ֧�ֵ����ͼ���
			String tagType = null;
			for(int i=0;i<tagFromIntent.getTechList().length;i++)
			{
				if(type>0)
					continue;
				if (tagTypes != null && tagTypes.length > 0) {
					tagType = tagFromIntent.getTechList()[i];
				}
				if ("android.nfc.tech.MifareUltralight".equals(tagType)) {
					getTagUID_MifareUltralight(tagFromIntent);
					type=TAGUTIL_TYPE_ULTRALIGHT;
				} else if ("android.nfc.tech.MifareClassic".equals(tagType)) {
					//getMifareClassicMes(tagFromIntent);
					byte[] myNFCID = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
					uid=bytesToHexString(myNFCID);
					type=TAGUTIL_TYPE_CLASSIC;
				} else if ("android.nfc.tech.NfcA".equals(tagType)) {
					try{
						getTagUID_NfcA(tagFromIntent,isCheckSUM);
						//getFinalPage_NfcA(tagFromIntent,isCheckSUM);
						type=TAGUTIL_NfcA;
						
					}catch(Exception ex)
					{
						if(i==tagFromIntent.getTechList().length-1)//test last time 
							throw ex;
					}
				}
				else
				{
						throw new Exception("unsupported action "+action +" only support ACTION_TECH_DISCOVERED or ACTION_TAG_DISCOVERED or ACTION_NDEF_DISCOVERED");
				}
			}
		tagFromIntent = null;
		TagUtil tagUtil = new TagUtil(uid,type); 
		return tagUtil;		
//		if(checkTag(type))
//		{
//			TagUtil tagUtil = new TagUtil(uid,type); 
//			return tagUtil;
//		}
//		else
//			throw new Exception ("illegal tag");
		}
		return null;
	}


	private static boolean checkTag(int type) throws Exception
	{
		if(uid!=null && uid.length()>0 && type>0)
		{
			if("80".equals(uid.substring(0,2)) || "a2".equals(uid.substring(0,2)) || "53".equals(uid.substring(0,2)))
			{
				if(finalPage!=null && finalPage.length()>0)
				{
					return true;
				}
				else
					return false;
			}
			else
			{
				return true;
				//throw new Exception ("uid seems illegal");
			}
		}else
			throw new Exception("can't get uid");
	
	}
	
	/**
	 * ��ȡһ��ҳ��( 1��ҳ����� 4 ���ֽ�),
	 * @param intent
	 * @param addr:  Ҫ��ȡ��ҳ���ַ 
	 * @param isCheckSUM: �Ƿ�����У��λ
	 * @return ���� 4 ���ֽڵ�����  �������ȡʧ�ܷ��� null �������Ҫ��֤���ܶ�ȡ���׳��쳣��
	 * @throws AuthenticationException
	 * @throws Exception
	 */	
	public byte[] readOnePage(Intent intent,byte addr,boolean isCheckSum) throws AuthenticationException,Exception
	{
		if(tagType==TagUtil.TAGUTIL_NfcA)
			return readOnePage_NfcA( intent, addr,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return readOnePage_MifareUltraLight( intent, addr,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return readOnePage_MifareClassic( intent, addr);
		else 
			return null;
	}
	
	private byte[] readOnePage_MifareUltraLight(Intent intent,byte addr,boolean isCheckSum) throws AuthenticationException,Exception
	{
		String action = intent.getAction();
		byte[] result = null;
		// �õ��Ƿ��⵽ACTION_TECH_DISCOVERED����
		if (isSupportedAction(action)) {
			// ȡ����װ��intent�е�TAG
			//Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			//NfcA mfc = NfcA.get(tagFromIntent);
			try {
				if(authorised){
					//mfc.connect();
					//accreditation(mfc,secretKeyDefault);//��֤
					byte[] data0 = new byte[2];
					byte[] dataWithCheckSum = new byte[4];
					data0[0] = 0x30;
					data0[1] = addr;
					byte[] data1;
					if(isCheckSum)
					{
						byte[] checkSum = getCheckSum(data0);
						dataWithCheckSum[0]=data0[0];
						dataWithCheckSum[1]=data0[1];
						dataWithCheckSum[2]=checkSum[0];
						dataWithCheckSum[3]=checkSum[1];
						data1 = ultralight.transceive(dataWithCheckSum);// ÿ�ζ�����������Ϊ4page������
					}
					else
						data1 = ultralight.transceive(data0);// ÿ�ζ�����������Ϊ4page������
					
					result = new byte[4];
					if(data1.length<16)
						throw new AuthenticationException("please authenticate first!");
					else
						System.arraycopy(data1, 0, result, 0, 4);// ȥ4page�еĵ�1page����
				}else{
					throw new AuthenticationException("please authenticate first!");
				}				
			} catch (Exception e) {
				throw e;
			} 
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
		}
		return result;
	
	}
	
	private byte[] readOnePage_MifareClassic(Intent intent,byte addr) throws AuthenticationException,Exception
	{
		byte[] testByte=null;
		String action = intent.getAction();
		Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		MifareClassic mfc = MifareClassic.get(tagFromIntent);
		MifareClassCard mifareClassCard = null;
		
	
		try {			
			mfc.connect();
		}catch(IllegalStateException ex)
		{
			mfc.close();
			mfc.connect();
		}
		
		try {
			boolean auth = false;
			int secCount = mfc.getSectorCount();
			mifareClassCard = new MifareClassCard(secCount);
			MifareSector mifareSector = new MifareSector();
			mifareSector.sectorIndex = addr;
			auth = mfc.authenticateSectorWithKeyA(addr,MifareClassic.KEY_DEFAULT);
			mifareSector.authorized = auth;
			int bCount = mfc.getBlockCountInSector(addr);
			bCount = Math.min(bCount, MifareSector.BLOCKCOUNT);
			int bIndex = mfc.sectorToBlock(addr);
			ByteBuffer buffer = ByteBuffer.allocate(bCount*16);
			StringBuffer b = new StringBuffer();
			for (int i = 0; i < bCount; i++) {
				byte[] data = mfc.readBlock(bIndex);
				String s  = TagUtil.bytesToHexString(data);
				b.append(s);
				buffer.put(data);
				bIndex++;
			}
			String result = b.toString();
			return buffer.array();
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
			return null;
				
//				int bCount = 0;
//				int bIndex = 0;
//				for (int j = 0; j < secCount; j++) {
//					MifareSector mifareSector = new MifareSector();
//					mifareSector.sectorIndex = j;
//					auth = mfc.authenticateSectorWithKeyA(j,MifareClassic.KEY_DEFAULT);
//					mifareSector.authorized = auth;
//					if (auth) {
//						bCount = mfc.getBlockCountInSector(j);
//						bCount = Math.min(bCount, MifareSector.BLOCKCOUNT);
//						bIndex = mfc.sectorToBlock(j);
//						for (int i = 0; i < bCount; i++) {
//							data = mfc.readBlock(bIndex);
//							if(j==12 && i==2){
//								try{
//
//									mfc.writeBlock(bIndex, testByte);
//								}catch(IOException e){
//								}finally{
//									//showAlert(3,"666");
//								}
//								
//							}
//							//showAlert(3,Integer.toString(j)+i);
//							MifareBlock mifareBlock = new MifareBlock(data);
//							mifareBlock.blockIndex = bIndex;
//							bIndex++;
//							mifareSector.blocks[i] = mifareBlock;
//
//						}
//						mifareClassCard.setSector(mifareSector.sectorIndex,mifareSector);
//					} else {
//
//					}
//
//						}
//				ArrayList<String> blockData = new ArrayList<String>();
//				int blockIndex = 0;
//				for (int i = 0; i < secCount; i++) {
//
//					MifareSector mifareSector = mifareClassCard.getSector(i);
//					for (int j = 0; j < MifareSector.BLOCKCOUNT; j++) {
//						MifareBlock mifareBlock = mifareSector.blocks[j];
//						data = mifareBlock.getData();
//						blockData.add("Block " + blockIndex++ + " : "
//								+ Converter.getHexString(data, data.length));
//					}
//				}
//				String[] contents = new String[blockData.size()];
//				blockData.toArray(contents);
////				setListAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, contents));
////				getListView().setTextFilterEnabled(true);
//			} catch (IOException e) {
//				e.printStackTrace();
//				//Log.e(TAG, e.getLocalizedMessage());
//				
//			} finally {
//
//				if (mifareClassCard != null) {
//					mifareClassCard.debugPrint();
//				}
//			}
//		//}// End of method
//return null;
//		//throw new Exception("unimplemented");
	}
	
	private byte[] readOnePage_NfcA(Intent intent,byte addr, boolean isCheckSum) throws AuthenticationException,Exception
	{
		String action = intent.getAction();
		byte[] result = null;
		// �õ��Ƿ��⵽ACTION_TECH_DISCOVERED����
		if (isSupportedAction(action)) {
			// ȡ����װ��intent�е�TAG
			//Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			//NfcA mfc = NfcA.get(tagFromIntent);
			try {
				if(authorised){
					//mfc.connect();
					//accreditation(mfc,secretKeyDefault);//��֤
					byte[] data0 = new byte[2];
					byte[] dataWithCheckSum = new byte[4];
					data0[0] = 0x30;
					data0[1] = addr;
					byte[] data1;
					if(isCheckSum)
					{
						byte[] checkSum = getCheckSum(data0);
						dataWithCheckSum[0]=data0[0];
						dataWithCheckSum[1]=data0[1];
						dataWithCheckSum[2]=checkSum[0];
						dataWithCheckSum[3]=checkSum[1];
						data1 = nfcA.transceive(dataWithCheckSum);// ÿ�ζ�����������Ϊ4page������
					}
					else
						data1 = nfcA.transceive(data0);// ÿ�ζ�����������Ϊ4page������
					
					result = new byte[4];
					if(data1.length<16)
						throw new AuthenticationException("please authenticate first!");
					else
						System.arraycopy(data1, 0, result, 0, 4);// ȥ4page�еĵ�1page����
				}else{
					throw new AuthenticationException("please authenticate first!");
				}				
			} catch (Exception e) {
				throw e;
			} 
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
		}
		return result;
	}
	
	/**
	 * ��ȡ4��ҳ�棨1��ҳ����� 4 ���ֽڣ�
	 * @param intent
	 * @param addr:  Ҫ��ȡ��4��ҳ��ĵ�һ��ҳ��ĵ�ַ 
	 * @param isCheckSUM: �Ƿ�����У��λ
	 * @return ����  16 ���ֽڵ����顣 �����ȡʧ�ܷ��� null �������Ҫ��֤���ܶ�ȡ���׳��쳣��
	 * @throws AuthenticationException
	 * @throws Exception
	 */	
	public byte[] readFourPage(Intent intent,byte addr, boolean isCheckSum) throws AuthenticationException,Exception
	{
		if(tagType==TagUtil.TAGUTIL_NfcA)
			return readFourPage_NfcA( intent, addr,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return readFourPage_MifareUltraLight( intent, addr,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return readFourPage_MifareClassic( intent, addr);
		else 
			return null;
	}
	
	private byte[] readFourPage_MifareUltraLight(Intent intent,byte addr,boolean isCheckSum) throws AuthenticationException,Exception
	{
		String action = intent.getAction();
		byte[] result = null;
		// �õ��Ƿ��⵽ACTION_TECH_DISCOVERED����
		if (isSupportedAction(action)) {
			// ȡ����װ��intent�е�TAG
			//Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			//NfcA mfc = NfcA.get(tagFromIntent);
			try {
				if(authorised){
					//mfc.connect();
					//accreditation(mfc,secretKeyDefault);
					byte[] data0 = new byte[2];
					byte[] dataWithCheckSum = new byte[4];
					data0[0] = 0x30;
					data0[1] = addr;
					byte[] data1;
					if(isCheckSum)
					{
						byte[] checkSum = getCheckSum(data0);
						dataWithCheckSum[0]=data0[0];
						dataWithCheckSum[1]=data0[1];
						dataWithCheckSum[2]=checkSum[0];
						dataWithCheckSum[3]=checkSum[1];
						result = ultralight.transceive(dataWithCheckSum);// ÿ�ζ�����������Ϊ4page������
					}
					else
						result = ultralight.transceive(data0);// ÿ�ζ�����������Ϊ4page������
				}else{
					throw new AuthenticationException("please authenticate first!");
				}				
			} catch (Exception e) {
				throw e;
			} 
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
		}
		return result;
	}
	
	private byte[] readFourPage_MifareClassic(Intent intent,byte addr) throws AuthenticationException,Exception
	{
		throw new Exception("unimplemented");
	}

	private byte[] readFourPage_NfcA(Intent intent,byte addr, boolean isCheckSum) throws AuthenticationException,Exception
	{
		String action = intent.getAction();
		byte[] result = null;
		// �õ��Ƿ��⵽ACTION_TECH_DISCOVERED����
		if (isSupportedAction(action)) {
			// ȡ����װ��intent�е�TAG
			//Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			//NfcA mfc = NfcA.get(tagFromIntent);
			try {
				if(authorised){
					//mfc.connect();
					//accreditation(mfc,secretKeyDefault);
					byte[] data0 = new byte[2];
					byte[] dataWithCheckSum = new byte[4];
					data0[0] = 0x30;
					data0[1] = addr;
					byte[] data1;
					if(isCheckSum)
					{
						byte[] checkSum = getCheckSum(data0);
						dataWithCheckSum[0]=data0[0];
						dataWithCheckSum[1]=data0[1];
						dataWithCheckSum[2]=checkSum[0];
						dataWithCheckSum[3]=checkSum[1];
						result = nfcA.transceive(dataWithCheckSum);// ÿ�ζ�����������Ϊ4page������
					}
					else
						result = nfcA.transceive(data0);// ÿ�ζ�����������Ϊ4page������
				}else{
					throw new AuthenticationException("please authenticate first!");
				}				
			} catch (Exception e) {
				throw e;
			} 
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
		}
		return result;
	}
	
	/**
	 * дһ��ҳ�棨1��ҳ�����  4���ֽڣ�
	 * @param intent
	 * @param addr  Ҫд��ҳ���
	 * @param contents  �ĸ��ֽڳ��ȵ�����
	 * @param isCheckSUM: �Ƿ�����У��λ
	 * @return д��ɹ����� true�� д��ʧ�ܷ��� false
	 * @throws AuthenticationException
	 * @throws Exception
	 */
	public boolean writeTag(Intent intent, byte addr, byte[] contents, boolean isCheckSum) throws AuthenticationException, Exception
	{
		if(tagType==TagUtil.TAGUTIL_NfcA)
			return writeAble( intent, addr,contents, isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return writeTag_MifareUltraLight( intent, addr,contents,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return writeTag_MifareClassic( intent, addr,contents);
		else 
			return false;
	}
	
	 private boolean writeAble(Intent intent, byte addr, byte contents[], boolean isCheckSum)
		        throws AuthenticationException, Exception
		    {
		        boolean res = false;
		        if((new Integer(addr)).intValue() < 4)
		            throw new AuthenticationException("page_no must be large then four");
		        try
		        {
		            byte newByteArray[] = appendByteArray(contents);
		            int pageNum = newByteArray.length / 4;
		            byte array[] = new byte[4];
		            for(int i = 0; i < pageNum; i++)
		            {
		                array[0] = newByteArray[0 + 4 * i];
		                array[1] = newByteArray[1 + 4 * i];
		                array[2] = newByteArray[2 + 4 * i];
		                array[3] = newByteArray[3 + 4 * i];
		                try
		                {
		                    writeTag_NfcA(intent, (byte)(addr + i), array, false);
		                }
		                catch(Exception e)
		                {
		                    throw new Exception((new StringBuilder()).append("write page ").append(addr + i).append(" failed").toString());
		                }
		            }

		            res = true;
		        }
		        catch(Exception e)
		        {
		            e.printStackTrace();
		            Log.e("xxx", e.getMessage());
		        }
		        return res;
		    }
	 
	private boolean writeTag_MifareUltraLight(Intent intent, byte addr, byte[] contents,boolean isCheckSum) throws AuthenticationException, Exception
	{
		boolean result = false;
		String action = intent.getAction();
		if (isSupportedAction(action)) {
//			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//			NfcA mfc = NfcA.get(tagFromIntent);
			
			try {
				if(authorised){
					if(contents != null && contents.length== 4){//�ж����������
						//mfc.connect();
						//accreditation(mfc,secretKeyDefault);//��֤
						byte[] data2 = new byte[6];
						byte[] dataWithCheckSum= new byte[8];
						data2[0] = (byte) 0xA2;
						data2[1] = addr;
						data2[2] = contents[0];
						data2[3] = contents[1];
						data2[4] = contents[2];
						data2[5] = contents[3];
						byte[] data3;
						if(isCheckSum)
						{
							byte[] checkSum = getCheckSum(data2);
							dataWithCheckSum[0]=data2[0];
							dataWithCheckSum[1]=data2[1];
							dataWithCheckSum[2]=data2[2];
							dataWithCheckSum[3]=data2[3];
							dataWithCheckSum[4]=data2[4];
							dataWithCheckSum[5]=data2[5];
							dataWithCheckSum[6]=checkSum[0];
							dataWithCheckSum[7]=checkSum[1];
							data3 = ultralight.transceive(dataWithCheckSum);// ÿ�ζ�����������Ϊ4page������
						}
						else
							data3 = ultralight.transceive(data2);
						result=true;
					}else{
						throw new AuthenticationException("contents must be four bytes");
					}
				}
				else
				{
					throw new AuthenticationException("please authenticate first!");
				}			
			} catch (Exception e) {
				throw e;
			} 
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
		}
		return result;
	}
	
	
	private boolean writeTag_MifareClassic(Intent intent, byte addr, byte[] contents) throws AuthenticationException, Exception
	{
		Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		MifareClassic mfc = MifareClassic.get(tagFromIntent);
		MifareClassCard mifareClassCard = null;
		try {
			mfc.connect();
			boolean auth = false;
			int secCount = mfc.getSectorCount();
			mifareClassCard = new MifareClassCard(secCount);
			MifareSector mifareSector = new MifareSector();
			mifareSector.sectorIndex = addr;
			auth = mfc.authenticateSectorWithKeyA(addr,MifareClassic.KEY_DEFAULT);
			mifareSector.authorized = auth;
			int bCount = mfc.getBlockCountInSector(addr);
			bCount = Math.min(bCount, MifareSector.BLOCKCOUNT);
			int bIndex = mfc.sectorToBlock(addr);
			for(int i =0;i<3;i++)
			{
				byte[] data = new byte[16];
				System.arraycopy(contents, 16*i, data, 0, 16);
				mfc.writeBlock(bIndex, data);
				bIndex++;
			}
			return true;
		}catch(Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
	}
	
	private boolean writeTag_NfcA(Intent intent, byte addr, byte[] contents, boolean isCheckSum) throws AuthenticationException, Exception
	{
		boolean result = false;
		String action = intent.getAction();
		if (isSupportedAction(action)) {
//			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//			NfcA mfc = NfcA.get(tagFromIntent);
			
			try {
				if(authorised){
					if(contents != null && contents.length== 4){//�ж����������
						//mfc.connect();
						//accreditation(mfc,secretKeyDefault);//��֤
						byte[] data2 = new byte[6];
						byte[] dataWithCheckSum= new byte[8];
						data2[0] = (byte) 0xA2;
						data2[1] = addr;
						data2[2] = contents[0];
						data2[3] = contents[1];
						data2[4] = contents[2];
						data2[5] = contents[3];
						byte[] data3;
						if(isCheckSum)
						{
							byte[] checkSum = getCheckSum(data2);
							dataWithCheckSum[0]=data2[0];
							dataWithCheckSum[1]=data2[1];
							dataWithCheckSum[2]=data2[2];
							dataWithCheckSum[3]=data2[3];
							dataWithCheckSum[4]=data2[4];
							dataWithCheckSum[5]=data2[5];
							dataWithCheckSum[6]=checkSum[0];
							dataWithCheckSum[7]=checkSum[1];
							data3 = nfcA.transceive(dataWithCheckSum);// ÿ�ζ�����������Ϊ4page������
						}
						else
							data3 = nfcA.transceive(data2);
						result=true;
					}else{
						throw new AuthenticationException("contents must be four bytes");
					}
				}
				else
				{
					throw new AuthenticationException("please authenticate first!");
				}			
			} catch (Exception e) {
				throw e;
			} 
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
		}
		return result;
	}
	
	private static boolean isSupportedAction(String action) {
		return NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action);
	}

	
	/**
	 * ��֤
	 * @param intent
	 * @param key ��Կ 16 ���ַ�����ĸ�����֣���
	 * @param isCheckSUM: �Ƿ�����У��λ
	 * @return
	 * @throws TagAuthenticationException
	 */
	public boolean authentication(Intent intent, String key, boolean isCheckSum) throws AuthenticationException, Exception
	{
		String dexString = this.bytesToHexString(key.getBytes());
		return authentication_internal(intent,dexString,isCheckSum);
	}
	
	/**
	 * ��֤
	 * @param intent
	 * @param key ��Կ 16 ���ַ�����ĸ�����֣���
	 * @param isCheckSUM: �Ƿ�����У��λ
	 * @return
	 * @throws TagAuthenticationException
	 */
	public boolean authentication(Intent intent, byte[] key, boolean isCheckSum) throws AuthenticationException, Exception
	{
		String dexString = this.bytesToHexString(key);
		return authentication_internal(intent,dexString,isCheckSum);
	}
	/**
	 * ��֤
	 * @param intent
	 * @param key ��Կ 16 ���ֽڣ� ��   32  �� 16 �����ַ����ַ�����ʾ��
	 * @param isCheckSUM: �Ƿ�����У��λ
	 * @return
	 * @throws AuthenticationException
	 */
	public boolean authentication_internal(Intent intent, String key, boolean isCheckSum) throws AuthenticationException, Exception
	{
		if(tagType==TagUtil.TAGUTIL_NfcA)
			 return authentication_NfcA( intent, key,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return authentication_MifareUltraLight( intent, key,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return authentication_MifareClassic( intent, key);
		else 
			throw new Exception("unknow tag Type"+ tagType+". or SelectTag first.");
	}
	
	private boolean authentication_MifareUltraLight(Intent intent, String key, boolean isCheckSum) throws AuthenticationException, Exception
	{
		return false;
	}
	
	private boolean authentication_MifareClassic(Intent intent, String key) throws AuthenticationException, Exception
	{
		return false;
	}	
	
	private boolean authentication_NfcA(Intent intent, String key, boolean isCheckSum) throws AuthenticationException
	{
		boolean result = false;
		String action = intent.getAction();
		if (isSupportedAction(action)) {
//			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//			NfcA mfc = NfcA.get(tagFromIntent);
			try {
				//Log.e("aaa",key);
				if(key != null && key.length() == 32){//�ж����������
					byte[] data = new byte[24];
					byte[] binaryKey = hexStringToBytes(key);	
					System.arraycopy(binaryKey, 0, data, 0, 16);
					System.arraycopy(binaryKey, 0, data, 16, 8);
					//mfc.connect();
					accreditation(nfcA,data,isCheckSum);//��֤
					authorised=true;
					secretKeyDefault = data;
					return true;
				}else{
					ERR_MSG = "key must be 32 hex chars,current key is "+key;
					return false;
				}	
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			} 
			finally
			{
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
			}
		}else
		{
			ERR_MSG=action+ " is not support"+ ", action must be on of ACTION_TECH_DISCOVERED or ACTION_TAG_DISCOVERED";
			return false;
		}
	}
	
	/**
	 * �ӵ� 0 ҳ��ʼ����ȡָ��ҳ�������ݣ�����һ���ֽ�����(1 ҳ 4 ���ֽ�)
	 * @param intent:  
	 * @param pageNums:  ָ��ҳ��
	 * @param isCheckSUM: �Ƿ�����У��λ
	 * @return
	 * @throws Exception
	 */
	public byte[] readAllPages(Intent intent,int pageNums,boolean isCheckSum) throws Exception{
		
		if(tagType==TagUtil.TAGUTIL_NfcA)
			return readAllPages_NfcA( intent, pageNums,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return readAllPages_MifareUltraLight( intent,pageNums);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return readAllPages_MifareClassic( intent,pageNums);
		else 
			return null;
	}
	
	private byte[] readAllPages_MifareUltraLight(Intent intent,int pageNums) throws Exception{
		Date date= new Date();
		long time_old;
		long time_new;
		byte[] testByte=null;
		String action = intent.getAction();

		//if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			
			time_old = date.getTime();
			MifareClassic mfc = MifareClassic.get(tagFromIntent);
			date = new Date();
			time_new = date.getTime();
			
			long long_time = time_new-time_old;
			time_old = time_new;
			String time_test = "��ʼ��NFC��ʱ��:"+ Long.toString(long_time)+";";
			MifareClassCard mifareClassCard = null;

			try {
				mfc.connect();
				date = new Date();
				time_new = date.getTime();
				
				long_time=time_new-time_old;
				time_old = time_new;
				time_test+="����NFC��ʱ��:"+ Long.toString(long_time)+";";
				boolean auth = false;
				int secCount = mfc.getSectorCount();
				mifareClassCard = new MifareClassCard(secCount);
				int bCount = 0;
				int bIndex = 0;
				long_time=date.getTime()-long_time;
				time_test+="����NFC���󵽿�ʼѭ����ȡ����:"+ Long.toString(long_time)+";";
				for (int j = 0; j < secCount; j++) {
					MifareSector mifareSector = new MifareSector();
					mifareSector.sectorIndex = j;
					auth = mfc.authenticateSectorWithKeyA(j,MifareClassic.KEY_DEFAULT);
					mifareSector.authorized = auth;
					if (auth) {
						bCount = mfc.getBlockCountInSector(j);
						bCount = Math.min(bCount, MifareSector.BLOCKCOUNT);
						bIndex = mfc.sectorToBlock(j);
						for (int i = 0; i < bCount; i++) {
							byte[] data = mfc.readBlock(bIndex);
							if(j==12 && i==2){
								try{

									mfc.writeBlock(bIndex, testByte);
								}catch(IOException e){
									time_test+="д������Ϣ"+e.toString()+";";
								}finally{
									//showAlert(3,"666");
								}
								
							}
							//showAlert(3,Integer.toString(j)+i);
							MifareBlock mifareBlock = new MifareBlock(data);
							mifareBlock.blockIndex = bIndex;
							bIndex++;
							mifareSector.blocks[i] = mifareBlock;

						}
						mifareClassCard.setSector(mifareSector.sectorIndex,mifareSector);
					} else {

					}
					date = new Date();
					time_new = date.getTime();
					
					long_time=time_new-time_old;
					time_old = time_new;
					time_test+="��"+ (j+1) +"��������ȡʱ��:"+ Long.toString(long_time)+";";
				}
				ArrayList<String> blockData = new ArrayList<String>();
				int blockIndex = 0;
				for (int i = 0; i < secCount; i++) {

					MifareSector mifareSector = mifareClassCard.getSector(i);
					for (int j = 0; j < MifareSector.BLOCKCOUNT; j++) {
						MifareBlock mifareBlock = mifareSector.blocks[j];
						byte[] data = mifareBlock.getData();
						blockData.add("Block " + blockIndex++ + " : "
								+ Converter.getHexString(data, data.length));
					}
				}
				String[] contents = new String[blockData.size()];
				blockData.toArray(contents);
//				setListAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, contents));
//				getListView().setTextFilterEnabled(true);
				date = new Date();
				time_new = date.getTime();
				
				long_time=time_new-time_old;
				time_old = time_new;
				time_test+="������װ��ҳ��:"+ Long.toString(long_time)+";";

			} catch (IOException e) {
				e.printStackTrace();
				//Log.e(TAG, e.getLocalizedMessage());
				
			} finally {

				if (mifareClassCard != null) {
					mifareClassCard.debugPrint();
				}
			}
		//}// End of method
return null;
		//throw new Exception("unimplemented");
	}
	
	private byte[] readAllPages_MifareClassic(Intent intent,int pageNums) throws Exception{
		Date date= new Date();
		long time_old;
		long time_new;
		byte[] testByte=null;
		String action = intent.getAction();

		//if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			
			time_old = date.getTime();
			MifareClassic mfc = MifareClassic.get(tagFromIntent);
			date = new Date();
			time_new = date.getTime();
			
			long long_time = time_new-time_old;
			time_old = time_new;
			String time_test = "��ʼ��NFC��ʱ��:"+ Long.toString(long_time)+";";
			MifareClassCard mifareClassCard = null;

			try {
				mfc.connect();
				date = new Date();
				time_new = date.getTime();
				
				long_time=time_new-time_old;
				time_old = time_new;
				time_test+="����NFC��ʱ��:"+ Long.toString(long_time)+";";
				boolean auth = false;
				int secCount = mfc.getSectorCount();
				mifareClassCard = new MifareClassCard(secCount);
				int bCount = 0;
				int bIndex = 0;
				long_time=date.getTime()-long_time;
				time_test+="����NFC���󵽿�ʼѭ����ȡ����:"+ Long.toString(long_time)+";";
				Log.w("xxx",time_test);
				for (int j = 1; j < secCount; j++) {
					MifareSector mifareSector = new MifareSector();
					mifareSector.sectorIndex = j;
					auth = mfc.authenticateSectorWithKeyA(j,MifareClassic.KEY_DEFAULT);
					mifareSector.authorized = auth;
					if (auth) {
						bCount = mfc.getBlockCountInSector(j);
						bCount = Math.min(bCount, MifareSector.BLOCKCOUNT);
						bIndex = mfc.sectorToBlock(j);
						for (int i = 0; i < bCount; i++) {
							byte[] data = mfc.readBlock(bIndex);
//							if(j==12 && i==2){
//								try{
//
//									mfc.writeBlock(bIndex, testByte);
//								}catch(IOException e){
//									time_test+="д������Ϣ"+e.toString()+";";
//								}finally{
//									//showAlert(3,"666");
//								}
//								
//							}
							//showAlert(3,Integer.toString(j)+i);
							MifareBlock mifareBlock = new MifareBlock(data);
							mifareBlock.blockIndex = bIndex;
							bIndex++;
							mifareSector.blocks[i] = mifareBlock;

						}
						mifareClassCard.setSector(mifareSector.sectorIndex,mifareSector);
					} else {

					}
					date = new Date();
					time_new = date.getTime();
					
					long_time=time_new-time_old;
					time_old = time_new;
					time_test+="��"+ (j+1) +"��������ȡʱ��:"+ Long.toString(long_time)+";";
					Log.e("xxx",time_test);
				}
				ArrayList<String> blockData = new ArrayList<String>();
				int blockIndex = 0;
				ByteBuffer buffer = ByteBuffer.allocate(16*4*16);
				for (int i = 0; i < secCount; i++) {

					MifareSector mifareSector = mifareClassCard.getSector(i);
					for (int j = 0; j < MifareSector.BLOCKCOUNT; j++) {
						MifareBlock mifareBlock = mifareSector.blocks[j];
						byte[] data = mifareBlock.getData();
						buffer.put(data);
//						blockData.add("Block " + blockIndex++ + " : "
//								+ Converter.getHexString(data, data.length));
					}
				}
				return buffer.array();
				
//				String[] contents = new String[blockData.size()];
//				blockData.toArray(contents);
//				setListAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, contents));
//				getListView().setTextFilterEnabled(true);
//				date = new Date();
//				time_new = date.getTime();
//				
//				long_time=time_new-time_old;
//				time_old = time_new;
//				time_test+="������װ��ҳ��:"+ Long.toString(long_time)+";";

			} catch (IOException e) {
				e.printStackTrace();
				//Log.e(TAG, e.getLocalizedMessage());
				
			} finally {

				if (mifareClassCard != null) {
					mifareClassCard.debugPrint();
				}
			}
		//}// End of method
       return null;
		//throw new Exception("unimplemented");
	}
	
	private byte[] readAllPages_NfcA(Intent intent, int pageNums,boolean isCheckSum) throws Exception{
//			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//			NfcA mfc = NfcA.get(tag);
			int byteNum =pageNums*4; // 4 bytes a page
			byte[] result= new byte[byteNum];
			try {
				if(authorised){
					//mfc.connect();
					for (int i = 0x04; i < byteNum/4; i++) {
						byte[] data0 = new byte[2];
						byte[] dataWithCheckSum = new byte[4];
						data0[0] = 0x30;
						data0[1] = (byte) i;
						
						byte[] data1;
						if(isCheckSum)
						{
							byte[] checkSum = getCheckSum(data0);
							dataWithCheckSum[0]=data0[0];
							dataWithCheckSum[1]=data0[1];
							dataWithCheckSum[2]=checkSum[0];
							dataWithCheckSum[3]=checkSum[1];
							data1 = nfcA.transceive(dataWithCheckSum);// ÿ�ζ�����������Ϊ4page������
						}
						else
						    data1 = nfcA.transceive(data0);// 4 pages
						if(data1.length>=4)
							System.arraycopy(data1, 0, result, 4*i, 4);// get one page
						else
							throw new Exception("read the" +i +"th page failed! "+data1.length +"bytes was read");
					}
					return result;
				}else{
					throw new AuthenticationException("please authenticate first!");
				}		
			} catch (Exception e) {
				throw e;
			} 
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
		}
		
	/**
	 * �ı���Կ
	 * @param intent
	 * @param newKey �µ���Կ
	 * @param isCheckSUM: �Ƿ�����У��λ
	 * @return
	 * @throws AuthenticationException
	 * @throws Exception
	 */
	public boolean writeNewKey(Intent intent,String newKey,boolean isCheckSum) throws AuthenticationException,Exception
	{
		if(newKey != null && newKey.length() == 32){
		if(tagType==TagUtil.TAGUTIL_NfcA)
			return writeNewKey_NfcA( intent,newKey,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return writeNewKey_MifareUltraLight( intent,newKey);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return writeNewKey_MifareClassic( intent,newKey);
		else 
			return false;
		}else
			throw new Exception("key must be 32 hex chars");
	}
	
	private boolean writeNewKey_MifareUltraLight(Intent intent,String newKey) throws Exception{
		throw new Exception("unimplemented");
	}
	
	private boolean writeNewKey_MifareClassic(Intent intent,String newKey) throws Exception{
		throw new Exception("unimplemented");
	}
	
	// д����Կ
	private boolean writeNewKey_NfcA(Intent intent,String newKey, boolean isCheckSum)  throws Exception{
		boolean result = false;
		String action = intent.getAction();
		if (isSupportedAction(action)) {
//			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//			NfcA mfc = NfcA.get(tagFromIntent);
			try {
				if(authorised){
					String dataString = newKey;
					//�ж����������	
						
						byte[] dataX = hexStringToBytes(dataString);
						byte[] dataY = new byte[16];
						for(int i=0;i<16;i++){
							dataY[i] = dataX[15-i];
							System.out.println("mi"+dataY[i]);
						}									
						byte[] data1 = new byte[6];
						byte[] data1WithCheckSum= new byte[8];
						data1[0] = (byte) 0xA2;
						data1[1] = (byte) 0x2C;
						System.arraycopy(dataY, 8, data1, 2, 4);
						
						byte[] data2 = new byte[6];
						byte[] data2WithCheckSum= new byte[8];
						data2[0] = (byte) 0xA2;
						data2[1] = (byte) 0x2D;
						System.arraycopy(dataY, 12, data2, 2, 4);
						
						byte[] data3 = new byte[6];
						byte[] data3WithCheckSum= new byte[8];
						data3[0] = (byte) 0xA2;
						data3[1] = (byte) 0x2E;
						System.arraycopy(dataY, 0, data3, 2, 4);
						
						byte[] data4 = new byte[6];
						byte[] data4WithCheckSum= new byte[8];
						data4[0] = (byte) 0xA2;
						data4[1] = (byte) 0x2F;
						System.arraycopy(dataY, 4, data4, 2, 4);
						
//						mfc.connect();
//						accreditation(mfc,secretKeyDefault);//��֤
						
						
						if(isCheckSum)
						{
							byte[] checkSum = getCheckSum(data1);
							for(int i=0;i<6;i++)
								data1WithCheckSum[i]=data1[i];
							data1WithCheckSum[6]=checkSum[0];
							data1WithCheckSum[7]=checkSum[1];
							nfcA.transceive(data1WithCheckSum);// ÿ�ζ�����������Ϊ4page������
						}
						else
							nfcA.transceive(data1);
						
						if(isCheckSum)
						{
							byte[] checkSum = getCheckSum(data2);
							for(int i=0;i<6;i++)
								data2WithCheckSum[i]=data2[i];
							data2WithCheckSum[6]=checkSum[0];
							data2WithCheckSum[7]=checkSum[1];
							nfcA.transceive(data2WithCheckSum);// ÿ�ζ�����������Ϊ4page������
						}
						else
							nfcA.transceive(data2);
						
						if(isCheckSum)
						{
							byte[] checkSum = getCheckSum(data3);
							for(int i=0;i<6;i++)
								data3WithCheckSum[i]=data3[i];
							data3WithCheckSum[6]=checkSum[0];
							data3WithCheckSum[7]=checkSum[1];
							nfcA.transceive(data3WithCheckSum);// ÿ�ζ�����������Ϊ4page������
						}
						else
							nfcA.transceive(data3);
						
						if(isCheckSum)
						{
							byte[] checkSum = getCheckSum(data4);
							for(int i=0;i<6;i++)
								data4WithCheckSum[i]=data4[i];
							data4WithCheckSum[6]=checkSum[0];
							data4WithCheckSum[7]=checkSum[1];
							nfcA.transceive(data4WithCheckSum);// ÿ�ζ�����������Ϊ4page������
						}
						else
							nfcA.transceive(data4);						
						result = true;
			
				}else{
					throw new AuthenticationException("please authenticate first!");
				}			
			}catch (NumberFormatException e) {
				throw new Exception("new key: "+newKey+" is not correct." +" key must be 32 hex chars");
			} catch (Exception e) {
				throw e;
			} 
//			finally {
//				try {
//					mfc.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
		}
		return result;
	}
	
	/**
	 * ����оƬ�������루������FJ8216 ��   NXP216 оƬ   �����ڵ��ñ�����ǰ��Ҫ��ͨ�� authentication216 ����������֤��
	 * @param intent
	 * @param newPWD �ĸ��ֽڵ�  byte ����,�µ�����
	 * @param PACK �����ֽڵ�  byte ����,����������֤�ɹ���ķ���ֵ��
	 * @param isCheckSum
	 * @return
	 * @throws AuthenticationException
	 * @throws Exception
	 */
	public boolean writePWD216(Intent intent,byte[] newPWD,byte[] PACK, boolean isCheckSum) throws AuthenticationException,Exception
	{
		if(newPWD != null && newPWD.length == 4 && PACK.length==2){
		if(tagType==TagUtil.TAGUTIL_NfcA)
			return writeNewKey216_NfcA( intent,newPWD,PACK,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return writeNewKey216_MifareUltraLight( intent,newPWD,PACK,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return writeNewKey216_MifareClassic( intent,newPWD,PACK,isCheckSum);
		else 
			return false;
		}else
			throw new Exception("new PWD must be 4 bytes and PACK must be 2 bytes");
	}
	
	private boolean writeNewKey216_MifareUltraLight(Intent intent, byte[] newPWD,byte[] PACK,boolean isCheckSum) throws Exception{
		byte[] oldE6 = readOnePage(intent, (byte)0XE6, isCheckSum);
		byte[] newE6 = new byte[4];
		newE6[0]=PACK[0];
		newE6[1]=PACK[1];
		newE6[2]=oldE6[2];
		newE6[3]=oldE6[3];
		boolean result1 = writeTag(intent, (byte)0XE5, newPWD, isCheckSum);
		boolean result2 = writeTag(intent, (byte)0XE6, newE6, isCheckSum);
		return result1 && result2 ;
	}

	private boolean writeNewKey216_MifareClassic(Intent intent, byte[] newPWD,byte[] PACK,boolean isCheckSum) throws Exception{
		throw new Exception("unimplemented");
	}

	private boolean writeNewKey216_NfcA(Intent intent, byte[] newPWD,byte[] PACK,
			boolean isCheckSum) throws Exception {		
		byte[] oldE6 = readOnePage(intent, (byte)0XE6, isCheckSum);
		byte[] newE6 = new byte[4];
		newE6[0]=PACK[0];
		newE6[1]=PACK[1];
		newE6[2]=oldE6[2];
		newE6[3]=oldE6[3];
		boolean result1 = writeTag(intent, (byte)0XE5, newPWD, isCheckSum);
		boolean result2 = writeTag(intent, (byte)0XE6, newE6, isCheckSum);
		return result1 && result2 ;		
	}

	/**
	 * ����һ����ʼҳ���ַ�ͷ��ʷ�ʽ�� ��ҳ���ַ���ҳ�涼��Ҫ��֤��ſɷ��ʡ����ʷ�ʽ�����֣�0Ϊ��д���ʣ�1 Ϊд����
	 * @param intent
	 * @param addr: ���ʵ�ַ��
	 * @param access: �������Ϊ  0, ��д��������Ҫ��Ȩ. �������Ϊ1 ,ֻ��д������Ҫ��Ȩ�� 
	 * @param isCheckSUM: �Ƿ�����У��λ
	 * @return
	 * @throws Exception
	 */
	public boolean setAccess(Intent intent,byte addr, int access,boolean isCheckSum) throws Exception
	{
		if(tagType==TagUtil.TAGUTIL_NfcA)
			return setAccess_NfcA( intent,addr, access,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return setAccess_MifareUltraLight( intent,addr, access);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return setAccess_MifareClassic( intent,addr, access);
		else 
			throw new Exception("unknow tag Type"+ tagType+". or SelectTag first.");
	}
	
	private boolean setAccess_MifareClassic(Intent intent, byte addr, int access) throws Exception {
		throw new Exception("unimplemented");
	}

	private boolean setAccess_MifareUltraLight(Intent intent, byte addr,
			int access) throws Exception {
		throw new Exception("unimplemented");
	}

	private boolean setAccess_NfcA(Intent intent, byte addr, int access, boolean isCheckSum ) throws Exception {
		if(addr< (byte)3 || addr > 0x30)
			throw new Exception("address must between 03h and 30h");
		else
		{
			int value = (int)addr<<24;
			access = access << 24;
			boolean result1 = writeTag(intent, (byte)0X2A, this.getBytesArray(value), isCheckSum);
			boolean result2 = writeTag(intent, (byte)0X2B, this.getBytesArray(access), isCheckSum);
			if(result1 && result2)
				return true;
			else
				return false;
		}
	}
	
	/**
	 * ��������λ
	 * @param intent
	 * @param isCheckSum �Ƿ���������Զ�����У��λ��
	 * @return
	 * @throws Exception
	 */
	public boolean lockLockingbits(Intent intent, boolean isCheckSum) throws Exception
	{
		if(tagType==TagUtil.TAGUTIL_NfcA)
			return lockLockingbits_NfcA( intent,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return lockLockingbits_MifareUltraLight( intent);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return lockLockingbits_MifareClassic( intent);
		else 
			throw new Exception("unknow tag Type"+ tagType+". or SelectTag first.");
	}

	private boolean lockLockingbits_MifareClassic(Intent intent)  throws Exception{
		throw new Exception("unimplemented");
	}

	private boolean lockLockingbits_MifareUltraLight(Intent intent) throws Exception {
		throw new Exception("unimplemented");
	}

	private boolean lockLockingbits_NfcA(Intent intent,boolean isCheckSum)  throws Exception {
		byte[] contents1= new byte[4];
		contents1[0]=(byte)0;
		contents1[1]=(byte)0;
		contents1[2]=(byte)7;
		contents1[3]=(byte)0;

		byte[] contents2= new byte[4];
		contents2[0]=(byte)17;
		contents2[1]=(byte)15;
		contents2[2]=(byte)0;
		contents2[3]=(byte)0;
		
		if (writeTag(intent, (byte)2, contents1,isCheckSum) && writeTag(intent, (byte)40, contents2,isCheckSum))
			return true;
		else
			return false;
	}

	/**
	 * ����һ��ҳ�淶Χ
	 * @param intent
	 * @param addr1 Ҫ�����Ŀ�ʼҳ��
	 * @param addr2 Ҫ�����Ľ���ҳ��
     * @param isCheckSum �Ƿ���������Զ�����У��λ��
	 * @return
	 * @throws Exception
	 */
	public boolean lockPage(Intent intent,byte addr1 ,byte addr2, boolean isCheckSum) throws Exception
	{
		if(tagType==TagUtil.TAGUTIL_NfcA)
			return lockPage_NfcA( intent,addr1, addr2, isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return lockPage_MifareUltraLight( intent,addr1, addr2);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return lockPage_MifareClassic( intent,addr1, addr2);
		else 
			throw new Exception("unknow tag Type"+ tagType+". or SelectTag first.");
	}
	
	
	private boolean lockPage_MifareClassic(Intent intent,byte addr1, byte addr2) throws Exception {
		throw new Exception("unimplemented");
	}

	private boolean lockPage_MifareUltraLight(Intent intent,byte addr1, byte addr2)  throws Exception {
		throw new Exception("unimplemented");
	}

	private boolean lockPage_NfcA(Intent intent, byte startAddr1, byte endAddr, boolean isCheckSum)  throws Exception {
		boolean result = false;
		
		if(startAddr1>endAddr)
		{
			throw new Exception ("endAdddr must greater than or equal to startAddr");
		}
		
		if(startAddr1<3 || endAddr >47)
		{
			throw new Exception ("startAddr and endAdddr must between [3,47]");
		}
		
		if(endAddr>15)
		{
			if(lockPage_NfcA_Part1(intent,startAddr1,(byte)15, isCheckSum)  &&	lockPage_NfcA_Part2(intent,(byte)16,endAddr, isCheckSum))
				result = true;
		}
		else
		{
			if(lockPage_NfcA_Part1(intent,startAddr1,(byte)15, isCheckSum))
				result=true;
		}
		return result;
	}

	/**
	 * ����Ϊ�ڵ� 40 ҳ�� ���������ķ�Χ�� 16 �� 47 ҳ�档
	 * lock in page 40, lock address between 16 and 47
	 * @param intent
	 * @param b
	 * @param endAddr
     * @param isCheckSum �Ƿ���������Զ�����У��λ��
	 */
	private boolean lockPage_NfcA_Part2(Intent intent, byte startAddr, byte endAddr, boolean isCheckSum) throws Exception{
		byte[] contents = new byte[4];
		int value=0;
		int totalValue=0;
		for(int j=startAddr;j<=endAddr;j++)
		{
			if(j<=39)
			{
				if(j%4>0)
					continue;
				int i=(j)/4;
				switch (i)
				{
					case 4:
						value=2^25;
						break;
					case 5:
						value=2^26;
						break;
					case 6:
						value=2^27;
						break;
					case 7:
						value=2^29;
						break;
					case 8:
						value=2^30;
						break;
					case 9:
						value=2^31;
						break;
					default:
						break;
				}
			}else
				{
					switch (j)
					{
						case 41:
							value=2^20;
							break;
						case 42:
							value=2^21;
							break;
						case 43:
							value=2^22;
							break;
						case 44:
							value=2^23;
							break;
						case 40:
						case 45:
						case 46:
						case 47:
							default:
							
				}
			}
			totalValue+=value;
		}
		contents = getBytesArray(totalValue);
	    return writeTag(intent, (byte)40, contents, isCheckSum);	
	}

	private byte[] getBytesArray(int value) {
		byte[] contents = new byte[4];
	    contents[0] = (byte)(value >>> 24);
	    contents[1] = (byte)(value >>> 16);  
	    contents[2] = (byte)(value >>> 8);    
	    contents[3] = (byte)(value );
	    return contents;
	}

	/**
	 * ����λ�ڵ� 2 ҳ�棬 ���������ĵ�ַ��Χ�� 3 �� 15
	 * lock in page 2, lock address between 3 and 15
	 * @param startAddr1
	 * @param endAddr
	 * @param isCheckSum �Ƿ���������Զ�����У��λ��
	 */
	private boolean lockPage_NfcA_Part1(Intent intent, byte startAddr, byte endAddr, boolean isCheckSum) throws Exception{
		byte[] contents = new byte[4];
		int value=0;
		int totalValue=0;
		for(int i=startAddr;i<=endAddr;i++)
		{
			switch (i)
			{
				case 3:
					value=2^8;
					break;
				case 4:
					value=2^12;
					break;
				case 5:
					value=2^13;
					break;
				case 6:
					value=2^14;
					break;
				case 7:
					value=2^15;
					break;
				case 8:
					value=2^0;
					break;
				case 9:
					value=2^1;
					break;
				case 10:
					value=2^2;
					break;
				case 11:
					value=2^3;
					break;
				case 12:
					value=2^4;
					break;
				case 13:
					value=2^5;
					break;
				case 14:
					value=2^6;
					break;
				case 15:
					value=2^7;
					break;
				default:
			}
			totalValue+=value;
		}
	    contents[0] = (byte)(totalValue >>> 24);
	    contents[1] = (byte)(totalValue >>> 16);  
	    contents[2] = (byte)(totalValue >>> 8);    
	    contents[3] = (byte)(totalValue );
	    return writeTag(intent, (byte)2, contents, isCheckSum);		
	}
	
	/**
	 * ��������ҳ��
	 * @param intent
	 * @param isCheckSum �Ƿ���������Զ�����У��λ��
	 * @return
	 * @throws Exception
	 */
	public boolean lockPageAll(Intent intent, boolean isCheckSum) throws Exception
	{	
		boolean result = false;
		switch(tagType)
		{
		case TagUtil.TAGUTIL_NfcA:
			result= lockPageAll_NfcA(intent, isCheckSum);
			break;
		case TagUtil.TAGUTIL_TYPE_ULTRALIGHT:
			result= lockPageAll_MifareUltraLight(intent);
			break;
		case TagUtil.TAGUTIL_TYPE_CLASSIC:
			result= lockPageAll_MifareClassic(intent);
			break;
		default:
			throw new Exception("unknow tag Type"+ tagType+". or SelectTag first.");
		}return result;
	}
	
	
	private boolean lockPageAll_NfcA(Intent intent, boolean isCheckSum) throws Exception
	{
		byte[] contents1= new byte[4];
		contents1[0]=(byte)0;
		contents1[1]=(byte)0;
		contents1[2]=(byte)255;
		contents1[3]=(byte)255;

		byte[] contents2= new byte[4];
		contents2[0]=(byte)255;
		contents2[1]=(byte)255;
		contents2[2]=(byte)0;
		contents2[3]=(byte)0;
		
		if (writeTag(intent, (byte)2, contents1, isCheckSum) && writeTag(intent, (byte)40, contents2, isCheckSum))
			return true;
		else
			return false;
	}
	
	private boolean lockPageAll_MifareUltraLight(Intent intent) throws Exception
	{
		throw new Exception("unimplemented");
	}
	
	private boolean lockPageAll_MifareClassic(Intent intent) throws Exception
	{
		throw new Exception("unimplemented");
	}
	
	/**
	 * ��ȡ��ǩ���ͣ�Ŀǰ��֧�ֵı�ǩ���Ͱ���  NFCA ��  UltraLight
	 * @return
	 * @throws AuthenticationException
	 */
	public int getTagType() throws AuthenticationException
	{
		return tagType;
	}
	
	private void accreditation(NfcA mfc,byte[] secretKeys,boolean isCheckSum) throws Exception {
		byte[] iv = ivDefault;
		
		byte[] command0 = new byte[2];// ������ָ֤��Ĳ���
		byte[] command0WithCheckSum = new byte[4];// ������ָ֤��Ĳ���(with check sum)
		
		byte[] command1 = null;// ������֤�󣬿�Ƭ���ص�����1
		byte[] command1WithCheckSum = null;// ������֤�󣬿�Ƭ���ص�����1
		
		byte[] command2 = null;// ����1ȥ�������еĵ�1������,ȡ����Ч����
		
		byte[] command3 = null;// ����1 ���ܺ������
		byte[] command4 = null;// command2 ����
		byte[] command5 = null;// command3 ѭ�����Ƶõ�������
		byte[] command6 = null;// ʹ��command5 �� command4 �ڶ��μ��ܺ������RNDB
		byte[] command7 = null;//
		byte[] command8 = null;//
		byte[] command9 = null;//
		byte[] command10 = null;//
		byte[] command11 = null;//

		command0[0] = (byte) 0x1A; // ����λ
		command0[1] = (byte) 0x00; // ��־λ
		if(isCheckSum)
		{
			byte[] checkSum = getCheckSum(command0);
			command0WithCheckSum[0]=command0[0];
			command0WithCheckSum[1]=command0[1];
			command0WithCheckSum[2]=checkSum[0];
			command0WithCheckSum[3]=checkSum[1];
			command1WithCheckSum = mfc.transceive(command0WithCheckSum);// 11 bytes
			if(command1WithCheckSum.length != 11)
			{
				String str="";
				for (int i = 0 ; i<command1WithCheckSum.length;i++)
				{
					str = str+" byte"+i+"="+command1WithCheckSum[i]+"  ";
				}
				throw new Exception("length of response is not 11 bytes. the response bytes is: "+str);
			}
			command1= new byte[9];
			System.arraycopy(command1WithCheckSum, 0, command1, 0, 9);
			Log.i("authen","first send end");
			Log.i("authen","first send response" +bytesToHexString(command1));
		}
		else
			command1 = mfc.transceive(command0);
		
		command2 = new byte[8];
		if(command1.length != 9)
		{
			String str="";
			for (int i = 0 ; i<command1.length;i++)
			{
				str = str+" byte"+i+"="+command1[i]+"  ";
			}
			throw new Exception("length of response is not 9 bytes. the response bytes is: "+str);
		}
		System.arraycopy(command1, 1, command2, 0, 8);
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
		command8 = new byte[17];
		
		command8[0] = (byte) 0xAF;
		System.arraycopy(command7, 0, command8, 1, 16);
		
		if(isCheckSum)
		{
			byte[] command8WithCheckSum= new byte[19];
			byte[] checkSum = getCheckSum(command8);
			for(int i=0;i<17;i++)
			{
				command8WithCheckSum[i]=command8[i];
			}
			command8WithCheckSum[17]=checkSum[0];
			command8WithCheckSum[18]=checkSum[1];
			Log.i("authen","sencond send:"+bytesToHexString(command8WithCheckSum));
			command9 = mfc.transceive(command8WithCheckSum);// 
			Log.i("authen","sencond send end");
		}
		else
			command9 = mfc.transceive(command8);
		command10 = new byte[8];
		System.arraycopy(command9, 1, command10, 0, 8);
		iv = command6;
		command11 = ThreeDES.decode(command10, iv, secretKeys);
	}
	
	/**
	 * 16�����ַ���ת��Ϊ�ֽ�����
	 */
	public static byte[] hexStringToBytes(String hexString) {  
	    if (hexString == null || hexString.equals("")) {  
	        return null;  
	    }  
	    hexString = hexString.toUpperCase();  
	    int length = hexString.length() / 2;  
	    char[] hexChars = hexString.toCharArray();  
	    byte[] d = new byte[length];  
	    for (int i = 0; i < length; i++) {  
	        int pos = i * 2;  
	        d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));  
	    }  
	    return d;  
	}  
	
	private static byte charToByte(char c) {  
		int i = "0123456789ABCDEF".indexOf(c);
		if(i == -1){
			throw new NumberFormatException();
		}else{
			return (byte)i;  
		}   
	}
	
	public static String StringtoHexString(String str)
    {
        String hexString = "0123456789ABCDEF";
        byte bytes[] = str.getBytes();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for(int i = 0; i < bytes.length; i++)
        {
            sb.append(hexString.charAt((bytes[i] & 240) >> 4));
            sb.append(hexString.charAt((bytes[i] & 15) >> 0));
        }

        return sb.toString();
    }

    public static String hexStringToString(String bytes)
    {
        String hexString = "0123456789ABCDEF";
        ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length() / 2);
        for(int i = 0; i < bytes.length(); i += 2)
            baos.write(hexString.indexOf(bytes.charAt(i)) << 4 | hexString.indexOf(bytes.charAt(i + 1)));

        return new String(baos.toByteArray());
    }

    public static byte[] StringtoBytes(String str)
    {
        String hexstr = StringtoHexString(str);
        byte byte1[] = hexStringToBytes(hexstr);
        return byte1;
    }	
    
    private byte[] appendByteArray(byte byteArray[])
    {
        int length = byteArray.length;
        int m = length % 4;
        byte newByteArray[];
        if(m == 0)
            newByteArray = new byte[length];
        else
            newByteArray = new byte[length + (4 - m)];
        System.arraycopy(byteArray, 0, newByteArray, 0, length);
        return newByteArray;
    }
    
	/**
	 * �ֽ�����ת��Ϊ16�����ַ���
	 */
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder();
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}
	
	private static void getTagUID_MifareUltralight(Tag tag) throws Exception
	{
		ultralight = MifareUltralight.get(tag);
		try {
			String metaInfo = "";
			ultralight.connect();
			byte[] datas = new byte[2];
			datas[0] = 0x30;
			datas[1] = 0x00;
			byte[] datar = ultralight.transceive(datas);// ÿ�ζ�����������Ϊ4page������
			byte[] datau = new byte[7];//uid��
			System.arraycopy(datar, 0, datau, 0, 3);// ȥ4page�еĵ�1page����
			System.arraycopy(datar, 4, datau, 3, 4);// ȥ4page�еĵ�1page����
			uid=bytesToHexString(datau);
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	private static void getTagUID_NfcA(Tag tag,boolean isCheckSum) throws Exception
	{
//		byte[] datau = tag.getId();
//		uid=bytesToHexString(datau);
		
		nfcA = NfcA.get(tag);
		try {
			String metaInfo = "";
			nfcA.connect();
			byte[] datas = new byte[2];
			byte[] datasWithCheckSum = new byte[4];
 			datas[0] = 0x30;
			datas[1] = 0x00;
			byte[] datar;
			if(isCheckSum)
			{
				byte[] checkSum = getCheckSum(datas);
				datasWithCheckSum[0]=datas[0];
				datasWithCheckSum[1]=datas[1];
				datasWithCheckSum[2]=checkSum[0];
				datasWithCheckSum[3]=checkSum[1];
				datar = nfcA.transceive(datasWithCheckSum);// ÿ�ζ�����������Ϊ4page������
			}
			else
				datar = nfcA.transceive(datas);// ÿ�ζ�����������Ϊ4page������
			byte[] datau = new byte[7];//uid��
			System.arraycopy(datar, 0, datau, 0, 3);// ȥ4page�еĵ�1page����
			System.arraycopy(datar, 4, datau, 3, 4);// ȥ4page�еĵ�1page����
			uid=bytesToHexString(datau);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw e;
		}
//		finally
//		{
//			try {
//				mfc.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
	}
	
	private static void getFinalPage_NfcA(Tag tag,boolean isCheckSum) throws Exception
	{
		try {
			//mfc.connect();
			byte[] datas = new byte[2];
			byte[] datasWithChcekSum = new byte[4];
			
			datas[0] = 0x30;
			datas[1] = (byte)0xFF;
			byte[] checkSum;
			
			if(isCheckSum)
			{
				checkSum = getCheckSum(datas);
				datasWithChcekSum[0]=datas[0];
				datasWithChcekSum[1]=datas[1];
				datasWithChcekSum[2]=checkSum[0];
				datasWithChcekSum[3]=checkSum[1];
			}
			
			if(nfcA==null)
			{
				nfcA = NfcA.get(tag);
				nfcA.connect();
			}
			
			byte[] datar;
			
			if(isCheckSum)
				datar = nfcA.transceive(datasWithChcekSum); 
			else
				datar = nfcA.transceive(datas);
			
			byte[] datau = new byte[4];//uid��
			System.arraycopy(datar, 0, datau, 0, 3);// 4page�еĵ�1page����
			finalPage=bytesToHexString(datau);
		}
		catch(Exception e)
		{
			throw e;
		}
//		finally
//		{
//			try {
//				mfc.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
	}
	
	public static String getUid() {
		return uid;
	}

	
	/**
	 * ����ʱ��Ҫ��֤��ҳ���ַ��Χ
	 * @param intent
	 * @return ����һ����ʼ��ַ�����ʸõ�ַ�������ҳ�棬����Ҫ��֤�� �������ֵ���ڵ��� 48 ���� ���ʣ�����д������ҳ�涼����Ҫ��֤���������ֵС��48��˵�����ʣ�����д������ֵ��ַ��  48  ֮���ҳ��ʱ����Ҫ��֤��
	 * @throws Exception
	 */
	public int getAuthenticationAddr(Intent intent,boolean isCheckSum) throws Exception
	{
		byte[] result = readOnePage(intent, (byte)PAGE_ADDR_AUTH0,isCheckSum);
		int r = result[0];
		return r;
	}
		
	/**
	 * �������ʱ,�е�ַ��Ҫ��֤���÷������Ի����֤������
	 * @param intent
	 * @param isCheckSum �Ƿ���������Զ�����У��λ��
	 * @return  �������ֵ=0 �����д����Ҫ��֤���������ֵ=1��ֻ��д��Ҫ��֤
	 * @throws Exception
	 */
	public int getAuthenticationType(Intent intent,boolean isCheckSum) throws Exception
	{
		byte[] result = readOnePage(intent, (byte)PAGE_ADDR_AUTH1,isCheckSum);
		int r = result[0];
		return r;
	}
	
	/**
	 * �ر�����
	 */
	public void close()
	{
		try {
			if(nfcA!=null)
			nfcA.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			if(this.ultralight!=null)
				ultralight.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static byte[] getCheckSum(byte[] byteAyyay) throws Exception {
		AbstractChecksum checksum=null;
        try {
        	String checksumArg="crc:16,1021,c6c6,true,true,0";
            checksum = JacksumAPI.getChecksumInstance(checksumArg,false);
        } catch (NoSuchAlgorithmException nsae) {
            throw new ExitException(nsae.getMessage()+"\nUse -a <code> to specify a valid one.\nFor help and a list of all supported algorithms use -h.\nExit.", ExitStatus.PARAMETER);
        }
        checksum.setEncoding(AbstractChecksum.HEX);
        //byte[] byteAyyay = hexStringToBytes(string);
        checksum.update(byteAyyay);
        String hexValue = checksum.getHexValue();
		//String resultStr =checksum.toString();//d97c 02a8
		byte[] result = reverse(hexStringToBytes(hexValue));
		return result;
	}
	
	/**
	 * ʹ������ 
	 * java -jar aofei_nfc.jar
	 * ��ȡ�汾��
	 * @param args
	 */
	public static void main(String[] args)
	{
			System.out.println("2.2.0");
	}
	
//	 private void testGetAuthWithAuthentication(TagUtil tagUtil,Intent intent)
//	    {
//	    	try{
//	    	tagUtil.authentication(intent,  "4A35B5D5454151522140515355405847");
//	    	int addr = tagUtil.getAuthenticationAddr(intent);
//	    	Log.e("kkk", "authentication addr = "+addr);
//	    	
//	    	int auth_type = tagUtil.getAuthenticationType(intent);
//	    	Log.e("kkk", "authentication type = "+auth_type);
//	    	
//	    	}catch ( AuthenticationException ex)  //should authenticate first 
//	    	{
//	    		ex.printStackTrace();
//	    	}catch ( Exception ex)   
//	    	{
//	    		ex.printStackTrace();
//	    	}
//	    }
//	    
//	    
//	    private void testGetAuthWithoutAuthentication(TagUtil tagUtil,Intent intent)
//	    {
//	    	try{
//	    	int addr = tagUtil.getAuthenticationAddr(intent);
//	    	Log.e("kkk", "authentication addr = "+addr);
//	    	
//	    	int auth_type = tagUtil.getAuthenticationType(intent);
//	    	Log.e("kkk", "authentication type = "+auth_type);
//	    	
//	    	}catch ( AuthenticationException ex)  //should authenticate first 
//	    	{
//	    		ex.printStackTrace();
//	    	}catch ( Exception ex)   
//	    	{
//	    		ex.printStackTrace();
//	    	}
//	    }
//	    
//	    /**
//	     * ����֤���ٶ�ȡȫ��ҳ��
//	     * @param tagUtil
//	     * @param intent
//	     * @throws Exception
//	     */
//	    private void testReadWithAuthentication(TagUtil tagUtil,Intent intent) throws Exception
//	    {
//	    	try{
//	    	tagUtil.authentication(intent, "4A35B5D5454151522140515355405847");
//	    	Log.e("kkk", "authentication success");
//	    	
//	    	byte[] bytes = tagUtil.readAllPages(intent);
//	    	for (int i=0;i<bytes.length;i++)
//	    		Log.e("kkk","byte "+i +" is "+bytes[i]);
//	    	
//	    	}catch (Exception ex)
//	    	{
//	    		ex.printStackTrace();
//	    	}
//	    }
	
	public static byte[] reverse(byte[] bytes)
	{
		byte[] result= new byte[bytes.length];
		
		for(int i=0;i<bytes.length;i++)
		{
			result[i]=bytes[bytes.length-i-1];
		}
		return result;
	}
	
	/**
	 * 	��ȡ��������ֵ
	 * @return
	 * @throws Exception 
	 */
	public int getCount(Intent intent,boolean isCheckSum) throws Exception
	{
		if(tagType==TagUtil.TAGUTIL_NfcA)
			return getCount_NfcA( intent,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return getCount_MifareUltraLight( intent,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return getCount_MifareClassic( intent,isCheckSum);
		else 
			throw new Exception("unsupport tag type:"+ tagType);		
	}
	
	private int getCount_NfcA(Intent intent,boolean isCheckSum) throws Exception
	{
		byte[] data0 = new byte[2];
		byte[] dataWithCheckSum = new byte[4];
		byte[] result = new byte[3];
		data0[0] = (byte)0x39;
		data0[1] = (byte)0x00;// FJ 39 00
		byte[] data1;
		if(isCheckSum)
		{
			byte[] checkSum = getCheckSum(data0);
			dataWithCheckSum[0]=data0[0];
			dataWithCheckSum[1]=data0[1];
			dataWithCheckSum[2]=checkSum[0];
			dataWithCheckSum[3]=checkSum[1];
			result = nfcA.transceive(dataWithCheckSum);// ÿ�ζ�����������Ϊ4page������
		}
		else
			result = nfcA.transceive(data0);// ÿ�ζ�����������Ϊ4page������
		
		int count;
		if(result.length==1)
		{
			count = (int)result[0];
		}else if(result.length==2)
		{
			count = 256*(int)result[1]+(int)result[0];
		}else
			count = 256*256*((int)result[2])+256*(int)result[1]+(int)result[0];
		return count;
	}
	
	private int getCount_MifareUltraLight(Intent intent,boolean isCheckSum) throws Exception
	{
		byte[] data0 = new byte[2];
		byte[] dataWithCheckSum = new byte[4];
		byte[] result = new byte[3];
		data0[0] = (byte)0x39;
		data0[1] = (byte)0x02;// NXP 39 02
		byte[] data1;
		if(isCheckSum)
		{
			byte[] checkSum = getCheckSum(data0);
			dataWithCheckSum[0]=data0[0];
			dataWithCheckSum[1]=data0[1];
			dataWithCheckSum[2]=checkSum[0];
			dataWithCheckSum[3]=checkSum[1];
			result = ultralight.transceive(dataWithCheckSum);// ÿ�ζ�����������Ϊ4page������
		}
		else
			result = ultralight.transceive(data0);// ÿ�ζ�����������Ϊ4page������
		
		int count;
		if(result.length==1)
		{
			count = (int)result[0];
		}else if(result.length==2)
		{
			count = 256*(int)result[1]+(int)result[0];
		}else
			count = 256*256*((int)result[2])+256*(int)result[1]+(int)result[0];
		return count;
	}
	
	private int getCount_MifareClassic(Intent intent,boolean isCheckSum) throws Exception
	{
		throw new Exception("unimplemented");
	}
	
	/**
	 * ʹ������������Ч��ʧЧ 
	 * @param intent
	 * @param isCount true ��Ч�� false ʧЧ
	 * @param isCheckSum �Ƿ���������Զ�����У��λ��
	 * @throws AuthenticationException
	 * @throws Exception
	 */
	public void enableCounter(Intent intent,boolean isCount,boolean isCheckSum) throws AuthenticationException, Exception
	{
		if(isCount)
		{
			byte[] bytes = readOnePage(intent, (byte)0xE4, isCheckSum);//E4 for ntag 216
			byte accessByte = bytes[0];
			byte newAccessByte = (byte) (accessByte | (byte)16);
			bytes[0] = newAccessByte;
			writeTag(intent, (byte)0xE4, bytes, isCheckSum);
			//writeTag(intent, (byte)228, bytes, isCheckSum);
		}
		else
		{
			byte[] bytes = readOnePage(intent, (byte)0xE4, isCheckSum);//E4 for ntag 216
			byte accessByte = bytes[0];
			byte newAccessByte = (byte) (accessByte & (byte)(255-16));
			bytes[0] = newAccessByte;
			this.writeTag(intent, (byte)0xE4, bytes, isCheckSum);
		}
			
	}

	/**
	 * ʹ�����������֤
	 * @param intent
	 * @param pwd ��֤���� 
	 * @param isCheckSum
	 * @return
	 * @throws AuthenticationException
	 * @throws Exception
	 */
	public boolean authentication216(Intent intent, byte[] pwd, boolean isCheckSum) throws AuthenticationException,Exception  {
		if(tagType==TagUtil.TAGUTIL_NfcA)
			 return authentication_NfcA_By_Compare( intent, pwd,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return authentication_MifareUltraLight_By_Compare( intent, pwd,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return authentication_MifareClassic_By_Compare( intent, pwd,isCheckSum);
		else 
			throw new Exception("unknow tag Type"+ tagType+". or SelectTag first.");
	}
	
	private boolean authentication_NfcA_By_Compare(Intent intent, byte[] key, boolean isCheckSum) throws AuthenticationException
	{
//		try{
//		accreditation(nfcA,key,isCheckSum);//��֤
//		}
//		catch(Exception ex)
//		{
//			throw new AuthenticationException(ex.toString());
//		}
//		
		byte[] data0 = new byte[5];
		byte[] dataWithCheckSum = new byte[7];
		data0[0] = 0x1B;
		data0[1] = key[0];
		data0[2] = key[1];
		data0[3] = key[2];
		data0[4] = key[3];
		byte[] PAK;
		
		try{
			if(isCheckSum)
			{
				byte[] checkSum = getCheckSum(key);
				dataWithCheckSum[0] = 0x1B;
				dataWithCheckSum[1] = key[0];
				dataWithCheckSum[2] = key[1];
				dataWithCheckSum[3] = key[2];
				dataWithCheckSum[4] = key[3];
				dataWithCheckSum[5]=checkSum[0];
				dataWithCheckSum[6]=checkSum[1];
				PAK = nfcA.transceive(dataWithCheckSum);// ÿ�ζ�����������Ϊ4page������
			}
			else
				PAK = nfcA.transceive(data0);// ÿ�ζ�����������Ϊ4page������
			
			if(PAK.length<2)
			{
				ERR_MSG="PAK less than 2 bytes,PAK = "+this.bytesToHexString(PAK);
				return false;
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	private boolean authentication_MifareUltraLight_By_Compare(Intent intent, byte[] key, boolean isCheckSum) throws AuthenticationException
	{
		throw new AuthenticationException("unimplemented");
	}
	
	private boolean authentication_MifareClassic_By_Compare(Intent intent, byte[] key, boolean isCheckSum) throws AuthenticationException
	{
		throw new AuthenticationException("unimplemented");
	}

	//����Ȩ�޵ķ�����E3h��Byte3��ʾ��ʼ��������ʼҳ��ַ
	//E4h��Byte0�����λ����0��ʾд�����ܱ���������1��ʾ��д���ܱ���
	//дE3��E4ʱ����λ�����ƻ���Ҳ����дǰҪ�ȶ���E3��E4������Byte3,Byte0���λ����д��E3h,E4h
	/**
	 * 
	 * @param intent
	 * @param addr    ��ʼ��������ʼҳ��ַ
	 * @param access  0�����λ����0��ʾд�����ܱ���������1��ʾ��д���ܱ���
	 * @param isCheckSum
	 * @throws Exception
	 */
	public void setAccess216(Intent intent, byte addr, int access, boolean isCheckSum) throws Exception{
		byte[] E3 = readOnePage(intent, (byte)0XE3,isCheckSum);
		byte[] newE3 = new byte[4];
		newE3[0]=E3[0];
		newE3[1]=E3[1];
		newE3[2]=E3[2];
		newE3[3]=addr;
		writeTag(intent, (byte)0XE3, newE3, isCheckSum);
		
		byte[] E4 = readOnePage(intent, (byte)0XE4,isCheckSum);
		byte[] newE4 = new byte[4];
		byte oldByte = E4[0];
		int tempByte = oldByte << 1;
		byte newByte;
		if(access ==1 )
			newByte = (byte) (tempByte+128);
		else
			newByte = (byte)tempByte;
		
		newE4[0]=newByte;
		newE4[1]=E4[1];
		newE4[2]=E4[2];
		newE4[3]=E4[3];
		writeTag(intent, (byte)0XE4, newE4, isCheckSum);
	}
	
/**
 * 	ȡоƬ��CIDֵ����ҳ��ַFFh���õ���16�ֽ����ݵ�ǰ�ĸ��ֽ���ΪCIDֵ���ء�
 * @param intent
 * @param isCheckSum
 * @return
 * @throws Exception 
 * @throws AuthenticationException 
 */
	public byte[] getCID(Intent intent, boolean isCheckSum) throws AuthenticationException, Exception
	{
		byte page = (byte)0xFF;
		byte[] result = this.readOnePage(intent, page, isCheckSum);
		return result;
	}

	/**
	 * 	��֤CID�Ƿ���ȷ��KEY1��ͬ�ڵ�1ʹ�õı�
	 * UID��00h����key1���ܣ������ĸ����ֽ���CID�Ƚϣ����رȽϽ����
	 * @param intent
	 * @param UID
	 * @param CID
	 * @return
	 * @throws Exception 
	 */
	public boolean  verifyCID(Intent intent,byte[] UID, byte[] CID) throws Exception
	{
		if(UID==null || UID.length!=7)
			throw new Exception("UID length is not 7 bytes length");
		if(CID==null || CID.length!=4)
			throw new Exception("UID length is not 4 bytes length");		
		String keyStr = KeyUtil.getKey(UID[6]);
		byte[] newUID= new byte[8];
		for(int i=0;i<7;i++)
		{
			newUID[i]=UID[i];
		}
		newUID[7]=0x00;
		byte[] data = new byte[24];
		byte[] binaryKey = hexStringToBytes(keyStr);	
		System.arraycopy(binaryKey, 0, data, 0, 16);
		System.arraycopy(binaryKey, 0, data, 16, 8);
		byte[] result = ThreeDES.encode(newUID, ivDefault, data);
		if(result[0]==CID[0] && result[1]==CID[1] && result[2]==CID[2] &&result[3]==CID[3])
			return true;
		else
			return false;
	}
	
	/**
	 * ������֤��һ������оƬ������ָ֤�ȡ��8���ֽڵ�RNDB��
	 * @param intent
	 * @return 8 bytes,response from chip
	 * @throws Exception 
	 */
	public byte[] authStep1(Intent intent, boolean isCheckSum) throws Exception
	{
		byte[] command0 = new byte[2];// ������ָ֤��Ĳ���
		byte[] command0WithCheckSum = new byte[4];// ������ָ֤��Ĳ���(with check sum)
		
		byte[] command1 = null;// ������֤�󣬿�Ƭ���ص�����
		byte[] command1WithCheckSum = null;// ������֤�󣬿�Ƭ���ص�����
		
		byte[] command2 = null;// ��command1ȥ�������еĵ�1������,ȡ����Ч����
		
		command0[0] = (byte) 0x1A; // ����λ
		command0[1] = (byte) 0x00; // ��־λ
		if(isCheckSum)
		{
			byte[] checkSum = getCheckSum(command0);
			command0WithCheckSum[0]=command0[0];
			command0WithCheckSum[1]=command0[1];
			command0WithCheckSum[2]=checkSum[0];
			command0WithCheckSum[3]=checkSum[1];
			command1WithCheckSum = nfcA.transceive(command0WithCheckSum);// 11 bytes
			if(command1WithCheckSum.length != 11)
			{
				String str="";
				for (int i = 0 ; i<command1WithCheckSum.length;i++)
				{
					str = str+" byte"+i+"="+command1WithCheckSum[i]+"  ";
				}
				throw new Exception("length of response is not 11 bytes. the response bytes is: "+str);
			}
			command1= new byte[9];
			System.arraycopy(command1WithCheckSum, 0, command1, 0, 9);
			Log.i("authen","first send end");
			Log.i("authen","first send response" +bytesToHexString(command1));
		}
		else
			command1 = nfcA.transceive(command0);
		
		command2 = new byte[8];
		if(command1.length != 9)
		{
			String str="";
			for (int i = 0 ; i<command1.length;i++)
			{
				str = str+" byte"+i+"="+command1[i]+"  ";
			}
			throw new Exception("length of response is not 9 bytes. the response bytes is: "+str);
		}
		System.arraycopy(command1, 1, command2, 0, 8);
		return command2;
	}
	
	/**
	 * ������֤�ڶ�������оƬ�����ɷ��������ص�RNDAB���õ�оƬ���ص�RNDA��
	 * @param intent
	 * @param RANAB 16 bytes
	 * @param isCheckSum
	 * @return 8 bytes,response from chip
	 * @throws Exception
	 */
	public byte[] authStep2(Intent intent,byte[] RANAB, boolean isCheckSum) throws Exception
	{

		byte[] command8 = new byte[17];
		byte[] command9 = null;//
		byte[] command10 = null;//
		
		command8[0] = (byte) 0xAF;
		System.arraycopy(RANAB, 0, command8, 1, 16);
		
		if(isCheckSum)
		{
			byte[] command8WithCheckSum= new byte[19];
			byte[] checkSum = getCheckSum(command8);
			for(int i=0;i<17;i++)
			{
				command8WithCheckSum[i]=command8[i];
			}
			command8WithCheckSum[17]=checkSum[0];
			command8WithCheckSum[18]=checkSum[1];
			Log.i("authen","sencond send:"+bytesToHexString(command8WithCheckSum));
			command9 = nfcA.transceive(command8WithCheckSum);// 
			Log.i("authen","sencond send end");
		}
		else
			command9 = nfcA.transceive(command8);
		command10 = new byte[8];
		if(command9.length==9)
		{
			System.arraycopy(command9, 1, command10, 0, 8);
			return command10;
		}
		else
		{
			throw new Exception("ERROR RNDA��:"+this.bytesToHexString(command9));
		}
		
	} 	
}
