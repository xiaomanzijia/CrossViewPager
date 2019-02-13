package me.licheng.crossviewpager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import me.licheng.crossviewpager2.ViewPagerContainer;

public class MainActivity extends AppCompatActivity {

    final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPagerContainer viewpager = findViewById(R.id.viewpager);
        viewpager.setOnPageChangeListener(new ViewPagerContainer.OnPageChangeListener() {
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
                return false;
            }
        });
    }
}
