package me.allenzjl.ptrdemo;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * The type Pull to refresh scroll behavior.
 */
public class PullToRefreshScrollBehavior extends ViewOffsetBehavior<View> {

    private WeakReference<PullToRefresh> mPtrRef;

    public PullToRefreshScrollBehavior() {
    }

    public PullToRefreshScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private PullToRefresh getPullToRefresh(CoordinatorLayout parent, View child) {
        if (mPtrRef == null || mPtrRef.get() == null) {
            mPtrRef = null;
            List<View> dependencies = parent.getDependencies(child);
            for (View view : dependencies) {
                if (view instanceof PullToRefresh) {
                    mPtrRef = new WeakReference<>((PullToRefresh) view);
                    break;
                }
            }
            return null;
        }
        return mPtrRef.get();
    }

    @Override
    public boolean onMeasureChild(CoordinatorLayout parent, View child, int parentWidthMeasureSpec, int widthUsed,
                                  int parentHeightMeasureSpec, int heightUsed) {
        final int childLpHeight = child.getLayoutParams().height;
        if (childLpHeight != ViewGroup.LayoutParams.MATCH_PARENT) {
            throw new IllegalArgumentException("Layout using this behavior should set its height to MATCH_PARENT.");
        }
        PullToRefresh ptr = getPullToRefresh(parent, child);
        if (ptr == null) {
            return false;
        }
        int parentHeight = View.MeasureSpec.getSize(parentHeightMeasureSpec);
        if (parentHeight == 0) {
            parentHeight = parent.getHeight();
        }
        if (ViewCompat.isLaidOut(ptr) && ptr.getCurrentState() == PtrState.REFRESHING) {
            PullToRefresh.Behavior b =
                    (PullToRefresh.Behavior) ((CoordinatorLayout.LayoutParams) ptr.getLayoutParams()).getBehavior();
            if (b != null) {
                int curOffset = b.getTopAndBottomOffset();
                int curVisibleHeight = ptr.getMeasuredHeight() + curOffset;
                int height = parentHeight - curVisibleHeight;
                int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
                parent.onMeasureChild(child, parentWidthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof PullToRefresh;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        super.onLayoutChild(parent, child, layoutDirection);

        final List<View> dependencies = parent.getDependencies(child);
        for (int i = 0, z = dependencies.size(); i < z; i++) {
            updateOffset(parent, child, dependencies.get(i));
        }
        return true;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        updateOffset(parent, child, dependency);
        return true;
    }

    private boolean updateOffset(CoordinatorLayout parent, View child, View dependency) {
        final CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) dependency.getLayoutParams()).getBehavior();
        if (behavior instanceof PullToRefresh.Behavior) {
            final int offset = ((PullToRefresh.Behavior) behavior).getTopAndBottomOffset();
            setTopAndBottomOffset(dependency.getHeight() + offset);
            return true;
        }
        return false;
    }

}
