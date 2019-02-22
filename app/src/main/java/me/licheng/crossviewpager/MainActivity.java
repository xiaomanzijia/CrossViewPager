package me.licheng.crossviewpager;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import me.licheng.crossviewpager2.OnPageChangeListener;
import me.licheng.viewpagerindicator.ILinePageIndicator;
import me.licheng.viewpagerindicator.IViewPager;

public class MainActivity extends AppCompatActivity {

    final static String TAG = MainActivity.class.getSimpleName();

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ILinePageIndicator indicator = findViewById(R.id.indicator);


        IViewPager viewpager = findViewById(R.id.viewpager);

        viewpager.setRowCount(6);
        viewpager.setColCount(4);

        adapter = new ArrayAdapter<String>(this, 0) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                final String text = getItem(position);
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.draggable_grid_item, null);
                }
                ((TextView) convertView).setText(text);
                return convertView;
            }

            ;
        };

        for (int i = 0; i < 55; i++) {
            adapter.add("Grid " + i);
        }
        viewpager.setAdapter(adapter);

        viewpager.addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                Log.i(TAG, String.format("onPageScrolled-->position=%d, positionOffset=%f, positionOffsetPixels=%d", position, positionOffset, positionOffsetPixels));
            }

            @Override
            public void onPageSelected(int position) {

                Log.i(TAG, String.format("onPageSelected-->position=%d", position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

                Log.i(TAG, String.format("onPageScrollStateChanged-->state=%d", state));
            }
        });
        viewpager.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Log.i(TAG, String.format("onItemLongClick-->position=%d", position));
                return true;
            }
        });

        indicator.setViewPager(viewpager);
    }
}
