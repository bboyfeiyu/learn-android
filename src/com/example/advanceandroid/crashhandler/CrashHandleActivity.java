package com.example.advanceandroid.crashhandler;

import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class CrashHandleActivity extends Activity {

	private View mNullView = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 设置未捕获的异常, 主要用于记录异常信息, 例如程序崩溃时记录崩溃的原因并且发送给服务器等.
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				sendCrashLogToServer(ex);
			}
		});

	}

	/**
	 * 
	 */
	@Override
	protected void onResume() {
		super.onResume();
		// 引发程序奔溃的代码
		mNullView.getBottom();
	}

	/**
	 * 处理异常, 例如发送到服务器
	 * 
	 * @param ex
	 */
	private void sendCrashLogToServer(Throwable ex) {
		Log.e("", "### 程序Crash信息捕获, " + getExceptionDetail(ex));
		ex.printStackTrace();
	}

	/**
	 * 获取异常的详细信息
	 * 
	 * @param e
	 * @return
	 */
	private String getExceptionDetail(Throwable e) {
		StackTraceElement[] stkElements = e.getStackTrace();
		String trace = "";
		for (StackTraceElement stackTraceElement : stkElements) {
			trace += "\tat " + stackTraceElement + " \n";
		}
		return "异常 :\n " + e.getLocalizedMessage() + ",\n 原因 : "
				+ e.getCause().toString() + ",\n 调用栈 : " + trace;
	}
}
