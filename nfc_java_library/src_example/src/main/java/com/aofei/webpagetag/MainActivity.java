package com.aofei.webpagetag;

import com.aofei.nfc.TagAccessActivity;
import com.aofei.websitetag.fragment.MainFragment;
import com.example.nfclibaray.R;

import android.os.Bundle;

public class MainActivity extends TagAccessActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_base);

		getFragmentManager().beginTransaction().add(R.id.container, new MainFragment()).commit();
	}

}
