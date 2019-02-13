package me.licheng.crossviewpager2;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Adapter;
import android.widget.Scroller;

import java.util.ArrayList;

public class ViewPagerContainer extends ViewGroup {

    private final static String TAG = ViewPagerContainer.class.getSimpleName();

    private static final int DEFAULT_COL_COUNT = 2; //默认列
    private static final int DEFAULT_ROW_COUNT = 4; //默认行
    private static final int DEFAULT_GRID_GAP = 8; //默认格子间隙
    private static final long EDGE_HOLD_DURATION = 1200;
    private static final int MAX_SETTLE_DURATION = 600;
    private static final int MIN_FLING_VELOCITY = 400;
    private final int mMaximumVelocity;

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

    private static final int EDGE_LFET = 0;
    private static final int EDGE_RIGHT = 1;

    private boolean mIsBeingDragged;
    private int mTouchSlop;


    private int mCurItem; // Index of currently displayed page.


    private Adapter mAdapter;
    private float mLastMotionX;
    private float mLastMotionY;
    private int mLastPosition = -1;
    private int mLastDragged = -1;
    private final static int SCROLL_STATE_IDLE = 0;
    private static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;
    private int mScrollState = SCROLL_STATE_IDLE;
    private int mLastTarget;

    private ArrayList<Integer> newPositions = new ArrayList<Integer>();
    private static final long ANIMATION_DURATION = 150; // ms
    private static final int INVALID_POINTER = -1;
    private int mActivePointerId = INVALID_POINTER;

    private float mInitialMotionX;
    private float mInitialMotionY;

    private int mCloseEnough;

    private static final int CLOSE_ENOUGH = 2; // dp


    private Scroller mScroller;

    private long mLastDownTime = Long.MAX_VALUE;
    private static final long LONG_CLICK_DURATION = 1000; // ms
    private int mLastEdge = -1;
    private long mLastEdgeTime = Long.MAX_VALUE;
    private int mFlingDistance;
    private int mMinimumVelocity;

