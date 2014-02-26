package com.example.advanceandroid.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.advanceandroid.R;

public class PullRefreshView extends LinearLayout implements OnTouchListener {

	/**
	 * 下拉状态
	 */
	public static final int STATUS_PULL_TO_REFRESH = 0;

	/**
	 * 释放立即刷新状态
	 */
	public static final int STATUS_RELEASE_TO_REFRESH = 1;

	/**
	 * 正在刷新状态
	 */
	public static final int STATUS_REFRESHING = 2;

	/**
	 * 刷新完成或未刷新状态
	 */
	public static final int STATUS_REFRESH_FINISHED = 3;

	/**
	 * 下拉头部回滚的速度
	 */
	public static final int SCROLL_SPEED = -20;

	/**
	 * 一分钟的毫秒值，用于判断上次的更新时间
	 */
	public static final long ONE_MINUTE = 60 * 1000;

	/**
	 * 一小时的毫秒值，用于判断上次的更新时间
	 */
	public static final long ONE_HOUR = 60 * ONE_MINUTE;

	/**
	 * 一天的毫秒值，用于判断上次的更新时间
	 */
	public static final long ONE_DAY = 24 * ONE_HOUR;

	/**
	 * 一月的毫秒值，用于判断上次的更新时间
	 */
	public static final long ONE_MONTH = 30 * ONE_DAY;

	/**
	 * 一年的毫秒值，用于判断上次的更新时间
	 */
	public static final long ONE_YEAR = 12 * ONE_MONTH;

	/**
	 * 上次更新时间的字符串常量，用于作为SharedPreferences的键值
	 */
	private static final String UPDATED_AT = "updated_at";

	/**
	 * 下拉刷新的回调接口
	 */
	private PullToRefreshListener mRefreshListener;

	/**
	 * 用于存储上次更新时间
	 */
	private SharedPreferences mSharedPreferences;

	/**
	 * 下拉头的View
	 */
	private View mHeaderView = null;

	/**
	 * 需要去下拉刷新的ListView
	 */
	private ListView mListView;

	/**
	 * 刷新时显示的进度条
	 */
	private ProgressBar mProgressBar;

	/**
	 * 指示下拉和释放的箭头
	 */
	private ImageView mImageArrow;

	/**
	 * 指示下拉和释放的文字描述
	 */
	private TextView mDescTextView;

	/**
	 * 上次更新时间的文字描述
	 */
	private TextView mLastUpdateTextView;

	/**
	 * 下拉头的布局参数
	 */
	private MarginLayoutParams mHeaderLayoutParams;

	/**
	 * 上次更新时间的毫秒值
	 */
	private long lastUpdateTime;

	/**
	 * 为了防止不同界面的下拉刷新在上次更新时间上互相有冲突，使用id来做区分
	 */
	private int mId = -1;

	/**
	 * 下拉头的高度
	 */
	private int hideHeaderHeight;

	/**
	 * 当前处理什么状态，可选值有STATUS_PULL_TO_REFRESH, STATUS_RELEASE_TO_REFRESH,
	 * STATUS_REFRESHING 和 STATUS_REFRESH_FINISHED
	 */
	private int currentStatus = STATUS_REFRESH_FINISHED;;

	/**
	 * 记录上一次的状态是什么，避免进行重复操作
	 */
	private int lastStatus = currentStatus;

	/**
	 * 手指按下时的屏幕纵坐标
	 */
	private float yDown;

	/**
	 * 在被判定为滚动之前用户手指可以移动的最大值。
	 */
	private int touchSlop;

	/**
	 * 是否已加载过一次layout，这里onLayout中的初始化只需加载一次
	 */
	private boolean loadOnce;

	/**
	 * 当前是否可以下拉，只有ListView滚动到头的时候才允许下拉
	 */
	private boolean ableToPull;

	private final String TAG = this.getClass().getSimpleName();

