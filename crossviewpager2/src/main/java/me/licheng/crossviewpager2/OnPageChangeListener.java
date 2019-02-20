package me.licheng.crossviewpager2;

/**
 * Created by changyuan on 2019/2/20.
 */
public interface OnPageChangeListener {
    void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);

    void onPageSelected(int position);

    /**
     * @param state The new scroll state.
     * @see ViewPagerContainer#SCROLL_STATE_IDLE
     * @see ViewPagerContainer#SCROLL_STATE_DRAGGING
     * @see ViewPagerContainer#SCROLL_STATE_SETTLING
     */
    void onPageScrollStateChanged(int state);
}
