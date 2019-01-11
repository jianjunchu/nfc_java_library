

package com.aofei.nfc;

import jonelo.jacksum.JacksumAPI;
import jonelo.jacksum.algorithm.AbstractChecksum;
import jonelo.jacksum.ui.ExitStatus;
import jonelo.sugar.util.ExitException;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.nio.ByteBuffer;


public class Test {


    private int width=16;
    private long poly =(new BigInteger("1021", 16)).longValue();
    private long initialValue= (new BigInteger("c6c6", 16)).longValue();
    private boolean refIn=true;
    private boolean refOut=true;
    private long xorOut= (new BigInteger("0", 16)).longValue();;
    private long[] table;
    private long value = 0L;
    private long length = 0L;
    private long topBit;
    private long maskAllBits;
    private long maskHelp;

    public static void main(String[] args)
    {

        try {
            Test test = new Test();
            String hexString = Long.toHexString(test.getCRC16(ThreeDES.secretKey));
            byte[] reversedBytes = TagUtil.reverse(TagUtil.hexStringToBytes(hexString));
            System.out.println(TagUtil.bytesToHexString(reversedBytes));
            } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void init() {
        this.topBit = 1L << this.width - 1;
        this.maskAllBits = -1L >>> 64 - this.width;
        this.maskHelp = this.maskAllBits >>> 8;
        initTable();
        this.length = 0L;
        this.value = this.initialValue;
        if (this.refIn) {
            this.value = reverse(this.value, this.width);
        }

    }



    private void exec(byte[] bytes, int start, int length) {
        for(int i = start; i < length + start; ++i) {
            this.exec(bytes[i]);
        }
    }

    private void exec(byte var1) {
        int var2;
        if (this.refIn) {
            var2 = (int)(this.value ^ (long)var1) & 255;
            this.value >>>= 8;
            this.value &= this.maskHelp;
        } else {
            var2 = (int)(this.value >>> this.width - 8 ^ (long)var1) & 255;
            this.value <<= 8;
        }
        this.value ^= this.table[var2];
        ++this.length;
    }

    public long getCRC16(byte[] bytes) {
        init();
        exec(bytes, 0, bytes.length);
        long result = this.value;
        if (this.refIn != this.refOut) {
            result = reverse(result, this.width);
        }
        return (result ^ this.xorOut) & this.maskAllBits;
    }


    private void initTable() {
        this.table = new long[256];

        for(int i = 0; i < 256; ++i) {
            long var1 = (long)i;
            if (refIn) {
                var1 = reverse(var1, 8);
            }
            var1 <<= this.width - 8;
            for(int j = 0; j < 8; ++j) {
                boolean var3 = (var1 & this.topBit) != 0L;
                var1 <<= 1;
                if (var3) {
                    var1 ^= this.poly;
                }
            }

            if (refIn) {
                var1 = reverse(var1, this.width);
            }

            this.table[i] = var1 & this.maskAllBits;
        }

    }

    private static long reverse(long var, int width) {
        long temp = 0L;
        for(int i = 0; i < width; ++i) {
            temp <<= 1;
            temp |= var & 1L;
            var >>>= 1;
        }
        return var << width | temp;
    }

//    public void reset() {
//        this.topBit = 1L << this.width - 1;
//        this.maskAllBits = -1L >>> 64 - this.width;
//        this.maskHelp = this.maskAllBits >>> 8;
//        fillTable();
//        this.length = 0L;
//        this.value = this.initialValue;
//        if (this.refIn) {
//            this.value = reflect(this.value, this.width);
//        }
//
//    }
//
//    public void update(byte[] var1, int var2, int var3) {
//        for(int var4 = var2; var4 < var3 + var2; ++var4) {
//            this.update(var1[var4]);
//        }
//
//    }
//
//    private long getFinal() {
//        long var1 = this.value;
//        if (this.refIn != this.refOut) {
//            var1 = reflect(var1, this.width);
//        }
//
//        return (var1 ^ this.xorOut) & this.maskAllBits;
//    }
//
//    public void update(byte[] var1) {
//        reset();
//        this.update(var1, 0, var1.length);
//    }
//
//    public void update(byte var1) {
//        int var2;
//        if (this.refIn) {
//            var2 = (int)(this.value ^ (long)var1) & 255;
//            this.value >>>= 8;
//            this.value &= this.maskHelp;
//        } else {
//            var2 = (int)(this.value >>> this.width - 8 ^ (long)var1) & 255;
//            this.value <<= 8;
//        }
//        this.value ^= this.table[var2];
//        ++this.length;
//    }
//
//    private void fillTable() {
//        this.table = new long[256];
//
//        for(int var4 = 0; var4 < 256; ++var4) {
//            long var1 = (long)var4;
//            if (this.refIn) {
//                var1 = reflect(var1, 8);
//            }
//
//            var1 <<= this.width - 8;
//
//            for(int var5 = 0; var5 < 8; ++var5) {
//                boolean var3 = (var1 & this.topBit) != 0L;
//                var1 <<= 1;
//                if (var3) {
//                    var1 ^= this.poly;
//                }
//            }
//
//            if (this.refIn) {
//                var1 = reflect(var1, this.width);
//            }
//
//            this.table[var4] = var1 & this.maskAllBits;
//        }
//
//    }
//    public static String bytesToHexString(byte[] src) {
//        StringBuilder stringBuilder = new StringBuilder();
//        if (src == null || src.length <= 0) {
//            return null;
//        }
//        for (int i = 0; i < src.length; i++) {
//            int v = src[i] & 0xFF;
//            String hv = Integer.toHexString(v);
//            if (hv.length() < 2) {
//                stringBuilder.append(0);
//            }
//            stringBuilder.append(hv);
//        }
//        return stringBuilder.toString();
//    }
//
//    public static byte[] getCheckSum(byte[] byteAyyay) throws Exception {
//        AbstractChecksum checksum=null;
//        try {
//            String checksumArg="crc:16,1021,c6c6,true,true,0";
//            checksum = JacksumAPI.getChecksumInstance(checksumArg,false);
//        } catch (NoSuchAlgorithmException nsae) {
//            throw new ExitException(nsae.getMessage()+"\nUse -a <code> to specify a valid one.\nFor help and a list of all supported algorithms use -h.\nExit.", ExitStatus.PARAMETER);
//        }
//        checksum.setEncoding(AbstractChecksum.HEX);
//        //byte[] byteAyyay = hexStringToBytes(string);
//        checksum.update(byteAyyay);
//        String hexValue = checksum.getHexValue();
//        //String resultStr =checksum.toString();//d97c 02a8
//        byte[] result = reverse(hexStringToBytes(hexValue));
//        return result;
//    }
//
//    private static byte[] reverse(byte[] bytes)
//    {
//        byte[] result= new byte[bytes.length];
//
//        for(int i=0;i<bytes.length;i++)
//        {
//            result[i]=bytes[bytes.length-i-1];
//        }
//        return result;
//    }
//
//    /**
//     * 16进制字符串转化为字节数组
//     */
//    public static byte[] hexStringToBytes(String hexString) {
//        if (hexString == null || hexString.equals("")) {
//            return null;
//        }
//        hexString = hexString.toUpperCase();
//        int length = hexString.length() / 2;
//        char[] hexChars = hexString.toCharArray();
//        byte[] d = new byte[length];
//        for (int i = 0; i < length; i++) {
//            int pos = i * 2;
//            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
//        }
//        return d;
//    }
//
//    private static byte charToByte(char c) {
//        int i = "0123456789ABCDEF".indexOf(c);
//        if(i == -1){
//            throw new NumberFormatException();
//        }else{
//            return (byte)i;
//        }
//    }
//
//    private static long reflect(long var0, int var2) {
//        long var3 = 0L;
//
//        for(int var5 = 0; var5 < var2; ++var5) {
//            var3 <<= 1;
//            var3 |= var0 & 1L;
//            var0 >>>= 1;
//        }
//
//        return var0 << var2 | var3;
//    }
}
