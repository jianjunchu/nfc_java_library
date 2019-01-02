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
import android.content.IntentFilter;
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
         Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
         
            //if(tagUtil == null)
            try {
					tagUtil = TagUtil.selectTag(intent,false);
				}
            catch (TagLostException ex1) {
				if(ex1 != null)
				{
					mNote.setText("Tag lost or Unsupported NFC Chip\r\n" +ex1.getMessage());
				}
			}catch (Exception ex2) {
				if(ex2 !=null )
				{
					mNote.setText("Error\r\n" +ex2.getMessage());
				}
			}
            
            if(writeModelFlag)
            {
            	String content = mNote.getText().toString();
            	byte[] contentByte = TagUtil.hexStringToBytes(content);
            	try {
    				String s =  ((EditText) findViewById(R.id.input_text)).getText().toString();
    				byte page_no = new Integer(s).byteValue();		
					writeTag(intent,page_no, contentByte);
				} catch (AuthenticationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.e("xxx", e.getMessage());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.e("xxx", e.getMessage());
				}
            }
            else
            {

            byte[] content=null;
			try {

				
//authenticaiton216 demo
//			  	byte[] pwd = new byte[]{(byte)0XFF,(byte)0XFF,(byte)0XFF,(byte)0XFF};
//			  	boolean result = false;
//		        try {
//		        	result = tagUtil.authentication216(intent, pwd, false);					
//				}catch(Exception ex)
//				{
//					ex.printStackTrace();
//				}
//		        
//				mNote.setText("result="+result);		
				
//read page content demo
				byte[] contents =null;
				
				String s =  ((EditText) findViewById(R.id.input_text)).getText().toString();
				byte page_no ;
				if(s==null||s.trim().length()==0)
					page_no=5;
				else
					page_no = new Integer(s).byteValue();
				//content = tagUtil.readOnePage(intent,page_no, false);
				content = tagUtil.readAllPages(intent,228,false);
				
				String uid = tagUtil.getUid();
				if(uid!=null && content!=null)
					//mNote.setText("UID="+uid+"    Content="+TagUtil.bytesToHexString(content));
					mNote.setText("UID="+uid+"    Content="+new String(content));
				
//				content = tagUtil.readAllPages(intent, false);
//				mNote.setText(TagUtil.bytesToHexString(content));
			} catch (AuthenticationException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
            
            		
          //get CID and verifyCID
//            boolean result=false;
//            try {  
//         	   byte[] cid = tagUtil.getCID(intent, false);
// 				mNote.setText("CID="+TagUtil.bytesToHexString(cid));  //完成操作
// 				result = tagUtil.verifyCID(intent, TagUtil.hexStringToBytes(tagUtil.getUid()), cid);
// 				mNote.setText(" Check Result="+result);  //完成操作
// 				}
// 			 catch (Exception e) {
// 				e.printStackTrace();
// 				mNote.setText(" exception Check Result="+result);  //完成操作
//        }       	 
      
            
//            try {
//       		 //tagUtil.enableCounter(intent, true, false);
//            
//            	
////            try {
////        			tagUtil.authentication216(intent, new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF}, false);
////        		} catch (AuthenticationException e1) {
////        			// TODO Auto-generated catch block
////        			e1.printStackTrace();
////        		} catch (Exception e1) {
////        			// TODO Auto-generated catch block
////        			e1.printStackTrace();
////        		}
////            	
////            try {
////        			tagUtil.setAccess216(intent, (byte)5, 1, false);
////        		}
////        		catch (Exception e) {
////        			e.printStackTrace();
////        	 }
//            
//            
//             boolean case1Result = testReadPageWithoutAuthentication(intent);	 
//             boolean case2Result = testWritePageWithoutAuthentication(intent);
//             boolean case3Result = testWritePWD216(intent);
//             
//             if(!case1Result)
//            	 Log.e("failed","case 1 failed");	
//             
//             if(!case2Result)
//            	 Log.e("failed","case 2 failed");
//             
//             testTag(tagUtil,intent);
//            	 
//       		 int c =tagUtil.getCount(intent, false);
//       		 mNote.setText("读取次数: \r\n" +c);
//			}catch (Exception e) {
//					e.printStackTrace();
//			}
            
// if  authentication needed            
//            try {
//            	
//            		tagUtil.authentication(intent, "4A35B5D5454151522140515355405847",false);
//            		Log.e("aaa","authentication success"); 
//            } catch (Exception e) {
//				//e.printStackTrace();
//            		Log.e("aaa","authentication failed"); 
//			}
            
//for FJ8010 3des demo            
//            boolean authenticationFlag = false;
//            try {
//            	tagUtil.authentication(intent, "4A35B5D5454151522140515355405847",false);
//            	authenticationFlag = true;
//            	Log.e("aaa","authentication success");
//            	//tagUtil.readOnePage(intent,(byte)5,false);            	            	
//            } catch (Exception e) {
//            	e.printStackTrace();
//            	Log.e("error","authentication failed");
//            	mNote.setText(e.getMessage());  
//            	e.printStackTrace();
//			}
//            if(authenticationFlag)
//            {
//	            try {            
//	        		for(int i=4;i<15;i++){
//	        			byte[] data = tagUtil.readOnePage(intent, (byte) i,false);
//	        			mNote.setText(mNote.getText().toString()+"page "+i+": "+bytes2HexString(data)+"\n");
//	        		} 
//				}
//			 catch (Exception e) {
//				// TODO Auto-generated catch block
//				 mNote.setText(mNote.getText().toString()+"\n5: "+e.getMessage());
//				 e.printStackTrace();
//			 }
//            }     
            
//lock page demo
//            try {
//					if(tagUtil.lockPageAll(intent,false))
//					{
//						counter++;
//						mNote.setText(new Integer(counter).toString());
//					}
//					else
//					{
//						mNote.setText("lock failed");								
//					}
//				}
//			 catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//        }
            
//get CID and verifyCID demo
//            byte[] cid = null;
//            try {
//         	   cid = tagUtil.getCID(intent, false);
// 				mNote.setText("CID="+TagUtil.bytesToHexString(cid));  //完成操作
// 				boolean result = tagUtil.verifyCID(intent, TagUtil.hexStringToBytes(tagUtil.getUid()), cid);
// 				mNote.setText(" Check Result="+result);  //完成操作
// 				}
// 			 catch (Exception e) {
// 				e.printStackTrace();
//        }
//            
//            try {
//            	if(cid == null)
//            	{
//            		boolean result  = tagUtil.verifyCID(intent,TagUtil.hexStringToBytes(tagUtil.getUid()), cid);
//            		mNote.setText(" Check Result="+result);
//            	}else
//            	{
//            		mNote.setText("cid is null");
//            	}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
            
//两步验证方法
//            byte[] secretKeys= {0x49, 0x45, 0x4D, 0x4B, 0x41, 0x45, 0x52, 0x42
//                  , 0x21, 0x4E, 0x41, 0x43, 0x55, 0x4F, 0x59, 0x46
//                  , 0x49, 0x45, 0x4D, 0x4B, 0x41, 0x45, 0x52, 0x42};    //24字节的密钥
//            
//            com.aofei.nfc.Verifier verifier = new com.aofei.nfc.Verifier();
//            try {
//				byte[] array1 = tagUtil.authStep1(intent,false);
//				Log.i("aaa", "===array"+array1);
//				byte[][] result1 = verifier.verifyStep1(array1, secretKeys);
//				
//				byte[] array2 = tagUtil.authStep2(intent,result1[0],false);
//				 byte[] result2 = verifier.verifyStep2(result1[1],array2,secretKeys);
//				 if(result2!=null)
//				 {
//					 String str =tagUtil.bytesToHexString(result2);
//					 Log.i("aaa", str);
//				 }
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
         }
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