package me.allenzjl.ptrdemo;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

@CoordinatorLayout.DefaultBehavior(PullToRefresh.Behavior.class)
public class PullToRefresh extends PtrBase {

    public PullToRefresh(Context context) {
        this(context, null);
    }

    public PullToRefresh(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullToRefresh(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    int getActionOffset() {
        int height = getHeight();
        return (int) (mReleaseFactor * height - height);
    }

    public static class Behavior extends PtrBase.PtrBaseBehavior {

        public Behavior() {
            super();
        }

        public Behavior(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public int getInitialOffset(CoordinatorLayout parent, PtrBase child) {
            return -child.getMeasuredHeight();
        }

        @Override
        public int getMinOffset(CoordinatorLayout parent, PtrBase child) {
            return -child.getHeight();
        }

        @Override
        public int getMaxOffset(CoordinatorLayout parent, PtrBase child) {
            return 0;
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        public void onApplyNewOffset(CoordinatorLayout parent, PtrBase child, int consumed, int newOffset) {
            PtrListener listener = child.mListener;
            if (child.mCurrentState == PtrState.REFRESHING) {
                listener.onRefresh();
            }
            int showReleaseOffset = child.getActionOffset();
            PtrState oldState = child.mCurrentState;
            PtrState newState = oldState;
            if (consumed < 0) { // Header展开
                if (child.mCurrentState == PtrState.HIDDEN) {
                    newState = child.mCurrentState = PtrState.DRAGGING;
                }
            } else { // Header折叠
                if (newOffset == getMinOffset(parent, child)) {
                    newState = child.mCurrentState = PtrState.HIDDEN;
                }
            }
            if (child.mCurrentState != PtrState.REFRESHING && child.mPendingAction && newOffset == showReleaseOffset) {
                newState = child.mCurrentState = PtrState.REFRESHING;
                child.mPendingAction = false;
                listener.onRefresh();
            }
            listener.onPtrScroll(child.mCurrentState, getOffsetPercent(child, newOffset), newOffset);
            if (oldState != newState) {
                listener.onPtrStateChanged(newState);
            }
        }

        float getOffsetPercent(PtrBase ptr, int newOffset) {
            int height = ptr.getHeight();
            return (newOffset + height) / (float) height;
        }

        @Override
        public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, PtrBase child, View directTargetChild,
                                           View target, int nestedScrollAxes) {
            boolean started = (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0 && child.mListener != null &&
                    child.mCurrentState != PtrState.REFRESHING;
            if (started && mAnimator != null) {
                mAnimator.cancel();
            }
            return started;
        }

        @Override
        public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, PtrBase child, View target, int dx, int dy,
                                      int[] consumed) {
            // 在RecyclerView中，dx和dy的值为按下时的坐标减去滑动后的坐标，所以dy的值在向上滑动时为正，向下滑动时为负
            if (dy != 0) {
                // dy为正，表示手势是向上滑动，也就是说列表是向下滚动的
                if (dy > 0) {
                    // 设定最大和最小的Header偏移，因为是往上推（折叠Header）
                    // ，所以Header的最大偏移为0（完全展开），最小偏移为负高度（完全折叠）
                    // 在列表滚动前，Header先往上偏移，并且告诉列表Header相对之前的位置偏移的距离（消耗了多少滚动距离）
                    int consumedY = scroll(coordinatorLayout, child, dy);
                    consumed[1] = consumedY;
                }
            }
        }

        @Override
        public void onNestedScroll(CoordinatorLayout coordinatorLayout, PtrBase child, View target, int dxConsumed,
                                   int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
            // dyUnconsumed的值为dy减去dyConsumed，dyUnconsumed的值为负表示已到达列表顶部，不能再滚动
            if (dyUnconsumed != 0) {
                if (dyUnconsumed < 0) {
                    // 剩下的距离用于Header偏移，因为dyUnconsumed为负，所以Header往下偏移
                    scroll(coordinatorLayout, child, dyUnconsumed);
                }
            }
        }

        @Override
        public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, PtrBase child, View target) {
            int curOffset = getTopAndBottomOffset();
            int showReleaseOffset = child.getActionOffset();
            int newOffset;
            if (curOffset >= showReleaseOffset) {
                newOffset = showReleaseOffset;
                child.mPendingAction = true;
            } else {
                newOffset = getInitialOffset(coordinatorLayout, child);
            }
            animateOffsetTo(coordinatorLayout, child, target, newOffset, false);
        }


    }
}
