package com.aofei.nfc;

import java.security.Key;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * 3DES加密工具类
 */
public class ThreeDES {
	private final static byte[] secretKey = {0x49, 0x45, 0x4D, 0x4B, 0x41, 0x45, 0x52, 0x42
            , 0x21, 0x4E, 0x41, 0x43, 0x55, 0x4F, 0x59, 0x46
            , 0x49, 0x45, 0x4D, 0x4B, 0x41, 0x45, 0x52, 0x42};    //24字节的密钥

    // 向量
    private final static byte[] iv = {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
    // 加解密统一使用的编码方式
    private final static String encoding = "utf-8";

    static{
        Security.addProvider(new com.sun.crypto.provider.SunJCE());
    }
    /**
     * 3DES加密
     *
     * @param plainText 普通文本
     * @param ivs 向量
     * @return
     * @throws Exception
     */
    public static byte[] encode(byte[] plainText,byte[] ivs,byte[] secretKeys) throws Exception {
        Key deskey = null;
        DESedeKeySpec spec = new DESedeKeySpec(secretKeys);
        SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
        deskey = keyfactory.generateSecret(spec);

        Cipher cipher = Cipher.getInstance("desede/CBC/NoPadding");
        IvParameterSpec ips = new IvParameterSpec(ivs);
        cipher.init(Cipher.ENCRYPT_MODE, deskey, ips);
        byte[] encryptData = cipher.doFinal(plainText);

        return encryptData;
    }

    /**
     * 3DES解密
     *
     * @param encryptText 加密文本
     * @param ivs 向量
     * @return
     * @throws Exception
     */
    public static byte[] decode(byte[] encryptText,byte[] ivs,byte[] secretKeys) throws Exception {
        Key deskey = null;
        DESedeKeySpec spec = new DESedeKeySpec(secretKeys);
        SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
        deskey = keyfactory.generateSecret(spec);
        Cipher cipher = Cipher.getInstance("desede/CBC/NoPadding");
        IvParameterSpec ips = new IvParameterSpec(ivs);
        cipher.init(Cipher.DECRYPT_MODE, deskey, ips);
        byte[] decryptData = cipher.doFinal(encryptText);

        return decryptData;
    }
}  
