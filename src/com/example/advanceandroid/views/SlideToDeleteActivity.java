package com.example.advanceandroid.views;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.advanceandroid.R;
import com.example.advanceandroid.views.SlideToDeleteItemListView.RemoveDirection;
import com.example.advanceandroid.views.SlideToDeleteItemListView.RemoveListener;

public class SlideToDeleteActivity extends Activity implements RemoveListener {

	private SlideToDeleteItemListView slideCutListView;
	private ArrayAdapter<String> adapter;
	private List<String> dataSourceList = new ArrayList<String>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.slide_to_delete_activity);
		init();
	}

	/**
	 * 
	 */
	private void init() {
		slideCutListView = (SlideToDeleteItemListView) findViewById(R.id.slideCutListView);
		slideCutListView.setRemoveListener(this);

		for (int i = 0; i < 20; i++) {
			dataSourceList.add("滑动删除" + i);
		}

		adapter = new ArrayAdapter<String>(this, R.layout.listview_item,
				R.id.listview_item, dataSourceList);
		slideCutListView.setAdapter(adapter);

		slideCutListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Toast.makeText(SlideToDeleteActivity.this,
						dataSourceList.get(position), Toast.LENGTH_SHORT)
						.show();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.example.advanceandroid.views.SlideToDeleteItemListView.RemoveListener
	 * #removeItem(com.example.advanceandroid.views.SlideToDeleteItemListView.
	 * RemoveDirection, int)
	 */
	@Override
	public void removeItem(RemoveDirection direction, int position) {
		adapter.remove(adapter.getItem(position));
		switch (direction) {
		case RIGHT:
			Toast.makeText(this, "向右删除  " + position, Toast.LENGTH_SHORT)
					.show();
			break;
		case LEFT:
			Toast.makeText(this, "向左删除  " + position, Toast.LENGTH_SHORT)
					.show();
			break;

		default:
			break;
		}

	}

}
