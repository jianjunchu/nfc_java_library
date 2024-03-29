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
import com.aofei.util.AuthNTAG424FailException;

import jonelo.jacksum.JacksumAPI;
import jonelo.jacksum.algorithm.AbstractChecksum;
import jonelo.jacksum.ui.ExitStatus;
import jonelo.sugar.util.ExitException;

import android.annotation.TargetApi;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;


/**
 * 这是一个用于  NFC 芯片（13.56Mhz， 14443TypeA ）的  Android SDK. 用来方便Android App 对 NFC 芯片进行读、写、锁定、认证、更换秘钥等操作.
 * 目前该 SDK 支持的芯片类型包括  FJ8018, NXP203, NXP216,M1, UltraLight 等.
 * 如果您在使用中有问题，请和我们联系。
 * 技术支持邮箱   support@nfcsolution.cn
 *
 * 如需购买本 SDK 支持的 NFC 芯片/标签/卡， 请到：
 * http://shop70712385.taobao.com/
 *
 * @date 2014-05-16
 * @author 天津傲飞物联科技有限公司
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
	private byte[] ivDefault = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };// 默认向量
	private byte[] random = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };// 随机数
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
	 * 获取标签对象, 如果没有标签返回Null， 如果是不支持的标签类型抛出异常
	 * @param intent
	 * @param isCheckSUM: 是否增加校验位 (对部分 MTK 的手机需要将该 校验位 设置为true).
	 * 判断方法：  使用  getprop 方法可以看到一个特殊的设置选项
	 * ro.mediatek.gemini_support
	 * 如果这个设置项是  true，就表示该手机是  mtk 芯片。 需要将本方法以及后面要执行的所有方法的 该参数都设置为 true。
	 * @return
	 * @throws Exception:  will throw this exception for unsupported chips
	 */
	public static TagUtil selectTag(Intent intent,boolean isCheckSUM) throws Exception
	{
		String action = intent.getAction();
		int type=0;
		// 得到是否检测到ACTION_TECH_DISCOVERED触发
		if (isSupportedAction(action)) {
			// 取出封装在intent中的TAG
			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			String[] tagTypes = tagFromIntent.getTechList();// 支持的类型集合
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
		else
		{
			throw new Exception("unsupported action "+action +" only support ACTION_TECH_DISCOVERED or ACTION_TAG_DISCOVERED or ACTION_NDEF_DISCOVERED");
		}
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
	 * 读取一个页面( 1个页面包含 4 个字节),
	 * @param intent
	 * @param addr:  要读取的页面地址
	 * @param isCheckSum: 是否增加校验位
	 * @return 返回 4 个字节的数组  ，如果读取失败返回 null ，如果需要认证才能读取，抛出异常。
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
		// 得到是否检测到ACTION_TECH_DISCOVERED触发
		if (isSupportedAction(action)) {
			// 取出封装在intent中的TAG
			//Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			//NfcA mfc = NfcA.get(tagFromIntent);
			try {
				if(authorised){
					//mfc.connect();
					//accreditation(mfc,secretKeyDefault);//认证
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
						data1 = ultralight.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
					}
					else
						data1 = ultralight.transceive(data0);// 每次读出来的数据为4page的数据

					result = new byte[4];
					if(data1.length<16)
						throw new AuthenticationException("please authenticate first!");
					else
						System.arraycopy(data1, 0, result, 0, 4);// 去4page中的第1page数据
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
		// 得到是否检测到ACTION_TECH_DISCOVERED触发
		if (isSupportedAction(action)) {
			// 取出封装在intent中的TAG
			//Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			//NfcA mfc = NfcA.get(tagFromIntent);
			try {
				if(authorised){
					//mfc.connect();
					//accreditation(mfc,secretKeyDefault);//认证
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
						data1 = nfcA.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
					}
					else
						data1 = nfcA.transceive(data0);// 每次读出来的数据为4page的数据

					result = new byte[4];
					if(data1.length<16)
						throw new AuthenticationException("please authenticate first!");
					else
						System.arraycopy(data1, 0, result, 0, 4);// 去4page中的第1page数据
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
	 * 读取4个页面（1个页面包含 4 个字节）
	 * @param intent
	 * @param addr:  要读取的4个页面的第一个页面的地址
	 * @param isCheckSum: 是否增加校验位
	 * @return 返回  16 个字节的数组。 如果读取失败返回 null ，如果需要认证才能读取，抛出异常。
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
		// 得到是否检测到ACTION_TECH_DISCOVERED触发
		if (isSupportedAction(action)) {
			// 取出封装在intent中的TAG
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
						result = ultralight.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
					}
					else
						result = ultralight.transceive(data0);// 每次读出来的数据为4page的数据
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
		// 得到是否检测到ACTION_TECH_DISCOVERED触发
		if (isSupportedAction(action)) {
			// 取出封装在intent中的TAG
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
						result = nfcA.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
					}
					else
						result = nfcA.transceive(data0);// 每次读出来的数据为4page的数据
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
	 * 写一个页面（1个页面包含  4个字节）
	 * @param intent
	 * @param addr  要写的页面号
	 * @param contents  四个字节长度的数组
	 * @param isCheckSum: 是否增加校验位
	 * @return 写入成功返回 true， 写入失败返回 false
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
		/*if(addr < 4){
			System.out.println(addr);
			throw new AuthenticationException("page_no must be large then four");
		}*/
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
					if(contents != null && contents.length== 4){//判断输入的数据
						//mfc.connect();
						//accreditation(mfc,secretKeyDefault);//认证
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
							data3 = ultralight.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
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
					if(contents != null && contents.length== 4){//判断输入的数据
						//mfc.connect();
						//accreditation(mfc,secretKeyDefault);//认证
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
							data3 = nfcA.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
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
	 * 认证
	 * @param intent
	 * @param key 秘钥 16 个字符（字母和数字）。
	 * @param isCheckSum: 是否增加校验位
	 * @return
	 * @throws AuthenticationException
	 */
	public boolean authentication(Intent intent, String key, boolean isCheckSum) throws AuthenticationException, Exception
	{
		String dexString = this.bytesToHexString(key.getBytes());
		return authentication_internal(intent,dexString,isCheckSum);
	}

	/**
	 * 认证
	 * @param intent
	 * @param key 秘钥 16 个字符（字母和数字）。
	 * @param isCheckSum: 是否增加校验位
	 * @return
	 * @throws AuthenticationException
	 */
	public boolean authentication(Intent intent, byte[] key, boolean isCheckSum) throws AuthenticationException, Exception
	{
		String dexString = this.bytesToHexString(key);
		return authentication_internal(intent,dexString,isCheckSum);
	}
	/**
	 * 认证
	 * @param intent
	 * @param key 秘钥 16 个字节， 用   32  个 16 进制字符的字符串表示。
	 * @param isCheckSum: 是否增加校验位
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
				if(key != null && key.length() == 32){//判断输入的数据
					byte[] data = new byte[24];
					byte[] binaryKey = hexStringToBytes(key);
					System.arraycopy(binaryKey, 0, data, 0, 16);
					System.arraycopy(binaryKey, 0, data, 16, 8);
					//mfc.connect();
					accreditation(nfcA,data,isCheckSum);//认证
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
	 * 从第 0 页开始，读取指定页数的数据，返回一个字节数组(1 页 4 个字节)
	 * @param intent:
	 * @param pageNums:  指定页数
	 * @param isCheckSum: 是否增加校验位
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
		String time_test = "初始化NFC卡时间:"+ Long.toString(long_time)+";";
		MifareClassCard mifareClassCard = null;

		try {
			mfc.connect();
			date = new Date();
			time_new = date.getTime();

			long_time=time_new-time_old;
			time_old = time_new;
			time_test+="连接NFC卡时间:"+ Long.toString(long_time)+";";
			boolean auth = false;
			int secCount = mfc.getSectorCount();
			mifareClassCard = new MifareClassCard(secCount);
			int bCount = 0;
			int bIndex = 0;
			long_time=date.getTime()-long_time;
			time_test+="连接NFC卡后到开始循环读取扇区:"+ Long.toString(long_time)+";";
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
								time_test+="写错误信息"+e.toString()+";";
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
				time_test+="第"+ (j+1) +"个扇区读取时间:"+ Long.toString(long_time)+";";
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
			time_test+="将数据装入页面:"+ Long.toString(long_time)+";";

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
		String time_test = "初始化NFC卡时间:"+ Long.toString(long_time)+";";
		MifareClassCard mifareClassCard = null;

		try {
			mfc.connect();
			date = new Date();
			time_new = date.getTime();

			long_time=time_new-time_old;
			time_old = time_new;
			time_test+="连接NFC卡时间:"+ Long.toString(long_time)+";";
			boolean auth = false;
			int secCount = mfc.getSectorCount();
			mifareClassCard = new MifareClassCard(secCount);
			int bCount = 0;
			int bIndex = 0;
			long_time=date.getTime()-long_time;
			time_test+="连接NFC卡后到开始循环读取扇区:"+ Long.toString(long_time)+";";
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
//									time_test+="写错误信息"+e.toString()+";";
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
				time_test+="第"+ (j+1) +"个扇区读取时间:"+ Long.toString(long_time)+";";
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
//				time_test+="将数据装入页面:"+ Long.toString(long_time)+";";

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
						data1 = nfcA.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
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
	 * 改变秘钥
	 * @param intent
	 * @param newKey 新的秘钥
	 * @param isCheckSum: 是否增加校验位
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

	/**
	 * 改变秘钥
	 * @param intent
	 * @param newKey 新的秘钥
	 * @param isCheckSum: 是否增加校验位
	 * @return
	 * @throws AuthenticationException
	 * @throws Exception
	 */
	public boolean writeNewKey216SC(Intent intent,String newKey,boolean isCheckSum) throws AuthenticationException,Exception
	{
		if(newKey != null && newKey.length() == 32){
			if(tagType==TagUtil.TAGUTIL_NfcA)
				return writeNewKey216SC_NfcA( intent,newKey,isCheckSum);
			else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
				throw new Exception("unimplemented");
			else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
				throw new Exception("unimplemented");
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

	private boolean writeNewKey216SC_NfcA(Intent intent, String newKey, boolean isCheckSum) throws Exception {
		boolean result = false;
		String action = intent.getAction();
		if (isSupportedAction(action)) {
//			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//			NfcA mfc = NfcA.get(tagFromIntent);
			try {
				if(authorised){
					String dataString = newKey;
					//判断输入的数据

					byte[] dataX = hexStringToBytes(dataString);
					byte[] dataY = new byte[16];
					for(int i=0;i<16;i++){
						dataY[i] = dataX[15-i];
						System.out.println("mi"+dataY[i]);
					}
					byte[] data1 = new byte[6];
					byte[] data1WithCheckSum= new byte[8];
					data1[0] = (byte) 0xA2;
					data1[1] = (byte) 0xF5;
					System.arraycopy(dataY, 8, data1, 2, 4);

					byte[] data2 = new byte[6];
					byte[] data2WithCheckSum= new byte[8];
					data2[0] = (byte) 0xA2;
					data2[1] = (byte) 0xF6;
					System.arraycopy(dataY, 12, data2, 2, 4);

					byte[] data3 = new byte[6];
					byte[] data3WithCheckSum= new byte[8];
					data3[0] = (byte) 0xA2;
					data3[1] = (byte) 0xF7;
					System.arraycopy(dataY, 0, data3, 2, 4);

					byte[] data4 = new byte[6];
					byte[] data4WithCheckSum= new byte[8];
					data4[0] = (byte) 0xA2;
					data4[1] = (byte) 0xF8;
					System.arraycopy(dataY, 4, data4, 2, 4);

//						mfc.connect();
//						accreditation(mfc,secretKeyDefault);//认证


					if(isCheckSum)
					{
						byte[] checkSum = getCheckSum(data1);
						for(int i=0;i<6;i++)
							data1WithCheckSum[i]=data1[i];
						data1WithCheckSum[6]=checkSum[0];
						data1WithCheckSum[7]=checkSum[1];
						nfcA.transceive(data1WithCheckSum);// 每次读出来的数据为4page的数据
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
						nfcA.transceive(data2WithCheckSum);// 每次读出来的数据为4page的数据
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
						nfcA.transceive(data3WithCheckSum);// 每次读出来的数据为4page的数据
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
						nfcA.transceive(data4WithCheckSum);// 每次读出来的数据为4page的数据
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

	// 写入密钥
	private boolean writeNewKey_NfcA(Intent intent,String newKey, boolean isCheckSum)  throws Exception{
		boolean result = false;
		String action = intent.getAction();
		if (isSupportedAction(action)) {
//			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//			NfcA mfc = NfcA.get(tagFromIntent);
			try {
				if(authorised){
					String dataString = newKey;
					//判断输入的数据

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
//						accreditation(mfc,secretKeyDefault);//认证


					if(isCheckSum)
					{
						byte[] checkSum = getCheckSum(data1);
						for(int i=0;i<6;i++)
							data1WithCheckSum[i]=data1[i];
						data1WithCheckSum[6]=checkSum[0];
						data1WithCheckSum[7]=checkSum[1];
						nfcA.transceive(data1WithCheckSum);// 每次读出来的数据为4page的数据
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
						nfcA.transceive(data2WithCheckSum);// 每次读出来的数据为4page的数据
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
						nfcA.transceive(data3WithCheckSum);// 每次读出来的数据为4page的数据
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
						nfcA.transceive(data4WithCheckSum);// 每次读出来的数据为4page的数据
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
	 * 设置芯片的新密码（适用于FJ8216 和   NXP216 芯片   ），在调用本方法前，要先通过 authentication216 方法进行认证。
	 * @param intent
	 * @param newPWD 四个字节的  byte 数组,新的密码
	 * @param PACK 两个字节的  byte 数组,用于设置验证成功后的返回值。
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
	 * 设置一个开始页面地址和访问方式， 该页面地址后的页面都需要认证后才可访问。访问方式有两种，0为读写访问，1 为写访问
	 * @param intent
	 * @param addr: 访问地址，
	 * @param access: 如果设置为  0, 读写操作都需要授权. 如果设置为1 ,只有写操作需要授权。
	 * @param isCheckSum: 是否增加校验位
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

	/**
	 * 设置一个开始页面地址和访问方式， 该页面地址后的页面都需要认证后才可访问。访问方式有两种，0为读写访问，1 为写访问
	 * @param intent
	 * @param addr: 访问地址，
	 * @param access: 如果设置为  0, 读写操作都需要授权. 如果设置为1 ,只有写操作需要授权。
	 * @param isCheckSum: 是否增加校验位
	 * @return
	 * @throws Exception
	 */
	public boolean setAccess216SC(Intent intent,byte addr, int access,boolean isCheckSum) throws Exception
	{
		if(tagType==TagUtil.TAGUTIL_NfcA)
			return setAccess_NfcA216SC( intent,addr, access,isCheckSum);
		else if (tagType==TagUtil.TAGUTIL_TYPE_ULTRALIGHT)
			return setAccess_MifareUltraLight( intent,addr, access);
		else if (tagType==TagUtil.TAGUTIL_TYPE_CLASSIC)
			return setAccess_MifareClassic( intent,addr, access);
		else
			throw new Exception("unknow tag Type"+ tagType+". or SelectTag first.");
	}

	//设置权限的方法是E3h的Byte3表示开始保护的起始页地址
	//E4h的Byte0的最高位等于0表示写操作受保护，等于1表示读写都受保护
	//写E3，E4时其它位不能破坏，也就是写前要先读出E3，E4。改完Byte3,Byte0最高位后再写回E3h,E4h
	/**
	 *
	 * @param intent
	 * @param addr    开始保护的起始页地址
	 * @param access  0的最高位等于0表示写操作受保护，等于1表示读写都受保护
	 * @param isCheckSum
	 * @throws Exception
	 */
	public boolean setAccess_NfcA216SC(Intent intent, byte addr, int access, boolean isCheckSum) throws Exception{
		try {

			byte[] F1 = readOnePage(intent, (byte) 0xF1,isCheckSum);
			byte[] newF1 = new byte[4];
			newF1[0]=F1[0];
			newF1[1]=F1[1];
			newF1[2]=F1[2];
			newF1[3]=addr;
			boolean result1 = writeTag(intent, (byte)0xF1, newF1, isCheckSum);

			byte[] F2 = readOnePage(intent, (byte)0xF2,isCheckSum);
			byte[] newF2 = new byte[4];
			byte oldByte = F2[0];
			int tempByte = oldByte << 1;
			byte newByte;
			if(access ==1 )
				newByte = (byte) (tempByte+128);
			else
				newByte = (byte)tempByte;

			newF2[0]=newByte;
			newF2[1]=F2[1];
			newF2[2]=F2[2];
			newF2[3]=F2[3];
			boolean result2 = writeTag(intent, (byte)0xF2, newF2, isCheckSum);


			if(result1 && result2)
				return true;
			else
				return false;


		}catch (Exception e){
			throw e;
		}


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
	 * 锁定加锁位
	 * @param intent
	 * @param isCheckSum 是否在命令后自动增加校验位。
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
	 * 锁定一个页面范围
	 * @param intent
	 * @param addr1 要锁定的开始页面
	 * @param addr2 要锁定的结束页面
	 * @param isCheckSum 是否在命令后自动增加校验位。
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
	 * 锁定为在第 40 页， 可以锁定的范围是 16 到 47 页面。
	 * lock in page 40, lock address between 16 and 47
	 * @param intent
	 * @param startAddr
	 * @param endAddr
	 * @param isCheckSum 是否在命令后自动增加校验位。
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
	 * 锁定位在第 2 页面， 可以锁定的地址范围是 3 到 15
	 * lock in page 2, lock address between 3 and 15
	 * @param startAddr
	 * @param endAddr
	 * @param isCheckSum 是否在命令后自动增加校验位。
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
	 * 锁定所有页面
	 * @param intent
	 * @param isCheckSum 是否在命令后自动增加校验位。
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
	 * 获取标签类型，目前可支持的标签类型包括  NFCA 和  UltraLight
	 * @return
	 * @throws AuthenticationException
	 */
	public int getTagType() throws AuthenticationException
	{
		return tagType;
	}

	/**
	 private void accreditation(NfcA mfc,byte[] secretKeys,boolean isCheckSum) throws Exception {
	 byte[] iv = ivDefault;

	 byte[] command0 = new byte[2];// 发送认证指令的参数
	 byte[] command0WithCheckSum = new byte[4];// 发送认证指令的参数(with check sum)

	 byte[] command1 = null;// 发送认证后，卡片返回的密文1
	 byte[] command1WithCheckSum = null;// 发送认证后，卡片返回的密文1

	 byte[] command2 = null;// 密文1去掉数组中的第1个数据,取出有效数组

	 byte[] command3 = null;// 密文1 解密后的数据
	 byte[] command4 = null;// command2 加密
	 byte[] command5 = null;// command3 循环左移得到的数据
	 byte[] command6 = null;// 使用command5 和 command4 第二次加密后的数据RNDB
	 byte[] command7 = null;//
	 byte[] command8 = null;//
	 byte[] command9 = null;//
	 byte[] command10 = null;//
	 byte[] command11 = null;//

	 command0[0] = (byte) 0x1A; // 命令位
	 command0[1] = (byte) 0x00; // 标志位
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
	 **/



	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	private void accreditation(NfcA mfc, byte[] secretKeys, boolean isCheckSum) throws Exception {
		byte[] iv = ivDefault;

		byte[] bytes0 = new byte[2];// 发送认证指令的参数
		byte[] bytes0WithCheckSum = new byte[4];// 发送认证指令的参数(with check sum)

		byte[] bytes1 = null;// 发送认证后，卡片返回的密文1
		byte[] bytes1WithCheckSum = null;// 发送认证后，卡片返回的密文1

		byte[] bytes2 = null;// 密文1去掉数组中的第1个数据,取出有效数组

		byte[] bytes3 = null;// 密文1 解密后的数据
		byte[] bytes4 = null;// bytes2 加密
		byte[] bytes5 = null;// bytes3 循环左移得到的数据
		byte[] bytes6 = null;// 使用bytes5 和 bytes4 第二次加密后的数据RNDB
		byte[] bytes7 = null;//
		byte[] bytes8 = null;//
		byte[] bytes9 = null;//
		byte[] bytes10 = null;//
		byte[] bytes11 = null;//

		bytes0[0] = (byte) 0x1A; // 命令位
		bytes0[1] = (byte) 0x00; // 标志位
		if(isCheckSum)
		{
			byte[] checkSum = getCheckSum(bytes0);
			bytes0WithCheckSum[0]=bytes0[0];
			bytes0WithCheckSum[1]=bytes0[1];
			bytes0WithCheckSum[2]=checkSum[0];
			bytes0WithCheckSum[3]=checkSum[1];
			bytes1WithCheckSum = mfc.transceive(bytes0WithCheckSum);// 11 bytes
			if(bytes1WithCheckSum.length != 11)
			{
				String str="";
				for (int i = 0 ; i<bytes1WithCheckSum.length;i++)
				{
					str = str+" byte"+i+"="+bytes1WithCheckSum[i]+"  ";
				}
				throw new Exception("length of response is not 11 bytes. the response bytes is: "+str);
			}
			bytes1= new byte[9];
			System.arraycopy(bytes1WithCheckSum, 0, bytes1, 0, 9);
			Log.i("authen","first send end");
			Log.i("authen","first send response" +bytesToHexString(bytes1));
		}
		else
			bytes1 = mfc.transceive(bytes0);

		bytes2 = new byte[8];
		if(bytes1.length != 9)
		{
			String str="";
			for (int i = 0 ; i<bytes1.length;i++)
			{
				str = str+" byte"+i+"="+bytes1[i]+"  ";
			}
			throw new Exception("length of response is not 9 bytes. the response bytes is: "+str);
		}
		System.arraycopy(bytes1, 1, bytes2, 0, 8);
		bytes3 = ThreeDES.decode(bytes2, iv, secretKeys);
		iv = bytes2;
		bytes4 = ThreeDES.encode(random, iv, secretKeys);
		iv = bytes4;
		bytes5 = new byte[8];
		System.arraycopy(bytes3, 1, bytes5, 0, 7);
		bytes5[7] = bytes3[0];
		bytes6 = ThreeDES.encode(bytes5, iv, secretKeys);

		bytes7 = new byte[16];
		System.arraycopy(bytes4, 0, bytes7, 0, 8);
		System.arraycopy(bytes6, 0, bytes7, 8, 8);
		bytes8 = new byte[17];

		bytes8[0] = (byte) 0xAF;
		System.arraycopy(bytes7, 0, bytes8, 1, 16);

		if(isCheckSum)
		{
			byte[] bytes8WithCheckSum= new byte[19];
			byte[] checkSum = getCheckSum(bytes8);
			for(int i=0;i<17;i++)
			{
				bytes8WithCheckSum[i]=bytes8[i];
			}
			bytes8WithCheckSum[17]=checkSum[0];
			bytes8WithCheckSum[18]=checkSum[1];
			Log.i("authen","sencond send:"+bytesToHexString(bytes8WithCheckSum));
			bytes9 = mfc.transceive(bytes8WithCheckSum);//
			Log.i("authen","sencond send end");
		}
		else
			bytes9 = mfc.transceive(bytes8);
		bytes10 = new byte[8];
		System.arraycopy(bytes9, 1, bytes10, 0, 8);
		iv = bytes6;
		bytes11 = ThreeDES.decode(bytes10, iv, secretKeys);
	}

	public void accreditationNtag424AES(int keyIndex, byte[] secretKeys, boolean isCheckSum) throws AuthNTAG424FailException, Exception{
		accreditationNtag424AES(nfcA,keyIndex,secretKeys,isCheckSum);
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	private void accreditationNtag424AES(NfcA mfc, int keyIndex, byte[] secretKeys, boolean isCheckSum) throws AuthNTAG424FailException, Exception {

		//RATS命令，交换双方支持的功能
		byte[] ratsCmd = hexStringToBytes("E080");
		byte[] rats_res =  mfc.transceive(ratsCmd);
		Log.i("rats_res:{}",bytesToHexString(rats_res));
		String rats_res_hex =bytesToHexString(rats_res);
		if(!TextUtils.isEmpty(rats_res_hex) && rats_res_hex.toUpperCase().startsWith("067777710280".toUpperCase())){
			byte[] selectDfFileCmd = hexStringToBytes("0200a4040007d2760000850101");
			byte[] selectDfFileRes =  mfc.transceive(selectDfFileCmd);

		}else{
			throw new Exception("Validation failure");
		}
		byte[] iv = hexStringToBytes("00000000000000000000000000000000");  // 初始 IV
		byte[] rnda = hexStringToBytes("00000000000000000000000000000000");// 初始 Reader 随机数

		String keyIndexStr="0"+new Integer(keyIndex).toString();

		byte[] command1 = hexStringToBytes("029071000008"+keyIndexStr+"0600000000000000");// 发送的第一个指令，获取芯片随机数,keyIndexStr是密钥在芯片的位置
		byte[] command1WithCheckSum = new byte[command1.length+2];// 发送的第一个指令，带2个校验字节(with check sum，部分手机需要)
		System.arraycopy(command1, 0, command1WithCheckSum, 0, command1.length);

		byte[] bytes1 = null;// 发送的第一个指令，芯片返回的完整密文1
		byte[] bytes1WithCheckSum = null;// 发送认证后，芯片返回的完整密文1，带校验字节
		byte[] bytes2 = null;// 完整密文1去掉数组中的第1个数据,取出 有效秘文

		byte[] rndbp = null;// 密文1 解密后的数据
		byte[] rndbpp = null;// rndbp 循环左移得到的数据

		byte[] bytes6 = null;// rndbpp 和 encodedIV 第二次加密后的数据 RNDB
		byte[] command2 = null;// 发送的第二个指令，

		byte[] receive = null;//
		byte[] rndapp = null;//
		byte[] bytes11 = null;//

		//////////Step1/////////////
		Log.d("424 Authen","command1:" +bytesToHexString(command1));

		if(isCheckSum)
		{
			byte[] checkSum = getCheckSum(command1);
			command1WithCheckSum[command1WithCheckSum.length-2]=checkSum[0];
			command1WithCheckSum[command1WithCheckSum.length-1]=checkSum[1];
			bytes1WithCheckSum = mfc.transceive(command1WithCheckSum);// 11 bytes
			String str="";
			Log.i("424 Authen","first command response with crc:" +bytesToHexString(bytes1WithCheckSum));
			bytes1= new byte[9];
			System.arraycopy(bytes1WithCheckSum, 0, bytes1, 0, 9);
			Log.i("424 Authen","first command response" +bytesToHexString(bytes1));
		}
		else
			bytes1 = mfc.transceive(command1);

		//////////Step2/////////////
		bytes2 = new byte[16];
		if(bytes1.length != 20)
		{
			Log.i("424 Authen","Length of response is not 20 bytes:" +bytesToHexString(bytes1));
			throw new Exception("Length of response is not 20 bytes. the response bytes is: "+bytesToHexString(bytes1));
		}
		Log.d("424 Authen","full encoded chip random:" +bytesToHexString(bytes1));

		System.arraycopy(bytes1, 1, bytes2, 0, 16);
		Log.d("424 Authen","valid encoded chip random:" +bytesToHexString(bytes2));

		rndbp = AES.decode(bytes2, iv, secretKeys);
		Log.d("424 Authen","decoded chip random:" +bytesToHexString(rndbp));

		rnda = AES.encode(rnda, iv, secretKeys);

		rndbpp = new byte[16];
		System.arraycopy(rndbp, 1, rndbpp, 0, 15);
		rndbpp[rndbpp.length-1] = rndbp[0];
		rndbpp = AES.encode(rndbpp, rnda, secretKeys);
		Log.d("424 Authen","rndbpp:" +bytesToHexString(rndbpp));

		String rndap_bpp = bytesToHexString(rnda)+ bytesToHexString(rndbpp);

		String command2Str ="0290AF000020"+rndap_bpp+"00";
		Log.d("424 Authen","command2:" +command2Str);
		command2 = hexStringToBytes(command2Str);
		if(isCheckSum)
		{
			byte[] command2WithCheckSum= new byte[command2.length+2];
			byte[] checkSum = getCheckSum(command2);
			for(int i=0;i<command2.length;i++)
			{
				command2WithCheckSum[i]=command2[i];
			}
			command2WithCheckSum[command2WithCheckSum.length-2]=checkSum[0];
			command2WithCheckSum[command2WithCheckSum.length-1]=checkSum[1];
			Log.i("424 Authen","sencond send:"+bytesToHexString(command2WithCheckSum));
			receive = mfc.transceive(command2WithCheckSum);//
			Log.i("424 Authen","sencond send end");
		}
		else
			receive = mfc.transceive(command2);
		Log.i("424 Authen","full encoded receive:"+bytesToHexString(receive));

		rndapp = new byte[32];

		try {
			System.arraycopy(receive, 1, rndapp, 0, 32);
		}catch (Exception e){
			if(receive.length > 3){
				if((receive[2] & 0x7E) == 0x7E){
					throw new AuthNTAG424FailException(bytesToHexString(rndapp),"Command size not allowed.");
				}else if((receive[2] & 0xAE) == 0xAE){
					throw new AuthNTAG424FailException(bytesToHexString(rndapp),"Wrong RndB’");
				}else if((receive[2] & 0xEE) == 0xEE){
					throw new AuthNTAG424FailException(bytesToHexString(rndapp),"Failure when reading or writing to non-volatile memory.");
				}else{
					throw e;
				}
			}else{
				throw e;
			}
		}
		Log.i("424 Authen","valid encoded receive:"+rndapp);

		byte[] andappnew = AES.decode(rndapp, iv, secretKeys);
		Log.i("424 Authen","valid decoded receive:"+bytesToHexString(andappnew));
		byte[] andappnewLast16 = new byte[16];// 解密后的后16个字节
		System.arraycopy(andappnew, andappnew.length-16,  andappnewLast16, 0, 16);

		if (!bytesToHexString(andappnewLast16).equals("00000000000000000000000000000000") ) {
			Log.i("424 Authen","full decoded receive:"+andappnew);
			throw new Exception("424 Authen failed, rndapp=" + rndapp);
		}
	}

	/**
	 * 16进制字符串转化为字节数组
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
	 * 字节数组转化为16进制字符串
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
			byte[] datar = ultralight.transceive(datas);// 每次读出来的数据为4page的数据
			byte[] datau = new byte[7];//uid号
			System.arraycopy(datar, 0, datau, 0, 3);// 去4page中的第1page数据
			System.arraycopy(datar, 4, datau, 3, 4);// 去4page中的第1page数据
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

			String atqa = bytesToHexString(nfcA.getAtqa()) ;
			Log.i("atqa：{}",atqa);
			if(!"4403".equalsIgnoreCase(atqa)){
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
					datar = nfcA.transceive(datasWithCheckSum);// 每次读出来的数据为4page的数据
				} else{
					Log.i("发送：{}",bytesToHexString(datas));
					datar = nfcA.transceive(datas);// 每次读出来的数据为4page的数据
					Log.i("返回：{}",bytesToHexString(datar));
				}

				byte[] datau = new byte[7];//uid号
				System.arraycopy(datar, 0, datau, 0, 3);// 去4page中的第1page数据
				System.arraycopy(datar, 4, datau, 3, 4);// 去4page中的第1page数据
				uid=bytesToHexString(datau);
			}


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

			byte[] datau = new byte[4];//uid号
			System.arraycopy(datar, 0, datau, 0, 3);// 4page中的第1page数据
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
	 * 访问时需要认证的页面地址范围
	 * @param intent
	 * @return 返回一个开始地址。访问该地址后的所有页面，都需要认证。 如果返回值大于等于 48 ，则 访问（读或写）所有页面都不需要认证。如果返回值小于48，说明访问（读或写）返回值地址到  48  之间的页面时，需要认证。
	 * @throws Exception
	 */
	public int getAuthenticationAddr(Intent intent,boolean isCheckSum) throws Exception
	{
		byte[] result = readOnePage(intent, (byte)PAGE_ADDR_AUTH0,isCheckSum);
		int r = result[0];
		return r;
	}

	/**
	 * 如果访问时,有地址需要认证。该方法可以获得认证的种类
	 * @param intent
	 * @param isCheckSum 是否在命令后自动增加校验位。
	 * @return  如果返回值=0 ，则读写都需要认证。如果返回值=1，只有写需要认证
	 * @throws Exception
	 */
	public int getAuthenticationType(Intent intent,boolean isCheckSum) throws Exception
	{
		byte[] result = readOnePage(intent, (byte)PAGE_ADDR_AUTH1,isCheckSum);
		int r = result[0];
		return r;
	}

	/**
	 * 关闭连接
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
	 * 使用命令
	 * java -jar aofei_nfc.jar
	 * 获取版本号
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.out.println(  (byte)0xf1 > 4);
//		try {
//			TagUtil.test1();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
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
//	     * 先认证，再读取全部页面
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
	 * 	获取计数器的值
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
			result = nfcA.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
		}
		else
			result = nfcA.transceive(data0);// 每次读出来的数据为4page的数据

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
			result = ultralight.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
		}
		else
			result = ultralight.transceive(data0);// 每次读出来的数据为4page的数据

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
	 * 使计数器功能生效或失效
	 * @param intent
	 * @param isCount true 生效， false 失效
	 * @param isCheckSum 是否在命令后自动增加校验位。
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
	 * 使用密码进行认证
	 * @param intent
	 * @param pwd 认证密码
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
//		accreditation(nfcA,key,isCheckSum);//认证
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
				PAK = nfcA.transceive(dataWithCheckSum);// 每次读出来的数据为4page的数据
			}
			else
				PAK = nfcA.transceive(data0);// 每次读出来的数据为4page的数据

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

	//设置权限的方法是E3h的Byte3表示开始保护的起始页地址
	//E4h的Byte0的最高位等于0表示写操作受保护，等于1表示读写都受保护
	//写E3，E4时其它位不能破坏，也就是写前要先读出E3，E4。改完Byte3,Byte0最高位后再写回E3h,E4h
	/**
	 *
	 * @param intent
	 * @param addr    开始保护的起始页地址
	 * @param access  0的最高位等于0表示写操作受保护，等于1表示读写都受保护
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
	 * 	取芯片的CID值，读页地址FFh，得到的16字节数据的前四个字节做为CID值返回。
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
	 * 	验证CID是否正确，KEY1表同节点1使用的表
	 * UID补00h，用key1加密，用密文高四字节与CID比较，返回比较结果。
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
	 * 三重认证第一步，向芯片发送认证指令，取得8个字节的RNDB’
	 * @param intent
	 * @return 8 bytes,response from chip
	 * @throws Exception
	 */
	public byte[] authStep1(Intent intent, boolean isCheckSum) throws Exception
	{
		byte[] command0 = new byte[2];// 发送认证指令的参数
		byte[] command0WithCheckSum = new byte[4];// 发送认证指令的参数(with check sum)

		byte[] command1 = null;// 发送认证后，卡片返回的密文
		byte[] command1WithCheckSum = null;// 发送认证后，卡片返回的密文

		byte[] command2 = null;// 由command1去掉数组中的第1个数据,取出有效数组

		command0[0] = (byte) 0x1A; // 命令位
		command0[1] = (byte) 0x00; // 标志位
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
	 * 三重认证第二步，向芯片发送由服务器返回的RNDAB，得到芯片返回的RNDA”
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
			throw new Exception("ERROR RNDA”:"+this.bytesToHexString(command9));
		}


	}

	public static boolean test1() throws Exception {
		byte[] secretKeys = hexStringToBytes("00000000000000000000000000000000");// 初始 Key
		byte[] iv = hexStringToBytes("00000000000000000000000000000000");  // 初始 IV
		byte[] rnda = hexStringToBytes("00000000000000000000000000000000");// 初始 Reader 随机数

		byte[] command1 = hexStringToBytes("029071000008000600000000000000");// 发送的第一个指令，获取芯片随机数
		byte[] command1WithCheckSum = new byte[command1.length+2];// 发送的第一个指令，带2个校验字节(with check sum，部分手机需要)
		System.arraycopy(command1, 0, command1WithCheckSum, 0, command1.length);

		byte[] bytes1 = null;// 发送的第一个指令，芯片返回的完整密文1
		byte[] bytes1WithCheckSum = null;// 发送认证后，芯片返回的完整密文1，带校验字节
		byte[] bytes2 = null;// 完整密文1去掉数组中的第1个数据,取出 有效秘文

		byte[] rndbp = null;// 密文1 解密后的数据
		byte[] rndbpp = null;// rndbp 循环左移得到的数据

		byte[] bytes6 = null;// rndbpp 和 encodedIV 第二次加密后的数据 RNDB
		byte[] command2 = null;// 发送的第二个指令，

		byte[] receive = null;//
		byte[] rndapp = null;//
		byte[] bytes11 = null;//
		//////////Step2/////////////
		bytes1= hexStringToBytes("0281123909B33E01476B96E355D480316291AF");
		bytes2 = new byte[16];

		System.arraycopy(bytes1, 1, bytes2, 0, 16);
		//Log.d("424 Authen","valid encoded chip random:" +bytesToHexString(bytes2));
		System.out.println(bytesToHexString(bytes2));
		rndbp = AES.decode(bytes2, iv, secretKeys);

		System.out.println("Result1: "+bytesToHexString(rndbp));
		System.out.println("Correct: 817A7C74CAFB3259D8849B2EE402CFDA");
		//Log.d("424 Authen","decoded chip random:" +bytesToHexString(rndbp));

		rnda = AES.encode(rnda, iv, secretKeys);
		System.out.println("Result2: "+bytesToHexString(rnda));
		System.out.println("Correct: 66E94BD4EF8A2C3B884CFA59CA342B2E");


		rndbpp = new byte[16];
		System.arraycopy(rndbp, 1, rndbpp, 0, 15);
		rndbpp[rndbpp.length-1] = rndbp[0];

		System.out.println("Result3: "+bytesToHexString(rndbpp));
		System.out.println("Correct: 7A7C74CAFB3259D8849B2EE402CFDA81");

		rndbpp = AES.encode(rndbpp, rnda, secretKeys);
		System.out.println("Result4: "+bytesToHexString(rndbpp));
		System.out.println("Correct: EF6649EF347D8FAC242C908C2425F18E");

		String rndap_bpp = bytesToHexString(rnda)+ bytesToHexString(rndbpp);
		System.out.println(rndap_bpp);

		return false;
	}

}