    private VelocityTracker mVelocityTracker;



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

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);

        mScroller = new Scroller(context, sInterpolator);

        mCloseEnough = (int) (CLOSE_ENOUGH * density);

        mMinimumVelocity = (int) (MIN_FLING_VELOCITY * density);
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();


    }

    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

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

        if (mCurItem > 0 && mCurItem < mPageCount) {
            final int curItem = mCurItem;
            mCurItem = 0;
            setCurrentItem(curItem);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the drag.
            Log.i(TAG, "Intercept done!");
            mIsBeingDragged = false;
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN) {
            if (mIsBeingDragged || mLastDragged >= 0) {
                Log.i(TAG, "Intercept returning true!");
                return true;
            }
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    Log.i(TAG, "activePointerId is invalid");
                    break;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
                final float x = MotionEventCompat.getX(ev, pointerIndex);
                final float dx = x - mLastMotionX;
                final float xDiff = Math.abs(dx);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = Math.abs(y - mInitialMotionY);
                Log.i(TAG, "***Moved to " + x + "," + y + " diff=" + xDiff + "," + yDiff);

                if (xDiff > mTouchSlop && xDiff * 0.5f > yDiff) {
                    Log.i(TAG, "***Starting drag!");
                    mIsBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                    mLastMotionX = dx > 0 ? mInitialMotionX + mTouchSlop :
                            mInitialMotionX - mTouchSlop;
                    mLastMotionY = y;
                    setScrollingCacheEnabled(true);
                }
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    if (performDrag(x)) {
                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                /*
                 * Remember location of down touch. ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);

                mScroller.computeScrollOffset();
                if (mScrollState == SCROLL_STATE_SETTLING &&
                        Math.abs(mScroller.getFinalX() - mScroller.getCurrX()) > mCloseEnough) {
                    // Let the user 'catch' the pager as it animates.
                    mScroller.abortAnimation();
                    mIsBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                } else {
                    completeScroll(false);
                    mIsBeingDragged = false;
                }

                Log.i(TAG, "***Down at " + mLastMotionX + "," + mLastMotionY
                        + " mIsBeingDragged=" + mIsBeingDragged);
                mLastDragged = -1;
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                break;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        return mIsBeingDragged;
    }

    @Override
    public void computeScroll() {
        if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();

            if (oldX != x || oldY != y) {
                scrollTo(x, y);
                if (!pageScrolled(x)) {
                    mScroller.abortAnimation();
                    scrollTo(0, y);
                }
            }

            // Keep on drawing until the animation has finished.
            ViewCompat.postInvalidateOnAnimation(this);
            return;
        }

        // Done with scroll, clean up state.
        completeScroll(true);
    }

    private void completeScroll(boolean postEvents) {
        if (mScrollState == SCROLL_STATE_SETTLING) {
            // Done with scroll, no longer want to cache view drawing.
            setScrollingCacheEnabled(false);
            mScroller.abortAnimation();
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            if (oldX != x || oldY != y) {
                scrollTo(x, y);
            }
            if (postEvents) {
                ViewCompat.postOnAnimation(this, mEndScrollRunnable);
            } else {
                mEndScrollRunnable.run();
            }
        }
    }

    private final Runnable mEndScrollRunnable = new Runnable() {
        public void run() {
            setScrollState(SCROLL_STATE_IDLE);
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
            return false;
        }

        if (mPageCount <= 0) {
            return false;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        final int action = event.getAction();

        boolean needsInvalidate = false;


        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = event.getX();
                mLastMotionY = event.getY();


                mLastPosition = getPositionByXY((int) mLastMotionX, (int) mLastMotionY);

                Log.i(TAG, "touch dowan at lastPosition --> " + mLastPosition);

                if (mLastPosition >= 0) {
                    mLastDownTime = System.currentTimeMillis();
                } else {
                    mLastDownTime = Long.MAX_VALUE;
                }

                break;

            case MotionEvent.ACTION_MOVE:
                final float x = event.getX();
                final float y = event.getY();

                Log.i(TAG, "onTouchEvent move mLastDragged --> " + mLastDragged + " mScrollState --> " + mScrollState);
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
                        final int edge = getEdgeByXY((int) x, (int) y);
                        if (mLastEdge == -1) {
                            if (edge != mLastEdge) {
                                mLastEdge = edge;
                                mLastEdgeTime = System.currentTimeMillis();
                            }
                        } else {
                            if (edge != mLastEdge) {
                                mLastEdge = -1;
                            } else {
                                if ((System.currentTimeMillis() - mLastEdgeTime) >= EDGE_HOLD_DURATION) {
                                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                    triggerSwipe(edge);
                                    mLastEdge = -1;
                                }
                            }
                        }

                    }
                } else if (!mIsBeingDragged) {
                    final float xDiff = Math.abs(x - mLastMotionX);
                    final float yDiff = Math.abs(y - mLastMotionY);
                    Log.i(TAG, "Moved to " + x + "," + y + " diff=" + xDiff + "," + yDiff);

                    if (xDiff > mTouchSlop && xDiff * 0.5f > yDiff) {
                        Log.i(TAG, "***Starting drag!");
                        mIsBeingDragged = true;
                        requestParentDisallowInterceptTouchEvent(true);
                        mLastMotionX = x - mInitialMotionX > 0 ? mInitialMotionX + mTouchSlop :
                                mInitialMotionX - mTouchSlop;
                        mLastMotionY = y;
                        setScrollState(SCROLL_STATE_DRAGGING);
                        setScrollingCacheEnabled(true);
                    }
                }

                if (mIsBeingDragged) {
                    needsInvalidate |= performDrag(x);
                } else if (mLastPosition >= 0) {
                    final int currentPosition = getPositionByXY((int) x, (int) y);
                    Log.i(TAG, "Moved to currentPosition=" + currentPosition);
                    if (currentPosition == mLastPosition) {
                        if ((System.currentTimeMillis() - mLastDownTime) >= LONG_CLICK_DURATION) {
                            if (onItemLongClick(currentPosition)) {
                                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                mLastDragged = mLastPosition;
                                requestParentDisallowInterceptTouchEvent(true);
                                mLastTarget = -1;
                                animateDragged();
                                mLastPosition = -1;
                            }
                            mLastDownTime = Long.MAX_VALUE;
                        }
                    } else {
                        mLastPosition = -1;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                Log.i(TAG, "Touch up!!!");
                final int pointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);
                final float xx = MotionEventCompat.getX(event, pointerIndex);
                final float yy = MotionEventCompat.getY(event, pointerIndex);

                if (mLastDragged >= 0) {
                    rearrange();
                } else if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) VelocityTrackerCompat.getXVelocity(velocityTracker, mActivePointerId);

                    final int width = getWidth();
                    final int scrollX = getScrollX();
                    final int currentPage = scrollX / width;
                    final int offsetPixels = scrollX - currentPage * width;
                    final float pageOffset = (float) offsetPixels / (float) width;
                    final int totalDelta = (int) (xx - mInitialMotionX);

                    int nextPage = determineTargetPage(currentPage, pageOffset, initialVelocity, totalDelta);
                    setCurrentItemInternal(nextPage, true, true, initialVelocity);

                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                } else if (mLastPosition >= 0) {
                    final int currentPosition = getPositionByXY((int) xx, (int) yy);
                    Log.i(TAG, "Touch up!!! currentPosition=" + currentPosition);
                    if (currentPosition == mLastPosition) {

                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                Log.i(TAG, "Touch cancel!!!");
                if (mLastDragged >= 0) {
                    rearrange();
                } else if (mIsBeingDragged) {
                    scrollToItem(mCurItem, true, 0, false);
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;


            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(event);
                final float xxx = MotionEventCompat.getX(event, index);
                mLastMotionX = xxx;
                mActivePointerId = MotionEventCompat.getPointerId(event, index);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                mLastMotionX = MotionEventCompat.getX(event,
                        MotionEventCompat.findPointerIndex(event, mActivePointerId));
                break;
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
        return true;
    }

    private int determineTargetPage(int currentPage, float pageOffset, int velocity, int deltaX) {
        int targetPage;
        if (Math.abs(deltaX) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
            targetPage = velocity > 0 ? currentPage : currentPage + 1;
        } else {
            final float truncator = currentPage >= mCurItem ? 0.4f : 0.6f;
            targetPage = (int) (currentPage + pageOffset + truncator);
        }
        return targetPage;
    }

    /**
     * 翻页
     */
    private void triggerSwipe(int edge) {
        if (edge == EDGE_LFET && mCurItem > 0) {
            setCurrentItem(mCurItem - 1, true);
        } else if (edge == EDGE_RIGHT && mCurItem < mPageCount - 1) {
            setCurrentItem(mCurItem + 1, true);
        }
    }

    public void setCurrentItem(int item) {
        setCurrentItemInternal(item, false, false);
    }

    private void setCurrentItem(int item, boolean smoothScroll) {
        setCurrentItemInternal(item, smoothScroll, false);
    }

    private void setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
        setCurrentItemInternal(item, smoothScroll, always, 0);
    }

    void setCurrentItemInternal(int item, boolean smoothScroll, boolean always, int velocity) {
        if (mPageCount <= 0) {
            setScrollingCacheEnabled(false);
            return;
        }
        if (!always && mCurItem == item) {
            setScrollingCacheEnabled(false);
            return;
        }

        if (item < 0) {
            item = 0;
        } else if (item >= mPageCount) {
            item = mPageCount - 1;
        }
        final boolean dispatchSelected = mCurItem != item;
        mCurItem = item;
        scrollToItem(item, smoothScroll, velocity, dispatchSelected);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = MotionEventCompat.getX(ev, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    private void scrollToItem(int item, boolean smoothScroll, int velocity, boolean dispatchSelected) {
        final int destX = getWidth() * item;
        if (smoothScroll) {
            smoothScrollTo(destX, 0, velocity);
        } else {
            completeScroll(false);
            scrollTo(destX, 0);
            pageScrolled(destX);
        }
    }

    private void smoothScrollTo(int x, int y, int velocity) {
        if (getChildCount() == 0) {
            // Nothing to do.
            setScrollingCacheEnabled(false);
            return;
        }
        int sx = getScrollX();
        int sy = getScrollY();
        int dx = x - sx;
        int dy = y - sy;
        if (dx == 0 && dy == 0) {
            completeScroll(false);
            setScrollState(SCROLL_STATE_IDLE);
            return;
        }
        setScrollingCacheEnabled(true);
        setScrollState(SCROLL_STATE_SETTLING);

        final int width = getWidth();
        final int halfWidth = width / 2;
        final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
        final float distance = halfWidth + halfWidth *
                distanceInfluenceForSnapDuration(distanceRatio);

        int duration = 0;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
        } else {
            final float pageDelta = (float) Math.abs(dx) / width;
            duration = (int) ((pageDelta + 1) * 100);
        }
        duration = Math.min(duration, MAX_SETTLE_DURATION);

        mScroller.startScroll(sx, sy, dx, dy, duration);
        ViewCompat.postInvalidateOnAnimation(this);
        
    }

    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    private void endDrag() {
        mIsBeingDragged = false;

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void rearrange() {
        if (mLastDragged >= 0) {
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).clearAnimation();
            }
            if (mLastTarget >= 0 && mLastDragged != mLastTarget) {
                final View child = getChildAt(mLastDragged);
                removeViewAt(mLastDragged);
                addView(child, mLastTarget);
            }
            mLastDragged = -1;
            mLastTarget = -1;
            requestLayout();
            invalidate();
        }
    }

    private void animateDragged() {
        if (mLastDragged >= 0) {
            final View v = getChildAt(mLastDragged);

            final Rect r = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
            r.inset(-r.width() / 20, -r.height() / 20);
            v.measure(MeasureSpec.makeMeasureSpec(r.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(r.height(), MeasureSpec.EXACTLY));
            v.layout(r.left, r.top, r.right, r.bottom);

            AnimationSet animSet = new AnimationSet(true);
            ScaleAnimation scale = new ScaleAnimation(0.9091f, 1, 0.9091f, 1, v.getWidth() / 2, v.getHeight() / 2);
            scale.setDuration(ANIMATION_DURATION);
            AlphaAnimation alpha = new AlphaAnimation(1, .5f);
            alpha.setDuration(ANIMATION_DURATION);

            animSet.addAnimation(scale);
            animSet.addAnimation(alpha);
            animSet.setFillEnabled(true);
            animSet.setFillAfter(true);

            v.clearAnimation();
            v.startAnimation(animSet);
        }
    }

    private boolean onItemLongClick(int currentPosition) {

        return true;
    }

    /**
     * 滑动
     */
    private boolean performDrag(float x) {
        boolean needsInvalidate = false;

        final float deltaX = mLastMotionX - x;
        mLastMotionX = x;

        float oldScrollX = getScrollX();
        float scrollX = oldScrollX + deltaX;
        final int width = getWidth();

        float leftBound = 0;
        float rightBound = width * (mPageCount - 1);

        if (scrollX < leftBound) {
            final float over = Math.min(leftBound - scrollX, mMaxOverScrollSize);
            scrollX = leftBound - over;
        } else if (scrollX > rightBound) {
            final float over = Math.min(scrollX - rightBound, mMaxOverScrollSize);
            scrollX = rightBound + over;
        }
        // Don't lose the rounded component
        mLastMotionX += scrollX - (int) scrollX;
        scrollTo((int) scrollX, getScrollY());
        pageScrolled((int) scrollX);

        return needsInvalidate;
    }

    private boolean pageScrolled(int scrollX) {
        return true;
    }

    private void setScrollingCacheEnabled(boolean b) {
    }

    /**
     * 设置滑动状态
     */
    private void setScrollState(int newState) {
        if (mScrollState == newState) {
            return;
        }
        mScrollState = newState;
    }

    /**
     * 通知父控件拦截事件 true: 通知父控件不要拦截
     */
    private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    /**
     * 获取左右边界
     */
    private int getEdgeByXY(int x, int y) {
        if (x < mEdgeSize) {
            return EDGE_LFET;
        } else if (x >= (getWidth() - mEdgeSize)) {
            return EDGE_RIGHT;
        }
        return -1;
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