	// /**
	// *
	// * @param context
	// */
	// public PullRefreshView(Context context) {
	// super(context);
	// initView(context);
	// }

	/**
	 * 
	 * @param context
	 * @param attrs
	 */
	public PullRefreshView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	/**
	 * 初始化
	 */
	private void initView(Context context) {
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		// ListView的Header
		mHeaderView = LayoutInflater.from(context).inflate(
				R.layout.pull_to_refresh_header, null, true);
		mProgressBar = (ProgressBar) mHeaderView
				.findViewById(R.id.progress_bar);
		mImageArrow = (ImageView) mHeaderView.findViewById(R.id.arrow);
		mDescTextView = (TextView) mHeaderView.findViewById(R.id.description);
		mLastUpdateTextView = (TextView) mHeaderView
				.findViewById(R.id.updated_at);
		// 在被判定为滚动之前用户手指可以移动的最大值
		touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

		refreshUpdatedAtValue();
		setOrientation(VERTICAL);
		addView(mHeaderView, 0);
	}

	/**
	 * 进行一些关键性的初始化操作，比如：将下拉头向上偏移进行隐藏，给ListView注册touch事件。
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);

		if (changed && !loadOnce) {
			hideHeaderHeight = -mHeaderView.getHeight();
			mHeaderLayoutParams = (MarginLayoutParams) mHeaderView
					.getLayoutParams();
			// 隐藏header view
			mHeaderLayoutParams.topMargin = hideHeaderHeight;
			// 初始化ListView
			mListView = (ListView) getChildAt(1);
			// 坚挺onTouch事件， 实现下拉刷新
			mListView.setOnTouchListener(this);
			loadOnce = true;
		}
	}

	/**
	 * 当ListView被触摸时调用，其中处理了各种下拉刷新的具体逻辑。
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		setIsPullable(event); 
		if (ableToPull) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				yDown = event.getRawY();
				break;
			case MotionEvent.ACTION_MOVE:
				float yMove = event.getRawY();
				int distance = (int) (yMove - yDown);
				// 如果手指是下滑状态，并且下拉头是完全隐藏的，就屏蔽下拉事件
				if (distance <= 0
						&& mHeaderLayoutParams.topMargin <= hideHeaderHeight) {
					return false;
				}
				if (distance < touchSlop) {
					return false;
				}
				Log.d(TAG, "#### currentStatus : " + currentStatus) ;
				if (currentStatus != STATUS_REFRESHING) {
					if (mHeaderLayoutParams.topMargin > 0) {
						currentStatus = STATUS_RELEASE_TO_REFRESH;
					} else {
						currentStatus = STATUS_PULL_TO_REFRESH;
					}
					// 通过偏移下拉头的topMargin值，来实现下拉效果
					mHeaderLayoutParams.topMargin = (distance / 2)
							+ hideHeaderHeight;
					Log.d(TAG, "### header view的topMargin : " + mHeaderLayoutParams.topMargin) ;
					mHeaderView.setLayoutParams(mHeaderLayoutParams);
				}
				break;
			case MotionEvent.ACTION_UP:
			default:
				if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
					// 松手时如果是释放立即刷新状态，就去调用正在刷新的任务
					new RefreshingTask().execute();
				} else if (currentStatus == STATUS_PULL_TO_REFRESH) {
					// 松手时如果是下拉状态，就去调用隐藏下拉头的任务
					new HideHeaderViewTask().execute();
				}
				break;
			} // end of switch
			// 时刻记得更新下拉头中的信息
			if (currentStatus == STATUS_PULL_TO_REFRESH
					|| currentStatus == STATUS_RELEASE_TO_REFRESH) {
				updateHeaderView();
				// 当前正处于下拉或释放状态，要让ListView失去焦点，否则被点击的那一项会一直处于选中状态
				mListView.setPressed(false);
				mListView.setFocusable(false);
				mListView.setFocusableInTouchMode(false);
				lastStatus = currentStatus;
				// 当前正处于下拉或释放状态，通过返回true屏蔽掉ListView的滚动事件
				return true;
			}
		}
		return false;
	}
	/**
	 * 
	 * @param listener
	 */
	public void setOnRefreshListener(int id, PullToRefreshListener listener) {
		mId = id;
		mRefreshListener = listener;
	}

