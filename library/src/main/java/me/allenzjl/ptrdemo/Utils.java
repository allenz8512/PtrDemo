package me.allenzjl.ptrdemo;

import android.support.design.widget.CoordinatorLayout;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.util.Stack;

public class Utils {

    public static int constrain(int amount, int low, int high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    public static float constrain(float amount, float low, float high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> T findViewByType(ViewGroup parent, Class<T> type) {
        Stack<ViewGroup> stack = new Stack<>();
        stack.push(parent);
        while (!stack.isEmpty()) {
            ViewGroup vg = stack.pop();
            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = vg.getChildAt(i);
                if (type.isAssignableFrom(v.getClass())) {
                    return (T) v;
                } else if (v instanceof ViewGroup) {
                    stack.push((ViewGroup) v);
                }
            }
        }
        return null;
    }

    public static <T extends CoordinatorLayout.Behavior> View findViewByBehavior(CoordinatorLayout parent,
                                                                                 Class<T> behaviorType) {
        Stack<ViewGroup> stack = new Stack<>();
        stack.push(parent);
        while (!stack.isEmpty()) {
            ViewGroup vg = stack.pop();
            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = vg.getChildAt(i);
                CoordinatorLayout.Behavior b = ((CoordinatorLayout.LayoutParams) v.getLayoutParams()).getBehavior();
                if (b != null && behaviorType.isAssignableFrom(b.getClass())) {
                    return v;
                } else if (v instanceof ViewGroup) {
                    stack.push((ViewGroup) v);
                }
            }
        }
        return null;
    }

    public static CoordinatorLayout findUpperCoordinatorLayout(View child) {
        ViewParent p = child.getParent();
        while (p != null) {
            if (p instanceof CoordinatorLayout) {
                return (CoordinatorLayout) p;
            }
            p = p.getParent();
        }
        return null;
    }
}
