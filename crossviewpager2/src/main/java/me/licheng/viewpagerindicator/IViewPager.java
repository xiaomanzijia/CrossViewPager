package me.licheng.viewpagerindicator;

import android.widget.Adapter;
import android.widget.AdapterView;

import me.licheng.crossviewpager2.OnPageChangeListener;

/**
 * Created by changyuan on 2019/2/20.
 */
public interface IViewPager {

    void setAdapter(Adapter adapter);

    Adapter getAdapter();

    void addOnPageChangeListener(OnPageChangeListener listener);

    void setCurrentItem(int item);

    void setOnItemLongClickListener(AdapterView.OnItemLongClickListener onItemLongClickListener);

    void setRowCount(int rowCount);

    void setColCount(int colCount);

    int getColCount();

    int getRowCount();
}