	/**
	 * 当所有的刷新逻辑完成后，记录调用一下，否则你的ListView将一直处于正在刷新状态。
	 */
	public void refreshComplete() {
		currentStatus = STATUS_REFRESH_FINISHED;
		mSharedPreferences.edit()
				.putLong(UPDATED_AT + mId, System.currentTimeMillis()).commit();
		new HideHeaderViewTask().execute();
	}

	/**
	 * 
	 * @param event
	 */
	private void setIsPullable(MotionEvent event) {

		// 第一个view, 用于判断是否拉倒了最顶端
		View firstChild = mListView.getChildAt(0);
		if (firstChild != null) {
			int firstVisiblePos = mListView.getFirstVisiblePosition();
			// 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新
			if (firstVisiblePos == 0) {
				if (!ableToPull) {
					yDown = event.getRawY();
				}
				// 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新
				ableToPull = true;
			} else {
				if (mHeaderLayoutParams.topMargin != hideHeaderHeight) {
					mHeaderLayoutParams.topMargin = hideHeaderHeight;
					mHeaderView.setLayoutParams(mHeaderLayoutParams);
				}
				ableToPull = false;
			}
		} else {
			// 如果ListView中没有元素，也应该允许下拉刷新
			ableToPull = true;
		}
	}

	/**
	 * 刷新下拉头中上次更新时间的文字描述。
	 */
	private void refreshUpdatedAtValue() {
		lastUpdateTime = mSharedPreferences.getLong(UPDATED_AT + mId, -1);
		long currentTime = System.currentTimeMillis();
		long timePassed = currentTime - lastUpdateTime;
		long timeIntoFormat;
		String updateAtValue;
		if (lastUpdateTime == -1) {
			updateAtValue = getResources().getString(R.string.not_updated_yet);
		} else if (timePassed < 0) {
			updateAtValue = getResources().getString(R.string.time_error);
		} else if (timePassed < ONE_MINUTE) {
			updateAtValue = getResources().getString(R.string.updated_just_now);
		} else if (timePassed < ONE_HOUR) {
			timeIntoFormat = timePassed / ONE_MINUTE;
			String value = timeIntoFormat + "分钟";
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		} else if (timePassed < ONE_DAY) {
			timeIntoFormat = timePassed / ONE_HOUR;
			String value = timeIntoFormat + "小时";
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		} else if (timePassed < ONE_MONTH) {
			timeIntoFormat = timePassed / ONE_DAY;
			String value = timeIntoFormat + "天";
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		} else if (timePassed < ONE_YEAR) {
			timeIntoFormat = timePassed / ONE_MONTH;
			String value = timeIntoFormat + "个月";
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		} else {
			timeIntoFormat = timePassed / ONE_YEAR;
			String value = timeIntoFormat + "年";
			updateAtValue = String.format(
					getResources().getString(R.string.updated_at), value);
		}
		mLastUpdateTextView.setText(updateAtValue);
	}

