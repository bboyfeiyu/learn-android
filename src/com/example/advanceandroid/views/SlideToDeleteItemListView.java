package com.example.advanceandroid.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Scroller;

public class SlideToDeleteItemListView extends ListView {

	/**
	 * 手指按下X的坐标
	 */
	private int downY;
	/**
	 * 手指按下Y的坐标
	 */
	private int downX;
	/**
	 * 屏幕宽度
	 */
	private int mScreenWidth;

	/**
	 * 当前滑动的ListView　position
	 */
	private int mSelectedPosition = -1;

	/**
	 * ListView的item
	 */
	private View mItemView = null;

	/**
	 * 滑动控制类
	 */
	private Scroller mScroller = null;

	private static final int SNAP_VELOCITY = 600;

	/**
	 * 速度追踪对象
	 */
	private VelocityTracker mVelocityTracker = null;

	/**
	 * 是否响应滑动，默认为不响应
	 */
	private boolean isSliding = false;

	/**
	 * 认为是用户滑动的最小距离
	 */
	private int mTouchSlop;

	/**
	 * 移除item后的回调接口
	 */
	private RemoveListener mRemoveListener;
	/**
	 * 用来指示item滑出屏幕的方向,向左或者向右,用一个枚举值来标记
	 */
	private RemoveDirection removeDirection;

	// 滑动删除方向的枚举值
	public enum RemoveDirection {
		RIGHT, LEFT;
	}

	/**
	 * 
	 * @param context
	 */
	public SlideToDeleteItemListView(Context context) {
		this(context, null);
	}

	public SlideToDeleteItemListView(Context context, AttributeSet attr) {
		this(context, attr, 0);
	}

	/**
	 * 
	 * @param context
	 * @param attr
	 * @param style
	 */
	public SlideToDeleteItemListView(Context context, AttributeSet attr,
			int style) {
		super(context, attr, style);

		WindowManager winManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		// 获取屏幕宽度
		mScreenWidth = winManager.getDefaultDisplay().getWidth();
		mScroller = new Scroller(context);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}

	/**
	 * 分发事件，主要做的是判断点击的是那个item, 以及通过postDelayed来设置响应左右滑动事件
	 */
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if ( mScroller == null ) {
			mScroller = new Scroller(getContext()) ;
		}
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// 添加速度跟踪器
			addVelocityTracker(ev);
			// 还在滚动， 直接返回
			if (!mScroller.isFinished()) {
				return super.dispatchTouchEvent(ev);
			}
			downX = (int) ev.getX();
			downY = (int) ev.getY();

			// 根据x, y的位置获取到被选中的item position.
			mSelectedPosition = pointToPosition(downX, downY);
			if (mSelectedPosition == AdapterView.INVALID_POSITION) {
				return super.dispatchTouchEvent(ev);
			}

			// 获取用户选中的item
			mItemView = getChildAt(mSelectedPosition
					- getFirstVisiblePosition());
			break;

		case MotionEvent.ACTION_MOVE:
			if (Math.abs(getXScrollVelocity()) > SNAP_VELOCITY
					|| (Math.abs(ev.getX() - downX) > mTouchSlop && Math.abs(ev
							.getY() - downY) < mTouchSlop)) {
				isSliding = true;
			}
			break;

