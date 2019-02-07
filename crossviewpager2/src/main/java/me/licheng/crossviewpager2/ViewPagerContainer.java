package me.licheng.crossviewpager2;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

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
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final Rect rect = getRectByPosition(i);
            child.measure(MeasureSpec.makeMeasureSpec(rect.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(rect.height(), MeasureSpec.EXACTLY));
            Log.i(TAG, "child onLayout-->  position: " + i + ", rect: " + rect);
            child.layout(rect.left, rect.top, rect.right, rect.bottom);
        }
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
