package me.allenzjl.ptrdemo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * The type Ptr base.
 */
public abstract class PtrBase extends RelativeLayout {

    public static final float DEFAULT_RELEASE_FACTOR = 0.5f;

    PtrState mCurrentState = PtrState.HIDDEN;

    protected boolean mPendingAction = false;

    protected PtrListener mListener;

    protected float mReleaseFactor;

    protected WeakReference<View> mDependentViewRef;

    public PtrBase(Context context) {
        this(context, null);
    }

    public PtrBase(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PtrBase(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ptr_demo, defStyleAttr, 0);
        mReleaseFactor = a.getFloat(R.styleable.ptr_demo_releaseFactor, DEFAULT_RELEASE_FACTOR);
        a.recycle();

        if (mReleaseFactor <= 0 || mReleaseFactor > 1) {
            throw new IllegalArgumentException("The range of releaseFactor should between (0, 1]");
        }
    }

    public float getReleaseFactor() {
        return mReleaseFactor;
    }

    public void setReleaseFactor(float releaseFactor) {
        mReleaseFactor = releaseFactor;
    }

    public PtrListener getListener() {
        return mListener;
    }

    public void setPtrListener(PtrListener listener) {
        mListener = listener;
    }

    public PtrState getCurrentState() {
        return mCurrentState;
    }

    abstract int getActionOffset();

    public void performAction() {
        if (mCurrentState == PtrState.REFRESHING || mPendingAction) {
            return;
        }
        PtrBaseBehavior b = (PtrBaseBehavior) ((CoordinatorLayout.LayoutParams) getLayoutParams()).getBehavior();
        if (b != null) {
            CoordinatorLayout parent = Utils.findUpperCoordinatorLayout(this);
            if (parent == null) {
                throw new IllegalStateException("CoordinatorLayout not found.");
            }
            mPendingAction = true;
            b.animateOffsetTo(parent, this, findDependentView(parent, this), getActionOffset(), false);
        }
    }

    public void notifyActionFinish() {
        if (mCurrentState != PtrState.REFRESHING) {
            return;
        }
        PtrBaseBehavior b = (PtrBaseBehavior) ((CoordinatorLayout.LayoutParams) getLayoutParams()).getBehavior();
        if (b != null) {
            CoordinatorLayout parent = Utils.findUpperCoordinatorLayout(this);
            if (parent == null) {
                throw new IllegalStateException("CoordinatorLayout not found.");
            }
            mCurrentState = PtrState.DRAGGING;
            b.animateOffsetTo(parent, this, findDependentView(parent, this), b.getInitialOffset(parent, this), true);
        }
    }

    protected View findDependentView(CoordinatorLayout parent, PtrBase ptr) {
        if (mDependentViewRef == null || mDependentViewRef.get() == null) {
            mDependentViewRef = null;
            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View target = parent.getChildAt(i);
                List<View> dependencies = parent.getDependencies(target);
                if (dependencies.contains(ptr)) {
                    mDependentViewRef = new WeakReference<>(target);
                    break;
                }
            }
        }
        if (mDependentViewRef == null) {
            throw new IllegalStateException("No view depend on " + getClass().getSimpleName() + ".");
        }
        return mDependentViewRef.get();
    }

    protected static abstract class PtrBaseBehavior extends ViewOffsetBehavior<PtrBase> {

        protected boolean mFirstLayout = true;

        protected ValueAnimator mAnimator;

        public PtrBaseBehavior() {
            super();
        }

        public PtrBaseBehavior(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public boolean onLayoutChild(CoordinatorLayout parent, PtrBase child, int layoutDirection) {
            if (mFirstLayout) {
                setTopAndBottomOffset(getInitialOffset(parent, child));
                mFirstLayout = false;
            }
            return super.onLayoutChild(parent, child, layoutDirection);
        }

        int scroll(CoordinatorLayout parent, PtrBase child, int dy) {
            int newOffset = getTopAndBottomOffset() - dy;
            return setHeaderTopBottomOffset(parent, child, newOffset, getMinOffset(parent, child), getMaxOffset(parent, child));
        }

        int setHeaderTopBottomOffset(CoordinatorLayout parent, PtrBase child, int newOffset, int minOffset, int maxOffset) {
            final int curOffset = getTopAndBottomOffset();
            int consumed = 0;

            if (minOffset != 0 && curOffset >= minOffset && curOffset <= maxOffset) {
                newOffset = Utils.constrain(newOffset, minOffset, maxOffset);

                if (curOffset != newOffset) {
                    setTopAndBottomOffset(newOffset);
                    consumed = curOffset - newOffset;

                    onApplyNewOffset(parent, child, consumed, newOffset);
                }
            }

            return consumed;
        }

        void animateOffsetTo(final CoordinatorLayout parent, PtrBase child, View target, int offset, boolean remeasure) {
            if (mAnimator == null) {
                mAnimator = new ValueAnimator();
                mAnimator.setInterpolator(new DecelerateInterpolator());
                mAnimator.addUpdateListener(animation -> {
                    int animatedValue = (int) animation.getAnimatedValue();
                    setHeaderTopBottomOffset(parent, child, animatedValue, getMinOffset(parent, child),
                            getMaxOffset(parent, child));
                    if (remeasure || animatedValue == child.getActionOffset()) {
                        target.requestLayout();
                    }
                });
            } else {
                mAnimator.cancel();
                if (remeasure) {
                    target.requestLayout();
                }
            }

            mAnimator.setIntValues(getTopAndBottomOffset(), offset);
            mAnimator.start();
        }

        public abstract int getInitialOffset(CoordinatorLayout parent, PtrBase child);

        public abstract int getMinOffset(CoordinatorLayout parent, PtrBase child);

        public abstract int getMaxOffset(CoordinatorLayout parent, PtrBase child);

        abstract void onApplyNewOffset(CoordinatorLayout parent, PtrBase child, int consumed, int newOffset);
    }

}
