package com.example.advanceandroid.views;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.advanceandroid.R;
import com.example.advanceandroid.views.PullRefreshView.PullToRefreshListener;

/**
 * 原因 : // View中的dispatchTouchEvent方法的源码： public boolean
 * dispatchTouchEvent(MotionEvent event) { if (mOnTouchListener != null &&
 * (mViewFlags & ENABLED_MASK) == ENABLED && mOnTouchListener.onTouch(this,
 * event)) { return true; } // onClick事件在onTouchEvent中 return
 * onTouchEvent(event); }
 * 
 * @author mrsimple
 * 
 */
public class ViewTouchActivity extends Activity {

	private View mTouchBtn = null;
	private View mTouchEventInterceptBtn = null;
	private View mImgView = null;

	private PullRefreshView mPullRefreshView = null;
	private ListView mListView = null;
	private ArrayAdapter mAdapter = null;
	String[] items = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
			"L" };

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_touch);

		initViews();
	}

	/**
	 * 
	 */
	private void initViews() {
		mTouchBtn = findViewById(R.id.touch_handle_btn1);
		mTouchBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("", "#### onClick点击事件");
			}
		});

		mTouchBtn.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.d("", "#### onTouch执行,并且返回false.事件继续传递,然后执行onClick.");
				return false;
			}
		});

		mTouchEventInterceptBtn = findViewById(R.id.touch_handle_btn2);
		mTouchEventInterceptBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("", "#### (onClick) 事件已被处理, 这句话不会被打印出来.");
			}
		});

		mTouchEventInterceptBtn.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.d("", "#### onTouch执行， 并且返回true.事件已被处理.");
				return true;
			}
		});

		mImgView = findViewById(R.id.image_view);
		// mImgView.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		// Log.d("", "#### (onClick) 事件已被处理, 这句话不会被打印出来.");
		// }
		// });

		mImgView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.d("",
						"#### onTouch执行， 并且返回false.事件已被处理. "
								+ event.getAction());
				return false;
			}
		});

		mPullRefreshView = (PullRefreshView) findViewById(R.id.pull_refresh);
		mListView = (ListView) findViewById(R.id.list_view);
		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, items);
		mListView.setAdapter(mAdapter);
		mPullRefreshView.setOnRefreshListener(0, new PullToRefreshListener() {
			@Override
			public void onRefresh() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