	/**
	 * 更新下拉头中的信息。
	 */
	private void updateHeaderView() {
		if (lastStatus != currentStatus) {
			if (currentStatus == STATUS_PULL_TO_REFRESH) {
				mDescTextView.setText(getResources().getString(
						R.string.pull_to_refresh));
				mImageArrow.setVisibility(View.VISIBLE);
				mProgressBar.setVisibility(View.GONE);
				rotateArrow();
			} else if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
				mDescTextView.setText(getResources().getString(
						R.string.release_to_refresh));
				mImageArrow.setVisibility(View.VISIBLE);
				mProgressBar.setVisibility(View.GONE);
				rotateArrow();
			} else if (currentStatus == STATUS_REFRESHING) {
				mDescTextView.setText(getResources().getString(
						R.string.refreshing));
				mProgressBar.setVisibility(View.VISIBLE);
				mImageArrow.clearAnimation();
				mImageArrow.setVisibility(View.GONE);
			}
			refreshUpdatedAtValue();
		}
	}

	/**
	 * 根据当前的状态来旋转箭头。
	 */
	private void rotateArrow() {
		float pivotX = mImageArrow.getWidth() / 2f;
		float pivotY = mImageArrow.getHeight() / 2f;
		float fromDegrees = 0f;
		float toDegrees = 0f;
		if (currentStatus == STATUS_PULL_TO_REFRESH) {
			fromDegrees = 180f;
			toDegrees = 360f;
		} else if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
			fromDegrees = 0f;
			toDegrees = 180f;
		}
		RotateAnimation animation = new RotateAnimation(fromDegrees, toDegrees,
				pivotX, pivotY);
		animation.setDuration(100);
		animation.setFillAfter(true);
		mImageArrow.startAnimation(animation);
	}

	/**
	 * 使当前线程睡眠指定的毫秒数。
	 * 
	 * @param time
	 *            指定当前线程睡眠多久，以毫秒为单位
	 */
	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 正在刷新的任务，在此任务中会去回调注册进来的下拉刷新监听器。
	 * 
	 * @author mrsimple
	 * 
	 */
	private class RefreshingTask extends AsyncTask<Void, Integer, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			int topMargin = mHeaderLayoutParams.topMargin;
			while (true) {
				topMargin = topMargin + SCROLL_SPEED;
				if (topMargin <= 0) {
					topMargin = 0;
					break;
				}
				publishProgress(topMargin);
				sleep(10);
			}
			currentStatus = STATUS_REFRESHING;
			publishProgress(0);
			if (mRefreshListener != null) {
				Log.d(TAG, "### 刷新中");
				mRefreshListener.onRefresh();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... topMargin) {
			updateHeaderView();
			mHeaderLayoutParams.topMargin = topMargin[0];
			mHeaderView.setLayoutParams(mHeaderLayoutParams);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		protected void onPostExecute(Void result) {
			 refreshComplete();
		};
	}

	/**
	 * 隐藏下拉头的任务，当未进行下拉刷新或下拉刷新完成后，此任务将会使下拉头重新隐藏。
	 * 
	 * @author guolin
	 */
	class HideHeaderViewTask extends AsyncTask<Void, Integer, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
			int topMargin = mHeaderLayoutParams.topMargin;
			while (true) {
				topMargin = topMargin + SCROLL_SPEED;
				if (topMargin <= hideHeaderHeight) {
					topMargin = hideHeaderHeight;
					break;
				}
				publishProgress(topMargin);
				sleep(10);
			}
			Log.d(TAG, "### 隐藏header view.");
			return topMargin;
		}

		@Override
		protected void onProgressUpdate(Integer... topMargin) {
			mHeaderLayoutParams.topMargin = topMargin[0];
			mHeaderView.setLayoutParams(mHeaderLayoutParams);
		}

		@Override
		protected void onPostExecute(Integer topMargin) {
			mHeaderLayoutParams.topMargin = topMargin;
			mHeaderView.setLayoutParams(mHeaderLayoutParams);
			currentStatus = STATUS_REFRESH_FINISHED;
		}
	}

	/**
	 * 
	 * @author mrsimple
	 * 
	 */
	public interface PullToRefreshListener {

		/**
		 * 刷新时会去回调此方法，在方法内编写具体的刷新逻辑。注意此方法是在子线程中调用的， 你可以不必另开线程来进行耗时操作。
		 */
		void onRefresh();
	}

}
