package com.charonchui.cyberlink;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

	public BaseActivity() {
	}

	public BaseActivity(int contentLayoutId) {
		super(contentLayoutId);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DMCApplication.getInstance().addActivity(this);
	}

	@Override
	protected void onDestroy() {
		DMCApplication.getInstance().removeActivity(this);
		super.onDestroy();
	}
}