		case MotionEvent.ACTION_UP:
			// 移除用户速度跟踪器
			recycleVelocityTracker();
			break;
		}
		return super.dispatchTouchEvent(ev);
	}

	/**
	 * 处理我们拖动ListView item的逻辑
	 */
	@Override
	public boolean onTouchEvent(MotionEvent ev) {

		if (isSliding && mSelectedPosition != AdapterView.INVALID_POSITION) {
			// listview不要拦截item view触控事件
			requestDisallowInterceptTouchEvent(true);
			addVelocityTracker(ev);

			final int action = ev.getAction();
			int x = (int) ev.getX();

			switch (action) {
			case MotionEvent.ACTION_DOWN:

				break;

			case MotionEvent.ACTION_MOVE:
				MotionEvent cancelEvent = MotionEvent.obtain(ev);
				cancelEvent
						.setAction(MotionEvent.ACTION_CANCEL
								| (ev.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
				// 滑动的X坐标差
				int deltaX = downX - x;
				downX = x;
				// item view在X轴上滑动deltaX单位, deltaX大于0向左滚动，小于0向右滚
				mItemView.scrollBy(deltaX, 0);
				// 滑动时返回true, 使得item view继续消费掉该事件, 不再进行传递
				return true;

			case MotionEvent.ACTION_UP:
				int velocity = getXScrollVelocity();
				if (velocity > SNAP_VELOCITY) {
					scrollRight();
				} else if (velocity < -SNAP_VELOCITY) {
					scrollLeft();
				} else {
					scrollByDistanceX();
				}
				recycleVelocityTracker();
				isSliding = false;
				break;

			default:
				break;
			}
		}
		return super.onTouchEvent(ev);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#computeScroll()
	 */
	@Override
	public void computeScroll() {
		if (mScroller != null && mScroller.computeScrollOffset()) {
			mItemView.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();

			if (mScroller.isFinished()) {
				if (mRemoveListener == null) {
					new NullPointerException("mRemoveListener is null.");
				} else {
					mRemoveListener.removeItem(removeDirection,
							mSelectedPosition);
				}

				mItemView.scrollTo(0, 0);

			}
		}
	}

	/**
	 * 往右滑动，getScrollX()返回的是左边缘的距离，就是以View左边缘为原点到开始滑动的距离，所以向右边滑动为负值
	 */
	private void scrollRight() {
		removeDirection = RemoveDirection.RIGHT;
		final int delta = (mScreenWidth + mItemView.getScrollX());
		// 调用startScroll方法来设置一些滚动的参数，我们在computeScroll()方法中调用scrollTo来滚动item
		mScroller.startScroll(mItemView.getScrollX(), 0, -delta, 0,
				Math.abs(delta));
		postInvalidate(); // 刷新itemView
	}

	/**
	 * 向左滑动，根据上面我们知道向左滑动为正值
	 */
	private void scrollLeft() {
		removeDirection = RemoveDirection.LEFT;
		final int delta = (mScreenWidth - mItemView.getScrollX());
		// 调用startScroll方法来设置一些滚动的参数，我们在computeScroll()方法中调用scrollTo来滚动item
		mScroller.startScroll(mItemView.getScrollX(), 0, delta, 0,
				Math.abs(delta));
		postInvalidate(); // 刷新itemView
	}

	/**
	 * 根据手指滚动itemView的距离来判断是滚动到开始位置还是向左或者向右滚动
	 */
	private void scrollByDistanceX() {
		// 如果向左滚动的距离大于屏幕的二分之一，就让其删除
		if (mItemView.getScrollX() >= mScreenWidth / 2) {
			scrollLeft();
		} else if (mItemView.getScrollX() <= -mScreenWidth / 2) {
			scrollRight();
		} else {
			// 滚回到原始位置,为了偷下懒这里是直接调用scrollTo滚动
			mItemView.scrollTo(0, 0);
		}

	}

	/**
	 * 添加用户的速度跟踪器
	 * 
	 * @param event
	 */
	private void addVelocityTracker(MotionEvent event) {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}

		mVelocityTracker.addMovement(event);
	}

	/**
	 * 移除用户速度跟踪器
	 */
	private void recycleVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	/**
	 * 获取X方向的滑动速度,
	 * 
	 * @return int 大于0向右滑动，反之向左
	 */
	private int getXScrollVelocity() {
		mVelocityTracker.computeCurrentVelocity(1000);
		int velocity = (int) mVelocityTracker.getXVelocity();
		return velocity;
	}

	/**
	 * 
	 * @return
	 */
	public RemoveListener getRemoveListener() {
		return mRemoveListener;
	}

	/**
	 * 
	 * @param listener
	 */
	public void setRemoveListener(RemoveListener listener) {
		this.mRemoveListener = listener;
	}

	/**
	 * 
	 * 当ListView item滑出屏幕，回调这个接口 我们需要在回调方法removeItem()中移除该Item,然后刷新ListView
	 * 
	 * @author xiaanming
	 * 
	 */
	public interface RemoveListener {
		public void removeItem(RemoveDirection direction, int position);
	}
}
