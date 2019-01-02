package com.aofei.nfc;

public class KeyUtil {

	public static String keys[] = new String[]{
			"0925A9458322B9CC0014958288AAC3DF",
			"59301BF5C321D9761720663E53AD0926",
			"5322763A334546B627435611270D09F6",
			"335577AA0123450091950211AB30226E",
			"223E562160924FBC1099230B2F99DE0A",
			"0912FFE003982011F930BBAA04923B45",
			"19301CF5325CDAFBA3D5F67725675265",
			"ABF762525456BFEA67290AD2156BFED1"
	};

	/**
	 * return different keys according to the flag
	 * @param flag
	 * @return
	 */
	public static String getKey(byte flag)
	{
		int mod = flag%8;
		String key = null;
		key = keys[mod];
		return key;
	}
}
