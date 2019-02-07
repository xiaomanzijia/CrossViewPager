package me.licheng.crossviewpager2;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.Adapter;

import java.util.ArrayList;

public class ViewPagerContainer extends ViewGroup {

    private final static String TAG = ViewPagerContainer.class.getSimpleName();

    private static final int DEFAULT_COL_COUNT = 2; //默认列
    private static final int DEFAULT_ROW_COUNT = 4; //默认行
    private static final int DEFAULT_GRID_GAP = 8; //默认格子间隙

    private int mColCount = DEFAULT_COL_COUNT; //列数
    private int mRowCount = DEFAULT_ROW_COUNT; //行数
    private int mPageSize = mColCount * mRowCount; //一页的格子数目

    private int mGridGap; //格子间隙


    private int mPageCount; //页面数
    private int mGridWidth; //格子宽
    private int mGridHeight; //格子高

    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;

    private int mMaxOverScrollSize; //最大滚动距离
    private int mEdgeSize;


    private int mCurItem; // Index of currently displayed page.


    private Adapter mAdapter;
    private float mLastMotionX;
    private float mLastMotionY;
    private int mLastPosition;
    private int mLastDragged;
    private final static int SCROLL_STATE_IDLE = 0;
    private int mScrollState = SCROLL_STATE_IDLE;
    private int mLastTarget;

    private ArrayList<Integer> newPositions = new ArrayList<Integer>();
    private static final long ANIMATION_DURATION = 150; // ms




    public ViewPagerContainer(Context context) {
        this(context, null);
    }

    public ViewPagerContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewPagerContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final float density = context.getResources().getDisplayMetrics().density;

        mGridGap = (int) (DEFAULT_GRID_GAP * density);

        mPaddingLeft = getPaddingLeft();
        mPaddingTop = getPaddingTop();
        mPaddingRight = getPaddingRight();
        mPaddingBottom = getPaddingBottom();
        super.setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom);

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int childCount = getChildCount();
        mPageCount = (childCount + mPageSize - 1) / mPageSize;
        mGridWidth = (getWidth() - mPaddingLeft - mPaddingRight - (mColCount - 1) * mGridGap) / mColCount;
        mGridHeight = (getHeight() - mPaddingTop - mPaddingBottom - (mRowCount - 1) * mGridGap) / mRowCount;
        mGridWidth = mGridHeight = Math.min(mGridWidth, mGridHeight); //取宽高最小值
        mMaxOverScrollSize = mGridWidth / 2;
        mEdgeSize = mGridWidth / 2;
        newPositions.clear();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final Rect rect = getRectByPosition(i);
            child.measure(MeasureSpec.makeMeasureSpec(rect.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(rect.height(), MeasureSpec.EXACTLY));
            Log.i(TAG, "child onLayout-->  position: " + i + ", rect: " + rect);
            child.layout(rect.left, rect.top, rect.right, rect.bottom);
            newPositions.add(-1);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = event.getX();
                mLastMotionY = event.getY();


                mLastPosition = getPositionByXY((int) mLastMotionX, (int) mLastMotionY);

                Log.i(TAG, "touch dowan at lastPosition --> " + mLastPosition);
                break;

            case MotionEvent.ACTION_MOVE:
                final float x = event.getX();
                final float y = event.getY();
                if (mLastDragged >= 0) {
                    final View v = getChildAt(mLastDragged);
                    final int l = getScrollX() + (int) x - v.getWidth() / 2;
                    final int t = getScrollY() + (int) y - v.getHeight() / 2;

                    Log.i(TAG, "Moved v getScrollX --> " + getScrollX() + " getScrollY --> " + getScrollY());

                    v.layout(l, t, l + v.getWidth(), t + v.getHeight());

                    if (mScrollState == SCROLL_STATE_IDLE) {
                        final int target = getTargetByXY((int) x, (int) y);
                        if (target != -1 && mLastTarget != target) {
                            animateGap(target);
                            mLastTarget = target;
                            Log.i(TAG, "Moved to mLastTarget=" + mLastTarget);
                        }
                    }
                }
                break;

            default:
                break;
        }

        return true;
    }

    /**
     * 交换动画
     */
    private void animateGap(int target) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (i == mLastDragged) {
                continue;
            }

            int newPos = i;
            if (mLastDragged < target && i >= mLastDragged + 1 && i <= target) {
                newPos--;
            } else if (target < mLastDragged && i >= target && i < mLastDragged) {
                newPos++;
            }

            int oldPos = i;
            if (newPositions.get(i) != -1) {
                oldPos = newPositions.get(i);
            }

            if (oldPos == newPos) {
                continue;
            }

            // animate
            Log.i(TAG, "animateGap from=" + oldPos + ", to=" + newPos);
            final Rect oldRect = getRectByPosition(oldPos);
            final Rect newRect = getRectByPosition(newPos);
            oldRect.offset(-v.getLeft(), -v.getTop());
            newRect.offset(-v.getLeft(), -v.getTop());

            TranslateAnimation translate = new TranslateAnimation(
                    oldRect.left, newRect.left,
                    oldRect.top, newRect.top);
            translate.setDuration(ANIMATION_DURATION);
            translate.setFillEnabled(true);
            translate.setFillAfter(true);
            v.clearAnimation();
            v.startAnimation(translate);

            newPositions.set(i, newPos);
        }
    }

    private int getTargetByXY(int x, int y) {
        final int position = getPositionByXY(x, y);
        if (position < 0) {
            return -1;
        }
        final Rect r = getRectByPosition(position);
        final int page = position / mPageSize;
        r.inset(r.width() / 4, r.height() / 4);
        r.offset(-getWidth() * page, 0);
        if (!r.contains(x, y)) {
            return -1;
        }
        return position;
    }

    /**
     * 根据触摸点计算坐标位置
     */
    private int getPositionByXY(int x, int y) {
        final int col = (x - mPaddingLeft) / (mGridWidth + mGridGap);
        final int row = (y - mPaddingTop) / (mGridHeight + mGridGap);
        if (x < mPaddingLeft || x >= (mPaddingLeft + col * (mGridWidth + mGridGap) + mGridWidth) ||
                y < mPaddingTop || y >= (mPaddingTop + row * (mGridHeight + mGridGap) + mGridHeight) ||
                col < 0 || col >= mColCount ||
                row < 0 || row >= mRowCount) {
            //触摸padding区域
            return -1;
        }
        final int position = mCurItem * mPageSize + row * mColCount + col;
        if (position < 0 || position >= getChildCount()) {
            // empty item
            return -1;
        }
        return position;
    }

    /**
     * 获取位置坐标
     */
    private Rect getRectByPosition(int position) {
        final int page = position / mPageSize; //页数
        final int col = (position % mPageSize) % mColCount; //列
        final int row = (position % mPageSize) / mColCount; //行
        final int left = getWidth() * page + mPaddingLeft + col * (mGridGap + mGridWidth); //左边距
        final int top = mPaddingTop + row * (mGridHeight + mGridGap); //上边距
        return new Rect(left, top, left + mGridWidth, top + mGridHeight);
    }


    /**
     * 设置适配器
     */
    public void setAdapter(Adapter adapter) {
        if (mAdapter != null) {
            removeAllViews();
            mCurItem = 0;
            scrollTo(0, 0);
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            for (int i = 0; i < mAdapter.getCount(); i++) {
                final View child = mAdapter.getView(i, null, this);
                addView(child);
            }
        }
    }
}
