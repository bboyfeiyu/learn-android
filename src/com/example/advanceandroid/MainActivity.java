package com.example.advanceandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

import com.example.advanceandroid.crashhandler.CrashHandleActivity;
import com.example.advanceandroid.views.SlideToDeleteActivity;
import com.example.advanceandroid.views.ViewTouchActivity;

public class MainActivity extends Activity {

	private View mCrashButton = null;
	private View mViewActivityButton = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initViews();
	}

	/**
	 * 
	 */
	private void initViews() {
		mCrashButton = findViewById(R.id.crash_handle_btn) ;
		mCrashButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, CrashHandleActivity.class) ;
				startActivity(intent);
			}
		});
		
		mViewActivityButton = findViewById(R.id.view_btn) ;
		mViewActivityButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
//				Intent intent = new Intent(MainActivity.this, ViewTouchActivity.class) ;
				Intent intent = new Intent(MainActivity.this, SlideToDeleteActivity.class) ;
				startActivity(intent);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
