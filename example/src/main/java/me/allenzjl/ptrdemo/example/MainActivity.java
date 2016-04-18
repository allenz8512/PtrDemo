package me.allenzjl.ptrdemo.example;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.allenz.androidapplog.Logger;
import me.allenz.androidapplog.LoggerFactory;
import me.allenzjl.ptrdemo.PtrListener;
import me.allenzjl.ptrdemo.PtrState;
import me.allenzjl.ptrdemo.PullToRefresh;

public class MainActivity extends AppCompatActivity {

    private final Logger LOGGER = LoggerFactory.getLogger();

    @Bind(R.id.list)
    RecyclerView mList;

    @Bind(R.id.ptr)
    PullToRefresh mPtr;

    @Bind(R.id.text)
    TextView mText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Handler handler = new Handler(Looper.getMainLooper());
        mPtr.setPtrListener(new PtrListener() {

            @Override
            public void onPtrStateChanged(PtrState state) {
                LOGGER.debug("onPtrStateChanged:%s", state);
            }

            @Override
            public void onRefresh() {
                LOGGER.debug("onRefresh");
                handler.postDelayed(() -> mPtr.notifyActionFinish(), 2000);
            }

            @Override
            public void onPtrScroll(PtrState state, float positionOffset, int positionOffsetPixels) {
                LOGGER.debug("onPtrScroll:%s %f %d", state, positionOffset, positionOffsetPixels);
                if (state == PtrState.REFRESHING) {
                    mText.setText("正在刷新...");
                } else if (positionOffset > mPtr.getReleaseFactor()) {
                    mText.setText("释放立即刷新");
                } else {
                    mText.setText("下拉刷新");
                }
            }
        });
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.setAdapter(new Adapter());
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(new TextView(MainActivity.this));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mTextView.setText(String.valueOf(position + 1));
        }

        @Override
        public int getItemCount() {
            return 50;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        TextView mTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView;
        }
    }

}
