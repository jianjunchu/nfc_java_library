package com.aofei.nfc;

import android.annotation.TargetApi;
import android.os.Build;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class AES {
    public static final String ivSpec = "v7AxZs3hNRQUz44a";
    public static final String pass = "Yjz7BRR4Rp0WJfV8";
//
//    /**
//     * 加密
//     *
//     * @param content
//     *            需要加密的内容
//     * @param message
//     *            加密密码
//     * @return /
//     */
//    @TargetApi(Build.VERSION_CODES.KITKAT)
//    public static byte[] encode(String content, String message) {
//        try {
//            IvParameterSpec iv = new IvParameterSpec(ivSpec.getBytes(StandardCharsets.UTF_8));
//            SecretKeySpec keySpec = new SecretKeySpec(message.getBytes(StandardCharsets.UTF_8), "AES");
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");// 创建密码器
//            byte[] byteContent = content.getBytes(StandardCharsets.UTF_8);
//            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);// 初始化
//            return cipher.doFinal(byteContent); // 加密
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }catch (NoSuchPaddingException e) {
//            throw new RuntimeException(e);
//        }catch (InvalidKeyException e) {
//            throw new RuntimeException(e);
//        }catch (IllegalBlockSizeException e) {
//            throw new RuntimeException(e);
//        }catch (BadPaddingException e) {
//            throw new RuntimeException(e);
//        } catch (InvalidAlgorithmParameterException e) {
//            throw new RuntimeException(e);
//        }
//        return null;
//    }

//    /**
//     * 解密
//     *
//     * @param content
//     *            待解密内容
//     * @param message
//     *            解密密钥
//     */
//    @TargetApi(Build.VERSION_CODES.KITKAT)
//    public static byte[] decode(byte[] content, String message) throws NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException {
//        try {
//            IvParameterSpec iv = new IvParameterSpec(ivSpec.getBytes(StandardCharsets.UTF_8));
//            SecretKeySpec keySpec = new SecretKeySpec(message.getBytes(StandardCharsets.UTF_8), "AES");
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");// 创建密码器
//            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);// 初始化
//            return cipher.doFinal(content); // 加密
//        } catch (NoSuchAlgorithmException  e) {
//            e.printStackTrace();
//            throw e;
//        } catch (BadPaddingException e) {
//            e.printStackTrace();
//            throw e;
//        }catch (IllegalBlockSizeException e) {
//            e.printStackTrace();
//            throw e;
//        }catch (InvalidKeyException e) {
//            e.printStackTrace();
//            throw e;
//        } catch (NoSuchPaddingException e) {
//            e.printStackTrace();
//            throw e;
//        }
//        catch (InvalidAlgorithmParameterException e) {
//            throw new RuntimeException(e);
//        }
//    }



    /**
     * 加密
     *
     * @param content
     *            需要加密的内容
     * @param message
     *            加密密码
     * @return /
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static byte[] encode(byte[] content,byte[] iv, byte[] message) {
        try {
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(message, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");// 创建密码器
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);// 初始化
            return cipher.doFinal(content); // 加密
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        }catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static byte[] decode(byte[] content,byte[] iv ,byte[] message) throws NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException {
        try {
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(message, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");// 创建密码器
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);// 初始化
            return cipher.doFinal(content); // 加密
        } catch (NoSuchAlgorithmException  e) {
            e.printStackTrace();
            throw e;
        } catch (BadPaddingException e) {
            e.printStackTrace();
            throw e;
        }catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            throw e;
        }catch (InvalidKeyException e) {
            e.printStackTrace();
            throw e;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            throw e;
        }
        catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将二进制转换成16进制
     *
     * @param buf
     * @return
     */
    public static String parseByte2HexStr(byte buf[]) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 将16进制转换为二进制
     *
     * @param hexStr
     * @return
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String decp() throws IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String content ="25EE9C9A3CC0F449FE153A7768C20F3B086BB173274BA729C534EC27211641D794E335C3714A7D1E16AE5E535D14AC2CA4EF546475FCBC02B541FD8BEFFDFE25D64100C91D97E393967714730488CC14BF59FB940579516DD3A7A3314091AB71";
        String password = pass;
        byte[] decode = AES.parseHexStr2Byte(content);
        byte[] decryptResult=AES.decode(decode, ivSpec.getBytes(), pass.getBytes());
        String data = new String(decryptResult != null ? decryptResult : new byte[0], StandardCharsets.UTF_8);
        System.out.println(data);
        return data;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void main(String[] args) throws UnsupportedEncodingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {

 //       AES.decp();
        String content = "我是shoneworn";
        String password = "Yjz7BRR4Rp0WJfV8";
        // 加密
        System.out.println("加密前：" + content);
        byte[] encode = encode(content.getBytes(),ivSpec.getBytes(), password.getBytes());

        //传输过程,不转成16进制的字符串，就等着程序崩溃掉吧
        String code = parseByte2HexStr(encode);
        System.out.println("密文字符串：" + code);
        byte[] decode = parseHexStr2Byte(code);
        // 解密
        byte[] decryptResult = decode(decode,ivSpec.getBytes(), password.getBytes());
        System.out.println("解密后：" + new String(decryptResult, StandardCharsets.UTF_8)); //不转码会乱码
//
    }
}