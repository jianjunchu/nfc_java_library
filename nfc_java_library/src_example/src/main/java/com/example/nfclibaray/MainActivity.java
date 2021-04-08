/*
 * Copyright 2011, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nfclibaray;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import com.aofei.nfc.AuthenticationException;
import com.aofei.nfc.TagUtil;

public class MainActivity extends Activity {
    private static final String TAG = "stickynotes";
    private boolean mResumed = false;
    private boolean mWriteMode = false;
    NfcAdapter mNfcAdapter;
    EditText mNote;
    public static int counter;

    PendingIntent mNfcPendingIntent;
    TagUtil tagUtil = null;
    PendingIntent pendingIntent=null;
    private boolean writeModelFlag = false;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        setContentView(R.layout.main);
        findViewById(R.id.clear_screen).setOnClickListener(mScreenClear);
        findViewById(R.id.write_tag).setOnClickListener(mTagWriter);
        findViewById(R.id.read_tag).setOnClickListener(mTagReader);
        mNote = ((EditText) findViewById(R.id.note));
        mNote.setText("test");
        mNote.setText(new Integer(counter).toString());
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);


    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        mResumed = true;
        Intent intent = getIntent();
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
//        	 if(tagUtil == null)
//        		 try {
// 					tagUtil = TagUtil.selectTag(intent,true);//选卡
// 				}catch (Exception e) {
// 					e.printStackTrace();
// 				}
//        	 byte[] pwd = new byte[]{(byte)0XFF,(byte)0XFF,(byte)0XFF,(byte)0XFF};
//        try {
//				tagUtil.authentication216(intent, pwd, false);
//				byte[] contents = tagUtil.readAllPages(intent,false);
//
//			} catch (AuthenticationException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}


//        	 if(tagUtil ==null)
//        	 {
//        		 mNote.setText("can't select tag");
//        		 return;
//        	 }
//        	 boolean case1Result = testReadPageWithoutAuthentication(intent);
//        	 boolean case2Result = testWritePageWithoutAuthentication(intent);
//
//        	 testTag(tagUtil,intent);
//        	 try {
//        		 //tagUtil.enableCounter(intent, true, false);
//        		 tagUtil.getCount(intent, true);
//				}catch (Exception e) {
//					e.printStackTrace();
//				}


// if  authentication needed
//             try {
//            	    //tagUtil.readOnePage(intent,(byte)1,true);
//            	    tagUtil.readAllPages(intent,false);
//             		tagUtil.authentication(intent, "4A35B5D5454151522140515355405847",false); //认证
//             } catch (Exception e) {
//            	 mNote.setText("authentication failed");
//            	 mNote.setText(e.getMessage());
// 				e.printStackTrace();
// 				return;
// 			}

//lock all pages
//             try {
// 					if(tagUtil.lockPageAll(intent, false))
// 					{
// 						counter++;
// 						mNote.setText(new Integer(counter).toString());  //完成操作
// 					}
// 					else
// 					{
// 						mNote.setText("lock failed");
// 					}
// 				}
// 			 catch (Exception e) {
// 				e.printStackTrace();
//         }

//get CID and verifyCID
//           try {
//        	   byte[] cid = tagUtil.getCID(intent, false);
//				mNote.setText("CID="+TagUtil.bytesToHexString(cid));  //完成操作
//				boolean result = tagUtil.verifyCID(intent, TagUtil.hexStringToBytes(tagUtil.getUid()), cid);
//				mNote.setText(" Check Result="+result);  //完成操作
//			}
//			 catch (Exception e) {
//				e.printStackTrace();
//       }
        }
    }

    private String testTag(TagUtil tagUtil2,Intent intent) {

        //byte[] contents = new byte[]{1,4,8,18};
        byte[] contents = new byte[4];
        byte[] bytes = null;
        try{
            bytes = new String("中").getBytes("UTF-8");
        }catch(Exception ex)
        {
            ex.printStackTrace();
        }
        if(bytes.length==3)
        {
            contents[0] = bytes[0];
            contents[1] = bytes[1];
            contents[2] = bytes[2];
            contents[3] = 0;
        }
        if(bytes.length==2)
        {
            contents[0] = bytes[0];
            contents[1] = bytes[1];
            contents[2] = 0;
            contents[3] = 0;
        }
        //byte[] contents = new byte[]{(byte)0x4e,(byte)0x2d,(byte)0x56,(byte)0xfd};

        try {
            tagUtil.writeTag(intent, (byte)4, contents, false);
            byte[] result = tagUtil.readOnePage(intent, (byte)4,false);
            String s = new String(result,"utf-8");
            if(result == null)
                throw new Exception("read one page is null");
//    		if(result[0]!=1 || result[1]!=4|| result[2]!=8|| result[3]!=18)
//    			throw new Exception("read one page data error");
        }catch (Exception e) {
            e.printStackTrace();
        }

        try {
            tagUtil.authentication216(intent, new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF}, false);
        } catch (AuthenticationException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }


        byte[] result = null;
        //can't read here

        try {
            result = tagUtil.readOnePage(intent, (byte)5,false);
            throw new Exception("Error: writeTag test failed! reading should not be permited here!");
        }catch (Exception e) {
            e.printStackTrace();
        }

//    	try {
//			tagUtil.authentication216(intent, new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF}, false);
//		} catch (AuthenticationException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}

        try {
            tagUtil.enableCounter(intent, true, false);
            tagUtil.getCount(intent, true);
        }catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
        mNfcAdapter.disableForegroundNdefPush(this);
    }

    protected void onNewIntent(Intent intent) {

        TagUtil tagUtil=null;




        //第一步 创建一个 TagUtil 对象 TagUtil tagUtil=null; if(tagUtil == null)
        try {
            tagUtil = TagUtil.selectTag(intent,false); //选卡操作,其中false 参数说明是 否增加校验值，对少数手机需要设置为true。
        }catch (Exception e) {
            e.printStackTrace();
        }

        /*try{
            //4A35B5D5454151522140515355405847
            boolean s = tagUtil.authentication_internal(intent, "4A35B5D5454151522140515355474747",false);
            if(s){
                Log.e("kkk", "认证成功");
            }else{
                Log.e("kkk", "认证失败");
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }

        try{
            boolean s  = tagUtil.setAccess216SC(intent, (byte)5, 1, false);
            if(s){
                Log.e("kkk", "设置读写认证成功");
            }else{
                Log.e("kkk", "设置读写认证失败");
            }

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }

        if(true){
            return;
        }*/


        //第二步 使用 tagUtil 对象完成各种操作，如 try{
        try{
            byte[] bytes = tagUtil.readAllPages(intent,15,false);//读取出5页面的内容, 其中false 参数说明是否增加校验值，对少数手机需要设置为true。

            Log.e("kkk",new String(bytes));
            for (int i=0;i<bytes.length;i++) Log.e("kkk","byte "+i +" is "+bytes[i]);

        }catch (Exception ex) {
            ex.printStackTrace();
        }



        try{

            byte[] bytes = "TestTest写入测试123".getBytes("UTF-8");

            boolean s  = tagUtil.writeTag(intent,(byte)5,bytes,false);
            if(s){
                Log.e("kkk", "写入成功");
            }else{
                Log.e("kkk", "写入失败");
            }

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }

        try{
            byte[] bytes = tagUtil.readAllPages(intent,15,false);//读取出5页面的内容, 其中false 参数说明是否增加校验值，对少数手机需要设置为true。

            Log.e("kkk",new String(bytes));
            for (int i=0;i<bytes.length;i++) Log.e("kkk","byte "+i +" is "+bytes[i]);

        }catch (Exception ex) {
            ex.printStackTrace();
        }




       /* try{
            //4A35B5D5454151522140515355474747
           boolean s =  tagUtil.writeNewKey216SC(intent,"4A35B5D5454151522140515355474747",false);
            if(s){
                Log.e("kkk", "设置新密码成功");
            }else{
                Log.e("kkk", "设置新密码失败");
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }*/




        /*try{
            boolean s  = tagUtil.setAccess216SC(intent, (byte)5, 0, false);
            if(s){
                Log.e("kkk", "设置读写认证成功");
            }else{
                Log.e("kkk", "设置读写认证失败");
            }

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }*/

        /*try{

            byte[] bytes = "TestTest写入测试".getBytes("UTF-8");

            boolean s  = tagUtil.writeTag(intent,(byte)5,bytes,false);
            if(s){
                Log.e("kkk", "写入成功");
            }else{
                Log.e("kkk", "写入失败");
            }

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }*/


        //第二步 使用 tagUtil 对象完成各种操作，如 try{
        /*try{
            byte[] bytes = tagUtil.readAllPages(intent,9,false);//读取出5页面的内容, 其中false 参数说明是否增加校验值，对少数手机需要设置为true。

            Log.e("kkk",new String(bytes));
            for (int i=0;i<bytes.length;i++) Log.e("kkk","byte "+i +" is "+bytes[i]);

        }catch (Exception ex) {
            ex.printStackTrace();
        }*/



        /*try{
            //4A35B5D5454151522140515355405847
            tagUtil.authentication_internal(intent, "4A35B5D5454151522140515355474747",false);
            Log.e("kkk", "authentication success"); }catch (Exception ex)
        {
            ex.printStackTrace();
        }*/

        /*try{
            //4A35B5D5454151522140515355474747
            tagUtil.writeNewKey216SC(intent,"4A35B5D5454151522140515355405847",false);
            Log.e("kkk", "writeNewKey216SC success"); }catch (Exception ex)
        {
            ex.printStackTrace();
        }*/

        /*try{
            tagUtil.authentication(intent, "4A35B5D5454151522140515355405847",false);
            Log.e("kkk", "old key authentication success"); }catch (Exception ex)
        {
            Log.e("kkk", "old key authentication failure");
            ex.printStackTrace();
        }

        try{
            tagUtil.authentication(intent, "4A35B5D5454151522140515355474747",false);
            Log.e("kkk", "new  key authentication success"); }catch (Exception ex)
        {
            Log.e("kkk", "new  key authentication failure");
            ex.printStackTrace();
        }



        try{
            tagUtil.setAccess216SC(intent, (byte)5, 1, false);
            Log.e("kkk", "设置读写认证"); }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        */

    }

    private void writeTag(Intent intent,byte page,byte[] content) throws AuthenticationException, Exception {
        if(tagUtil == null)
            try {
                tagUtil = TagUtil.selectTag(intent,false);
            }catch (Exception e) {
                if(e!=null)
                {
                    e.printStackTrace();
                    mNote.setText("Exception: \r\n" +e.getMessage());
                }
            }
        tagUtil.writeTag(intent,page,content, false);
    }


    //不认证直接读加密标签的标签
    private boolean testReadPageWithoutAuthentication(Intent intent) {
        byte[] result;
        try{
            result = tagUtil.readOnePage(intent, (byte)5,false);
            String s = new String(result,"utf-8");
            if(result == null)
                return true;
            else
            {
                System.out.println("test read page failed. should not come here!");
                return false;
            }
        }catch(Exception ex)
        {
            ex.printStackTrace();
            System.out.println("test read page after authentication success!");
            return true;
        }
    }

    //不认证直接读加密标签的标签
    private boolean testWritePageWithoutAuthentication(Intent intent) {
        byte[] contents = new byte[]{1,4,8,18};
        try {

            tagUtil.writeTag(intent, (byte)5, contents, false);
            return false;
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("test write page after authentication success!");
            return true;
        }

    }

    //修改密码的例子
    private boolean testWritePWD216(Intent intent) {
        byte[] oldPWD = new byte[]{(byte)0XFF,(byte)0XFF,(byte)0XFF,(byte)0XFF};//默认密码（旧密码）
        byte[] PWD = new byte[]{1,2,3,4};//要设置的新密码
        byte[] PACK = new byte[]{1,2};//要设置的密码验证成功的返回值。
        try {
            boolean result = tagUtil.authentication216(intent, oldPWD, false);//修改密码钱要先认证，使用旧密码
            if(result)
                tagUtil.writePWD216(intent, PWD, PACK, false);//验证成功后修改密码
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private void setNoteBody(String body) {
        Editable text = mNote.getText();
        text.clear();
        text.append(body);
    }


    private View.OnClickListener mScreenClear = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            counter=0;
            mNote.setText(new Integer(counter).toString());
        }
    };

    private View.OnClickListener mTagWriter = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            writeModelFlag =true;
        }
    };

    private View.OnClickListener mTagReader = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            writeModelFlag =false;
        }
    };

    public static String bytes2HexString(byte[] b) {
        String ret = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[ i ] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }


}
