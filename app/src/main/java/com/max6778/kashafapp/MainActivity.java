package com.max6778.kashafapp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.util.Linkify;
import android.util.LruCache;
import android.util.Patterns;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_CHAT_IMAGE = 7104;
    private static final int PICK_ANNOUNCEMENT_IMAGE = 7105;
    private static final int MAX_MEDIA_BYTES = 700 * 1024;
    private static final int MAX_SOURCE_BYTES = 12 * 1024 * 1024;
    private static final int MAX_MESSAGE = 1000;
    private static final int MEMBER_CLASS_COUNT = 5;
    private static final long RESET_COOLDOWN_MS = 60000L;
    private static final Locale AR = new Locale("ar", "MA");

    // ---- Theme colors (filled by applyTheme) ----
    private boolean dark;
    private int bg, surface, ink, muted, line, brand, green, heroBg, heroSub, danger;

    private final String branchArabic = "فرع السلام", organizationArabic = "الكشفية الحسنية المغربية";
    private final String[] classIds = {"cubs", "wolf_cubs_brownies", "scouts_guides", "senior_scouts_guides", "rovers_rangers", "admins"};
    private final String[] classArabic = {"البراعم", "الأشبال والزهرات", "الكشافة والمرشدات", "الكشاف المتقدم والرائدات", "الجوالة والدليلات", "الإدارة"};
    private final String[] classAges = {"من 5 إلى 7 سنوات", "من 7 إلى 12 سنة", "من 12 إلى 15 سنة", "من 15 إلى 18 سنة", "من 18 إلى 23 سنة", "قادة ومشرفو الفرع"};
    private final String[] classTaglines = {"اكتشاف ومتعة", "معًا نتعلّم وننمو", "خدمة ومهارة", "قيادة ومبادرة", "مسؤولية وخدمة المجتمع", "تنظيم وخدمة الفرع"};
    private final int[] classColors = {Color.rgb(211, 166, 55), Color.rgb(52, 112, 190), Color.rgb(45, 137, 86), Color.rgb(202, 61, 68), Color.rgb(137, 119, 77), Color.rgb(20, 38, 61)};
    private final int[] classLogos = {R.drawable.class_cubs, R.drawable.class_wolf_cubs, R.drawable.class_scouts_guides, R.drawable.class_senior, R.drawable.class_rovers, R.mipmap.ic_launcher};
    private final String[] genderLabels = {"أنثى", "ذكر"};
    private final String[] genderValues = {"female", "male"};

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;
    private int accent, selectedClass = -1;
    private String scoutClassId = "", user = "", emailAddress = "", gender = "", rank = "member", page = "Home";
    private boolean legacyClassSetup = false, isAdmin = false, authSignupVisible = false;
    private String approvalStatus = "approved", rejectionReason = "", appealMessage = "";
    private byte[] announcementImage;
    private TextView announcementImageStatus;
    private ImageView announcementThumb;
    private String currentRoomId = "global", currentRoomTitle = "الصالة العامة";
    private ListenerRegistration chatListener, announcementListener, newsWatcher;
    private final ArrayList<ListenerRegistration> previewListeners = new ArrayList<>();
    private LinearLayout root, content, roomMessages, latestNewsHolder;
    private EditText messageInput;

    // ---- UI state ----
    private final Handler ui = new Handler(Looper.getMainLooper());
    private Toast fallbackToast;
    private View snackView;
    private Runnable snackHide;
    private int insetTop;
    private long lastBackAt;
    private int screenToken;
    private boolean authBusy = false, resetSending = false;
    private Runnable authIdle;
    private DocumentSnapshot latestNews;
    private int newsUnread;
    private long newsSeenBefore;
    private View newsDot;
    private ScrollView roomScroll;
    private TextView newMessagePill;
    private boolean roomFirstLoad = true;
    private final HashSet<String> knownMessageIds = new HashSet<>();
    private final LruCache<String, Bitmap> bitmapCache = new LruCache<String, Bitmap>(24 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };

    // =====================================================================
    // Lifecycle
    // =====================================================================
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("kashaf_prefs", MODE_PRIVATE);
        auth = FirebaseAuth.getInstance();
        try { auth.setLanguageCode("ar"); } catch (Exception ignored) { }
        db = FirebaseFirestore.getInstance();
        applyTheme();
        accent = defaultAccent();
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (auth.getCurrentUser() != null) { showSplash(); loadProfile(); } else showAuth(false);
    }

    @Override protected void onDestroy() { ui.removeCallbacksAndMessages(null); imageExecutor.shutdownNow(); stopAllListeners(); super.onDestroy(); }

    @Override public void onBackPressed() {
        if (authSignupVisible) { backToSignIn(); return; }
        if (!isAdmin && ("pending".equals(approvalStatus) || "rejected".equals(approvalStatus))) { super.onBackPressed(); return; }
        boolean signedInUser = auth.getCurrentUser() != null && !user.isEmpty();
        if (root != null && page.equals("Room")) { page = "Chat"; showApp(); }
        else if (root != null && !page.equals("Home") && signedInUser) { page = "Home"; showApp(); }
        else if (root != null && signedInUser) {
            long now = System.currentTimeMillis();
            if (now - lastBackAt < 2200) super.onBackPressed();
            else { lastBackAt = now; toast("اضغط رجوع مرة أخرى للخروج"); }
        } else super.onBackPressed();
    }

    // =====================================================================
    // Theme
    // =====================================================================
    private void applyTheme() {
        String mode = prefs.getString("theme", "system");
        if ("dark".equals(mode)) dark = true;
        else if ("light".equals(mode)) dark = false;
        else dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (dark) {
            bg = Color.rgb(11, 16, 22); surface = Color.rgb(21, 28, 37); ink = Color.rgb(236, 241, 246); muted = Color.rgb(146, 160, 175);
            line = Color.rgb(38, 49, 61); brand = Color.rgb(186, 211, 240); green = Color.rgb(112, 201, 150);
            heroBg = Color.rgb(24, 44, 70); heroSub = Color.rgb(196, 208, 222); danger = Color.rgb(240, 120, 120);
        } else {
            bg = Color.rgb(244, 246, 244); surface = Color.WHITE; ink = Color.rgb(24, 40, 60); muted = Color.rgb(98, 110, 125);
            line = Color.rgb(226, 232, 228); brand = Color.rgb(20, 38, 61); green = Color.rgb(34, 102, 69);
            heroBg = Color.rgb(20, 38, 61); heroSub = Color.rgb(212, 222, 234); danger = Color.rgb(190, 45, 50);
        }
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setBackgroundColor(bg);
        int flags = dark ? 0 : (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private int defaultAccent() { return dark ? Color.rgb(104, 156, 222) : Color.rgb(20, 38, 61); }
    private int blend(int c, int base, float a) {
        return Color.rgb((int) (Color.red(c) * a + Color.red(base) * (1 - a)), (int) (Color.green(c) * a + Color.green(base) * (1 - a)), (int) (Color.blue(c) * a + Color.blue(base) * (1 - a)));
    }
    private int tint(int color, float f) { return blend(color, surface, dark ? Math.min(.38f, f * 2.2f) : f); }
    private int accentText(int c) { return dark ? blend(c, Color.WHITE, .72f) : blend(c, Color.BLACK, .78f); }
    private int onColor(int c) {
        double lum = (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0;
        return lum > 0.62 ? Color.rgb(26, 26, 26) : Color.WHITE;
    }
    /** In dark mode very dark accents (the navy admin color) are lightened so buttons stay visible. */
    private int vivid(int c) {
        if (!dark) return c;
        double lum = (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0;
        return lum >= 0.3 ? c : blend(Color.WHITE, c, 0.3f);
    }
    private int solid() { return vivid(accent); }

    // =====================================================================
    // View helpers
    // =====================================================================
    private int dp(float n) { return (int) (n * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private GradientDrawable bordered(int color, int radius, int stroke) { return bordered(color, radius, stroke, 1); }
    private GradientDrawable bordered(int color, int radius, int stroke, int width) { GradientDrawable d = shape(color, radius); d.setStroke(dp(width), stroke); return d; }
    private GradientDrawable gradient(int from, int to, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{from, to}); d.setCornerRadius(dp(radius)); return d;
    }
    private Drawable ripple(Drawable d) { return new RippleDrawable(ColorStateList.valueOf(Color.argb(dark ? 50 : 40, 128, 128, 128)), d, null); }
    /** Ripple drawn on top of a view (so the view itself can clip its children to rounded corners). */
    private void rippleOver(View v, int radius) {
        v.setForeground(new RippleDrawable(ColorStateList.valueOf(Color.argb(dark ? 50 : 40, 128, 128, 128)), null, shape(Color.WHITE, radius)));
    }
    private void elev(View v) { if (!dark) v.setElevation(dp(2)); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setLineSpacing(0, 1.12f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private TextView oneLine(TextView t) { t.setSingleLine(true); t.setEllipsize(TextUtils.TruncateAt.END); return t; }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void pad(View v, int l, int t, int r, int b) { v.setPadding(dp(l), dp(t), dp(r), dp(b)); }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w > 0 ? dp(w) : w, h > 0 ? dp(h) : h); }
    private LinearLayout.LayoutParams weighted(int startMargin) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f); p.setMarginStart(dp(startMargin)); return p; }
    private void top(LinearLayout parent, View child, int margin) { LinearLayout.LayoutParams p = lp(-1, -2); p.topMargin = dp(margin); parent.addView(child, p); }
    private LinearLayout card(int radius) { LinearLayout c = column(); c.setBackground(bordered(surface, radius, line)); elev(c); return c; }
    private ImageView icon(int res, int color) {
        ImageView v = new ImageView(this); v.setImageResource(res); v.setColorFilter(color, PorterDuff.Mode.SRC_IN); v.setScaleType(ImageView.ScaleType.FIT_CENTER); return v;
    }
    private FrameLayout iconBox(int res, int iconColor, int boxColor, int iconDp, int radius) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(ripple(shape(boxColor, radius)));
        f.addView(icon(res, iconColor), new FrameLayout.LayoutParams(dp(iconDp), dp(iconDp), Gravity.CENTER));
        return f;
    }
    private ImageView logoView(int idx, int sizeDp) {
        ImageView logo = new ImageView(this); logo.setImageResource(classLogos[idx]); logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setLayoutParams(lp(sizeDp, sizeDp)); return logo;
    }
    private TextView button(String label, int fill, int textColor) {
        TextView b = text(label, 16, textColor, true); b.setGravity(Gravity.CENTER); b.setBackground(ripple(shape(fill, 16))); b.setMinHeight(dp(54)); pad(b, 18, 14, 18, 14);
        pressable(b); return b;
    }
    private void haptic(View v) { try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) { } }
    private void hideKeyboard() {
        try {
            View focus = getCurrentFocus();
            if (focus != null) {
                InputMethodManager m = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (m != null) m.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                focus.clearFocus();
            }
        } catch (Exception ignored) { }
    }

    // ---- Motion helpers ----
    private void fadeUp(View v, long delay) {
        v.setAlpha(0f); v.setTranslationY(dp(14));
        v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(320).setInterpolator(new DecelerateInterpolator(1.6f)).start();
    }
    private void stagger(ViewGroup group, int maxItems) {
        int n = Math.min(group.getChildCount(), maxItems);
        for (int i = 0; i < n; i++) fadeUp(group.getChildAt(i), i * 45L);
    }
    private void pop(View v) {
        v.setScaleX(.5f); v.setScaleY(.5f); v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).setInterpolator(new OvershootInterpolator(2.2f)).start();
    }
    private void pressable(View v) {
        v.setOnTouchListener((view, e) -> {
            if (!view.isEnabled()) return false;
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) view.animate().scaleX(.97f).scaleY(.97f).setDuration(90).start();
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
                view.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(new OvershootInterpolator(2.5f)).start();
            return false;
        });
    }
    private void shake(View v) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, View.TRANSLATION_X, 0f, -dp(10), dp(10), -dp(7), dp(7), -dp(3), 0f);
        a.setDuration(380); a.start(); haptic(v);
    }
    private void pulse(View v) {
        final ObjectAnimator a = ObjectAnimator.ofFloat(v, View.ALPHA, .45f, 1f);
        a.setDuration(850); a.setRepeatCount(ValueAnimator.INFINITE); a.setRepeatMode(ValueAnimator.REVERSE); a.start();
        v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { }
            @Override public void onViewDetachedFromWindow(View view) { a.cancel(); }
        });
    }
    private View skeleton(int heightDp, int radius) {
        View v = new View(this); v.setBackground(shape(tint(muted, .16f), radius)); pulse(v); return v;
    }
    private void skeletonList(LinearLayout parent, int count, int heightDp) {
        for (int i = 0; i < count; i++) {
            LinearLayout.LayoutParams p = lp(-1, heightDp); p.topMargin = dp(i == 0 ? 0 : 12);
            parent.addView(skeleton(heightDp, 20), p);
        }
    }

    /** Small pill message shown at the top of the screen (replaces the system Toast). */
    private void toast(String value) {
        try {
            final ViewGroup host = (ViewGroup) getWindow().getDecorView();
            if (snackView != null) { ViewGroup old = (ViewGroup) snackView.getParent(); if (old != null) old.removeView(snackView); snackView = null; }
            if (snackHide != null) ui.removeCallbacks(snackHide);
            final TextView t = text(value, 14, dark ? Color.rgb(18, 24, 32) : Color.WHITE, true);
            t.setGravity(Gravity.CENTER); t.setMaxWidth(dp(340));
            t.setBackground(shape(dark ? Color.rgb(228, 235, 243) : Color.rgb(28, 40, 56), 18));
            pad(t, 18, 12, 18, 12); t.setElevation(dp(10));
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            p.topMargin = insetTop + dp(12);
            host.addView(t, p);
            t.setAlpha(0f); t.setTranslationY(-dp(16));
            t.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();
            snackView = t;
            snackHide = () -> t.animate().alpha(0f).translationY(-dp(12)).setDuration(200).withEndAction(() -> {
                ViewGroup parent = (ViewGroup) t.getParent();
                if (parent != null) parent.removeView(t);
                if (snackView == t) snackView = null;
            }).start();
            ui.postDelayed(snackHide, value.length() > 50 ? 3600 : 2500);
        } catch (Exception error) {
            if (fallbackToast != null) fallbackToast.cancel();
            fallbackToast = Toast.makeText(this, value, Toast.LENGTH_SHORT); fallbackToast.show();
        }
    }

    private void applyInsets(View v) {
        v.setOnApplyWindowInsetsListener((view, insets) -> {
            int t, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                t = bars.top; b = Math.max(bars.bottom, ime.bottom);
            } else { t = insets.getSystemWindowInsetTop(); b = insets.getSystemWindowInsetBottom(); }
            insetTop = t;
            view.setPadding(0, t, 0, b);
            return insets;
        });
        v.requestApplyInsets();
    }

    /** Single-line or multi-line text input without its own frame; wrap with box() for a bordered field. */
    private EditText field(String hint) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setTextSize(16); e.setTextColor(ink); e.setHintTextColor(muted); e.setHint(hint);
        e.setBackground(null); e.setPadding(dp(16), 0, dp(16), 0); e.setContentDescription(hint); return e;
    }
    private LinearLayout box(EditText e, View trailing) {
        final LinearLayout b = row(); b.setBackground(bordered(surface, 14, line));
        b.addView(e, new LinearLayout.LayoutParams(0, dp(54), 1f));
        if (trailing != null) { pad(trailing, 14, 0, 14, 0); trailing.setMinimumHeight(dp(54)); b.addView(trailing, new LinearLayout.LayoutParams(-2, dp(54))); }
        final EditText input = e;
        e.setOnFocusChangeListener((v, focus) -> b.setBackground(focus ? bordered(surface, 14, solid(), 2) : bordered(surface, 14, line)));
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(Editable s) { b.setBackground(input.hasFocus() ? bordered(surface, 14, solid(), 2) : bordered(surface, 14, line)); }
        });
        return b;
    }
    private void fieldError(View box, String message) {
        box.setBackground(bordered(surface, 14, danger, 2)); shake(box); toast(message);
    }
    private void labeled(LinearLayout parent, String label, View input, int topMargin) {
        top(parent, text(label, 13, muted, true), topMargin);
        top(parent, input, 7);
    }

    /** Wrapping selectable chips (two per row). */
    private LinearLayout chips(String[] labels, int[] sel) {
        LinearLayout wrap = column(); ArrayList<TextView> all = new ArrayList<>();
        Runnable refresh = () -> { for (int i = 0; i < all.size(); i++) styleChip(all.get(i), i == sel[0]); };
        LinearLayout current = null;
        for (int i = 0; i < labels.length; i++) {
            if (i % 2 == 0) { current = row(); LinearLayout.LayoutParams rp = lp(-1, -2); if (i > 0) rp.topMargin = dp(8); wrap.addView(current, rp); }
            final int index = i;
            TextView chip = text(labels[i], 14, ink, false); chip.setGravity(Gravity.CENTER); chip.setSingleLine(true); chip.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            if (i % 2 == 1) cp.setMarginStart(dp(8));
            current.addView(chip, cp); all.add(chip); pressable(chip);
            chip.setOnClickListener(v -> { sel[0] = index; refresh.run(); haptic(v); });
        }
        refresh.run();
        return wrap;
    }
    private void styleChip(TextView chip, boolean selected) {
        chip.setBackground(ripple(selected ? bordered(tint(brand, .14f), 14, brand, 2) : bordered(surface, 14, line)));
        chip.setTextColor(selected ? brand : ink); chip.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    // =====================================================================
    // Bottom sheet (slides up, dims the screen) – used for announcements
    // =====================================================================
    private class Sheet {
        final Dialog dialog; final LinearLayout panel; final View scrim; boolean closing;
        Sheet(View body) {
            dialog = new Dialog(MainActivity.this, android.R.style.Theme_Translucent_NoTitleBar);
            final FrameLayout frame = new FrameLayout(MainActivity.this); frame.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            scrim = new View(MainActivity.this); scrim.setBackgroundColor(Color.BLACK); scrim.setAlpha(0f);
            frame.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
            panel = column();
            float r = dp(28);
            GradientDrawable bgd = new GradientDrawable(); bgd.setColor(surface); bgd.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
            panel.setBackground(bgd);
            View grab = new View(MainActivity.this); grab.setBackground(shape(line, 3));
            LinearLayout.LayoutParams gp = lp(42, 5); gp.gravity = Gravity.CENTER_HORIZONTAL; gp.topMargin = dp(10); gp.bottomMargin = dp(2);
            panel.addView(grab, gp);
            ScrollView sv = new ScrollView(MainActivity.this); sv.setOverScrollMode(View.OVER_SCROLL_NEVER); sv.addView(body);
            panel.addView(sv, new LinearLayout.LayoutParams(-1, -2));
            FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM); pp.topMargin = dp(48);
            frame.addView(panel, pp);
            scrim.setOnClickListener(v -> close());
            frame.setOnApplyWindowInsetsListener((v, insets) -> {
                int bottom = Build.VERSION.SDK_INT >= 30 ? insets.getInsets(WindowInsets.Type.systemBars()).bottom : insets.getSystemWindowInsetBottom();
                panel.setPadding(0, 0, 0, bottom);
                return insets;
            });
            dialog.setContentView(frame);
            Window w = dialog.getWindow();
            if (w != null) { w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); w.setLayout(-1, -1); w.setWindowAnimations(0); w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE); }
            dialog.setOnKeyListener((d, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) { close(); return true; }
                return false;
            });
        }
        void show() {
            dialog.show();
            panel.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override public boolean onPreDraw() {
                    panel.getViewTreeObserver().removeOnPreDrawListener(this);
                    panel.setTranslationY(panel.getHeight());
                    panel.animate().translationY(0f).setDuration(320).setInterpolator(new DecelerateInterpolator(2f)).start();
                    scrim.animate().alpha(.55f).setDuration(320).start();
                    return true;
                }
            });
        }
        void close() {
            if (closing) return; closing = true;
            scrim.animate().alpha(0f).setDuration(200).start();
            panel.animate().translationY(panel.getHeight()).setDuration(230).setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> { try { dialog.dismiss(); } catch (Exception ignored) { } }).start();
        }
    }

    private int dialogTheme() { return dark ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert; }
    /** Rounded corners + themed buttons for standard dialogs. */
    private void styleDialog(AlertDialog d) {
        try {
            if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(new InsetDrawable(shape(surface, 24), dp(14)));
            int[] which = {AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL};
            for (int w : which) { Button b = d.getButton(w); if (b != null) b.setTextColor(w == AlertDialog.BUTTON_NEGATIVE ? muted : accentText(accent)); }
        } catch (Exception ignored) { }
    }
    private void showStyled(AlertDialog.Builder builder) { AlertDialog d = builder.create(); d.show(); styleDialog(d); }

    // =====================================================================
    // Splash
    // =====================================================================
    private void showSplash() {
        FrameLayout f = new FrameLayout(this); f.setBackgroundColor(bg);
        LinearLayout c = column(); c.setGravity(Gravity.CENTER);
        ImageView logo = new ImageView(this); logo.setImageResource(R.mipmap.ic_launcher);
        c.addView(logo, lp(88, 88));
        TextView t = text(organizationArabic, 18, brand, true); t.setGravity(Gravity.CENTER); top(c, t, 18);
        TextView s = text(branchArabic, 13, green, true); s.setGravity(Gravity.CENTER); top(c, s, 4);
        f.addView(c, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        setContentView(f);
        pulse(logo);
        fadeUp(c, 0);
    }

    // =====================================================================
    // Auth
    // =====================================================================
    private TextView passwordToggle(final EditText passField) {
        final TextView toggle = text("إظهار", 13, accentText(accent), true); toggle.setGravity(Gravity.CENTER);
        final boolean[] visible = {false};
        toggle.setOnClickListener(v -> {
            visible[0] = !visible[0];
            passField.setTransformationMethod(visible[0] ? null : PasswordTransformationMethod.getInstance());
            passField.setSelection(passField.getText().length());
            toggle.setText(visible[0] ? "إخفاء" : "إظهار");
        });
        return toggle;
    }

    private void showAuth(boolean signup) {
        authSignupVisible = signup; authBusy = false; authIdle = null;
        final int token = ++screenToken;
        accent = signup && selectedClass >= 0 ? classColors[selectedClass] : defaultAccent();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(bg);
        LinearLayout form = column(); pad(form, 20, 14, 20, 32); scroll.addView(form); setContentView(scroll); applyInsets(scroll);
        FirebaseUser signedIn = auth.getCurrentUser();

        if (signup) {
            LinearLayout back = row(); pad(back, 4, 8, 12, 8);
            back.addView(icon(R.drawable.ic_back, accentText(defaultAccent())), lp(20, 20));
            LinearLayout.LayoutParams backLabel = lp(-2, -2); backLabel.setMarginStart(dp(6));
            back.addView(text("العودة إلى تسجيل الدخول", 14, accentText(defaultAccent()), true), backLabel);
            form.addView(back, lp(-2, -2));
            pressable(back);
            back.setOnClickListener(v -> backToSignIn());
        }

        final LinearLayout hero = column(); pad(hero, 20, 20, 20, 22);
        hero.setBackground(gradient(heroBg, blend(vivid(accent), heroBg, .38f), 28));
        LinearLayout brandRow = row();
        ImageView logo = new ImageView(this); logo.setImageResource(R.mipmap.ic_launcher); brandRow.addView(logo, lp(52, 52));
        LinearLayout names = column(); names.addView(text(organizationArabic, 16, Color.WHITE, true)); top(names, text(branchArabic, 13, heroSub, true), 2);
        brandRow.addView(names, weighted(12)); hero.addView(brandRow);
        top(hero, text(signup ? "انضم إلى فرع السلام" : "مرحبًا بعودتك", 28, Color.WHITE, true), 22);
        top(hero, text(signup ? "أنشئ حسابك واختر فئتك الكشفية." : "سجّل الدخول للوصول إلى مجتمعك الكشفي.", 14, heroSub, false), 6);
        LinearLayout.LayoutParams heroParams = lp(-1, -2); heroParams.topMargin = dp(signup ? 6 : 14); form.addView(hero, heroParams);

        final EditText nameField = signup ? field("اسمك الكامل") : null;
        final LinearLayout nameBox = nameField == null ? null : box(nameField, null);
        if (nameField != null) {
            nameField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            nameField.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            if (Build.VERSION.SDK_INT >= 26) { nameField.setAutofillHints("name"); nameField.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES); }
            if (legacyClassSetup && user != null) nameField.setText(user);
            labeled(form, "الاسم", nameBox, 24);
        }

        final EditText emailField = field("name@example.com");
        if (Build.VERSION.SDK_INT >= 26) { emailField.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS); emailField.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES); }
        emailField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        emailField.setTextDirection(View.TEXT_DIRECTION_LTR); emailField.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        if (signup && signedIn != null && signedIn.getEmail() != null) { emailField.setText(signedIn.getEmail()); emailField.setEnabled(false); }
        else if (!signup) { String last = prefs.getString("last_email", ""); if (last != null && !last.isEmpty()) emailField.setText(last); }
        final LinearLayout emailBox = box(emailField, null);
        labeled(form, "البريد الإلكتروني", emailBox, signup ? 16 : 24);
        if (!signup) {
            TextView saved = text("اختيار حساب محفوظ", 13, accentText(accent), true); pad(saved, 0, 6, 0, 6); top(form, saved, 2);
            saved.setOnClickListener(v -> showSavedAccounts(emailField));
        }

        final EditText passField = signedIn == null ? field("كلمة المرور") : null;
        final LinearLayout passBox;
        if (passField != null) {
            if (Build.VERSION.SDK_INT >= 26) passField.setAutofillHints(signup ? "newPassword" : View.AUTOFILL_HINT_PASSWORD);
            passField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passField.setImeOptions(EditorInfo.IME_ACTION_DONE);
            passField.setTextDirection(View.TEXT_DIRECTION_LTR); passField.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            passBox = box(passField, passwordToggle(passField));
            labeled(form, "كلمة المرور", passBox, 16);
        } else passBox = null;

        final int[] genderSel = {(legacyClassSetup || signedIn != null) ? genderPosition(gender) : -1};
        final TextView[] submitRef = new TextView[1];
        if (signup) {
            top(form, text("الجنس", 13, muted, true), 20); top(form, chips(genderLabels, genderSel), 7);
            top(form, text("الفئة الكشفية", 19, ink, true), 28);
            top(form, text("فئتك تحدّد لونك ودردشتك الخاصة.", 13, muted, false), 4);
            LinearLayout options = column(); top(form, options, 12);
            final LinearLayout[] rows = new LinearLayout[MEMBER_CLASS_COUNT]; final FrameLayout[] checks = new FrameLayout[MEMBER_CLASS_COUNT];
            for (int i = 0; i < MEMBER_CLASS_COUNT; i++) {
                final int index = i;
                LinearLayout option = row(); rows[i] = option; pad(option, 12, 12, 14, 12);
                option.addView(logoView(i, 50));
                LinearLayout labels = column();
                labels.addView(text(classArabic[i], 16, ink, true));
                top(labels, text(classAges[i], 12, muted, false), 2);
                top(labels, text(classTaglines[i], 12, accentText(classColors[i]), true), 2);
                option.addView(labels, weighted(12));
                FrameLayout check = iconBox(R.drawable.ic_check, onColor(classColors[i]), classColors[i], 14, 12);
                checks[i] = check; option.addView(check, lp(24, 24));
                LinearLayout.LayoutParams op = lp(-1, -2); if (i > 0) op.topMargin = dp(10); options.addView(option, op);
                pressable(option);
                option.setOnClickListener(v -> {
                    selectedClass = index; accent = classColors[index]; haptic(v);
                    for (int j = 0; j < rows.length; j++) styleClassOption(rows[j], checks[j], j, j == index);
                    hero.setBackground(gradient(heroBg, blend(vivid(accent), heroBg, .38f), 28));
                    if (submitRef[0] != null) { submitRef[0].setBackground(ripple(shape(solid(), 16))); submitRef[0].setTextColor(onColor(solid())); }
                });
                styleClassOption(option, check, i, false);
            }
            top(form, text("الرتبة عند التسجيل: عضو", 13, muted, true), 14);
        }

        if (!signup) {
            final TextView forgot = text("نسيت كلمة المرور؟", 13, accentText(accent), true); pad(forgot, 0, 8, 0, 8); top(form, forgot, 6);
            final Runnable[] tick = new Runnable[1];
            tick[0] = () -> {
                if (token != screenToken) return;
                long remain = resetRemainingMs(emailField.getText().toString());
                boolean waiting = resetSending || remain > 0;
                forgot.setText(resetSending ? "جارٍ الإرسال…" : (remain > 0 ? "أُرسل الرابط · إعادة الإرسال بعد " + ((remain + 999) / 1000) + " ث" : "نسيت كلمة المرور؟"));
                forgot.setAlpha(waiting ? .6f : 1f);
                ui.removeCallbacks(tick[0]);
                if (waiting) ui.postDelayed(tick[0], 500);
            };
            forgot.setOnClickListener(v -> requestPasswordReset(emailField.getText().toString(), tick[0]));
            emailField.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(Editable s) { tick[0].run(); }
            });
            tick[0].run();
        }

        final String idleLabel = signup ? (signedIn != null ? "إكمال الحساب" : "إنشاء الحساب") : "تسجيل الدخول";
        final TextView submit = button(idleLabel, solid(), onColor(solid()));
        top(form, submit, signup ? 22 : 16); if (signup) submitRef[0] = submit;
        authIdle = () -> { submit.setText(idleLabel); submit.setAlpha(1f); };
        submit.setOnClickListener(v -> {
            if (authBusy) return;
            String e = emailField.getText().toString().trim(), p = passField == null ? "" : passField.getText().toString();
            String n = nameField == null ? "" : nameField.getText().toString().trim();
            if (signup && n.isEmpty()) { fieldError(nameBox, "اكتب اسمك الكامل."); return; }
            if (e.isEmpty()) { fieldError(emailBox, "اكتب بريدك الإلكتروني."); return; }
            if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) { fieldError(emailBox, "صيغة البريد الإلكتروني غير صحيحة."); return; }
            if (passField != null && p.isEmpty()) { fieldError(passBox, "اكتب كلمة المرور."); return; }
            if (passField != null && p.length() < 6) { fieldError(passBox, "كلمة المرور يجب ألا تقل عن 6 أحرف."); return; }
            if (signup) {
                if (genderSel[0] < 0) { toast("اختر الجنس."); return; }
                if (selectedClass < 0) { toast("اختر فئتك الكشفية أولًا."); return; }
            }
            authBusy = true; submit.setText("جارٍ المعالجة…"); submit.setAlpha(.75f); hideKeyboard();
            if (signup) createAccount(n, e, p, genderValues[genderSel[0]], selectedClass);
            else {
                auth.signInWithEmailAndPassword(e, p).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) { rememberAccount(e); loadProfile(); }
                    else { authDone(); toast(authFailureMessage(task.getException(), false)); }
                });
            }
        });
        if (passField != null) {
            passField.setOnEditorActionListener((tv, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) { if (signup) hideKeyboard(); else submit.performClick(); return true; }
                return false;
            });
        }
        if (signedIn == null) {
            TextView toggle = text(signup ? "لديك حساب؟ سجّل الدخول" : "مستخدم جديد؟ أنشئ حسابًا", 14, accentText(accent), true);
            toggle.setGravity(Gravity.CENTER); pad(toggle, 8, 14, 8, 14); top(form, toggle, 10);
            toggle.setOnClickListener(v -> { selectedClass = -1; showAuth(!signup); });
        }
        stagger(form, 12);
    }
    private void authDone() { authBusy = false; if (authIdle != null) authIdle.run(); }
    private void backToSignIn() {
        if (auth.getCurrentUser() != null) auth.signOut();
        legacyClassSetup = false; selectedClass = -1; gender = "";
        showAuth(false);
    }
    private void styleClassOption(LinearLayout option, FrameLayout check, int i, boolean animate) {
        boolean sel = i == selectedClass;
        option.setBackground(ripple(sel ? bordered(tint(classColors[i], .12f), 18, classColors[i], 2) : bordered(surface, 18, line)));
        check.setVisibility(sel ? View.VISIBLE : View.INVISIBLE);
        if (sel && animate) pop(check);
    }

    private int genderPosition(String value) { for (int i = 0; i < genderValues.length; i++) if (genderValues[i].equals(value)) return i; return -1; }
    private String genderLabel(String value) { int p = genderPosition(value); return p < 0 ? "غير محدد" : genderLabels[p]; }
    private String rankLabel(String value) { if ("member".equals(value)) return "عضو"; if ("admin".equals(value)) return "مشرف"; return value; }
    private String classId(int index) { return classIds[index]; }
    private int classIndex(String id) { for (int i = 0; i < classIds.length; i++) if (classIds[i].equals(id)) return i; return 0; }

    private void createAccount(String name, String email, String password, String genderValue, int classPosition) {
        FirebaseUser current = auth.getCurrentUser();
        if (current != null && email.equalsIgnoreCase(current.getEmail())) { saveProfile(current, name, email, genderValue, classPosition); return; }
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { authDone(); toast(authFailureMessage(task.getException(), true)); return; }
            rememberAccount(email);
            saveProfile(auth.getCurrentUser(), name, email, genderValue, classPosition);
        });
    }
    private String authFailureMessage(Exception error, boolean creating) {
        String code = error instanceof FirebaseAuthException ? ((FirebaseAuthException) error).getErrorCode() : "";
        if (code == null) code = "";
        if (error instanceof FirebaseNetworkException || code.contains("NETWORK")) return "لا يوجد اتصال بالإنترنت. اتصل بالشبكة ثم حاول مجددًا.";
        if (code.contains("TOO_MANY")) return "محاولات كثيرة. انتظر قليلًا ثم حاول مجددًا.";
        if (creating) {
            if (code.contains("EMAIL_ALREADY_IN_USE")) return "هذا البريد مسجّل بالفعل. سجّل الدخول بدل إنشاء حساب جديد.";
            if (code.contains("WEAK_PASSWORD")) return "كلمة المرور ضعيفة. اختر كلمة أقوى.";
            if (code.contains("INVALID_EMAIL")) return "صيغة البريد الإلكتروني غير صحيحة.";
            return "تعذّر إنشاء الحساب. تحقق من البريد وكلمة المرور ثم حاول مجددًا.";
        }
        if (error instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) error).getCode() == FirebaseFirestoreException.Code.UNAVAILABLE)
            return "لا يوجد اتصال بالإنترنت. اتصل بالشبكة ثم حاول مجددًا.";
        return "تعذّر تسجيل الدخول. تحقق من البريد وكلمة المرور.";
    }
    private void rememberAccount(String email) {
        if (prefs == null || email == null || email.trim().isEmpty()) return;
        Set<String> saved = new HashSet<>(prefs.getStringSet("saved_accounts", Collections.emptySet()));
        saved.add(email.trim().toLowerCase(Locale.ROOT));
        prefs.edit().putStringSet("saved_accounts", saved).putString("last_email", email.trim()).apply();
    }
    private void showSavedAccounts(EditText emailField) {
        Set<String> saved = prefs.getStringSet("saved_accounts", Collections.emptySet());
        if (saved.isEmpty()) { toast("لا توجد حسابات محفوظة على هذا الجهاز بعد."); return; }
        ArrayList<String> accounts = new ArrayList<>(saved); Collections.sort(accounts);
        showStyled(new AlertDialog.Builder(this, dialogTheme()).setTitle("اختيار حساب")
            .setItems(accounts.toArray(new String[0]), (dialog, which) -> emailField.setText(accounts.get(which)))
            .setNegativeButton("إلغاء", null));
    }

    // ---- Password reset: one email per request, with an in-flight guard and a 60 s cooldown per address ----
    private long resetRemainingMs(String email) {
        if (email == null || email.trim().isEmpty()) return 0;
        long until = prefs.getLong("reset_until_" + email.trim().toLowerCase(Locale.ROOT), 0);
        return Math.max(0, Math.min(RESET_COOLDOWN_MS, until - System.currentTimeMillis()));
    }
    private void requestPasswordReset(String rawEmail, Runnable refreshLabel) {
        if (resetSending) return;
        final String email = rawEmail.trim();
        if (email.isEmpty()) { toast("اكتب بريدك الإلكتروني أولًا."); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { toast("صيغة البريد الإلكتروني غير صحيحة."); return; }
        long remain = resetRemainingMs(email);
        if (remain > 0) {
            toast("أرسلنا الرابط قبل لحظات. تحقق من بريدك (وملف البريد المزعج) أو أعد المحاولة بعد " + ((remain + 999) / 1000) + " ثانية.");
            refreshLabel.run(); return;
        }
        resetSending = true; refreshLabel.run(); hideKeyboard();
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            resetSending = false;
            if (task.isSuccessful()) {
                prefs.edit().putLong("reset_until_" + email.toLowerCase(Locale.ROOT), System.currentTimeMillis() + RESET_COOLDOWN_MS).apply();
                toast("إذا كان البريد مسجّلًا لدينا فستصلك رسالة استعادة كلمة المرور. تحقق أيضًا من البريد المزعج.");
            } else {
                Exception error = task.getException();
                String code = error instanceof FirebaseAuthException ? ((FirebaseAuthException) error).getErrorCode() : "";
                if (error instanceof FirebaseNetworkException || (code != null && code.contains("NETWORK"))) toast("لا يوجد اتصال بالإنترنت. اتصل بالشبكة ثم حاول مجددًا.");
                else if (code != null && code.contains("TOO_MANY")) toast("محاولات كثيرة. انتظر قليلًا ثم حاول مجددًا.");
                else toast("تعذّر إرسال رسالة الاستعادة. تحقق من البريد ثم حاول مجددًا.");
            }
            refreshLabel.run();
        });
    }

    private void switchAccount() {
        stopAllListeners(); latestNews = null; newsUnread = 0;
        auth.signOut(); legacyClassSetup = false; isAdmin = false; user = ""; gender = ""; scoutClassId = ""; approvalStatus = "approved"; selectedClass = -1;
        page = "Home"; accent = defaultAccent(); showAuth(false);
    }

    private void saveProfile(FirebaseUser account, String name, String email, String genderValue, int classPosition) {
        if (account == null) { authDone(); toast("الرجاء تسجيل الدخول من جديد."); return; }
        String verifiedEmail = account.getEmail() == null ? email : account.getEmail();
        gender = genderValue;
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name); profile.put("email", verifiedEmail); profile.put("gender", genderValue); profile.put("scoutClass", classId(classPosition));
        DocumentReference ref = db.collection("users").document(account.getUid());
        if (legacyClassSetup) {
            profile.put("clan", FieldValue.delete());
            ref.update(profile).addOnSuccessListener(unused -> { legacyClassSetup = false; finishProfile(name, verifiedEmail, genderValue, classPosition, rank); })
                .addOnFailureListener(error -> { toast("تعذّر حفظ فئتك. حدّث قواعد Firestore وحاول مجددًا."); showAuth(true); });
        } else {
            profile.put("rank", "member"); profile.put("approvalStatus", "pending"); profile.put("rejectionReason", ""); profile.put("appealMessage", ""); profile.put("createdAt", FieldValue.serverTimestamp());
            ref.set(profile).addOnSuccessListener(unused -> { rememberAccount(verifiedEmail); approvalStatus = "pending"; finishProfile(name, verifiedEmail, genderValue, classPosition, "member"); })
                .addOnFailureListener(error -> {
                    String hint = error instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) error).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED
                        ? "تحقق من نشر قواعد Firestore المحدّثة."
                        : "تحقق من الاتصال ثم حاول مجددًا.";
                    String code = error instanceof FirebaseFirestoreException ? ((FirebaseFirestoreException) error).getCode().name() : error.getClass().getSimpleName();
                    toast("تم إنشاء الحساب، لكن تعذّر حفظ الملف الشخصي. " + hint + " رمز الخطأ: " + code);
                    showAuth(true);
                });
        }
    }
    private void finishProfile(String name, String email, String genderValue, int classPosition, String savedRank) {
        user = name; emailAddress = email; gender = genderValue; scoutClassId = classId(classPosition); rank = savedRank; accent = classColors[classPosition];
        page = "Home"; loadAdminStatus();
    }
    private void loadProfile() {
        FirebaseUser account = auth.getCurrentUser();
        if (account == null) { showAuth(false); return; }
        db.collection("users").document(account.getUid()).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) { legacyClassSetup = false; user = ""; gender = ""; selectedClass = -1; toast("أكمل إعداد ملفك الشخصي."); showAuth(true); return; }
            user = snapshot.getString("name") == null ? "كشاف" : snapshot.getString("name");
            emailAddress = snapshot.getString("email") == null ? account.getEmail() : snapshot.getString("email");
            gender = snapshot.getString("gender") == null ? "" : snapshot.getString("gender");
            rank = snapshot.getString("rank") == null ? "member" : snapshot.getString("rank");
            approvalStatus = snapshot.getString("approvalStatus") == null ? "approved" : snapshot.getString("approvalStatus");
            rejectionReason = snapshot.getString("rejectionReason") == null ? "" : snapshot.getString("rejectionReason");
            appealMessage = snapshot.getString("appealMessage") == null ? "" : snapshot.getString("appealMessage");
            if ("admin".equals(rank)) approvalStatus = "approved";
            String storedClass = snapshot.getString("scoutClass");
            if ("admin".equals(rank)) scoutClassId = "admins"; else scoutClassId = storedClass;
            if (!"admin".equals(rank) && (storedClass == null || !Arrays.asList(classIds).subList(0, MEMBER_CLASS_COUNT).contains(storedClass))) { legacyClassSetup = true; selectedClass = -1; toast("اختر فئتك الكشفية لإكمال ملفك الشخصي."); showAuth(true); return; }
            rememberAccount(emailAddress);
            legacyClassSetup = false; accent = classColors[classIndex(scoutClassId)];
            loadAdminStatus();
        }).addOnFailureListener(error -> { toast("تعذّر تحميل ملفك الشخصي. تحقق من الاتصال."); showAuth(false); });
    }
    private void loadAdminStatus() {
        FirebaseUser account = auth.getCurrentUser();
        if (account == null) { isAdmin = false; enterApp(); return; }
        db.collection("admins").document(account.getUid()).get().addOnSuccessListener(doc -> {
            isAdmin = "admin".equals(rank) || doc.exists();
            enterApp();
        }).addOnFailureListener(error -> {
            isAdmin = "admin".equals(rank);
            enterApp();
        });
    }
    private void enterApp() {
        if (isAdmin) { approvalStatus = "approved"; scoutClassId = "admins"; accent = classColors[classIndex(scoutClassId)]; page = "Home"; }
        else page = "pending".equals(approvalStatus) ? "Pending" : ("rejected".equals(approvalStatus) ? "Rejected" : "Home");
        startNewsWatcher();
        showApp();
    }
    /** Page-scoped listeners (chat, announcements feed, room previews). */
    private void clearListeners() {
        if (chatListener != null) { chatListener.remove(); chatListener = null; }
        if (announcementListener != null) { announcementListener.remove(); announcementListener = null; }
        for (ListenerRegistration r : previewListeners) r.remove();
        previewListeners.clear();
    }
    private void stopAllListeners() {
        clearListeners();
        if (newsWatcher != null) { newsWatcher.remove(); newsWatcher = null; }
    }

    // =====================================================================
    // App shell
    // =====================================================================
    private ScrollView pageScroll;
    private FrameLayout[] navPills;
    private ImageView[] navIcons;
    private TextView[] navCaptions;
    private TextView newsTileBadge;
    private final String[] navTabs = {"Home", "Chat", "News", "Profile", "Settings"};

    private void showApp() {
        authSignupVisible = false;
        clearListeners();
        if (!page.equals("Room")) mediaViews.clear();
        newsDot = null; newsTileBadge = null; latestNewsHolder = null; navPills = null; roomScroll = null; newMessagePill = null;
        root = column(); root.setBackgroundColor(bg); setContentView(root); applyInsets(root);
        if (page.equals("Room")) { roomScreen(); return; }
        pageScroll = new ScrollView(this); pageScroll.setFillViewport(true); pageScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(pageScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        content = column(); pad(content, 20, 20, 20, 24); pageScroll.addView(content);
        if (page.equals("Settings")) content.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        renderPage();
        stagger(content, 9);
        if (isAdmin || "approved".equals(approvalStatus)) bottomNav();
    }
    /** Switches page without rebuilding the bottom bar, so tab changes feel instant and smooth. */
    private void goTo(String target) {
        if (root == null || content == null || navPills == null || page.equals("Room")) { page = target; showApp(); return; }
        if (target.equals(page)) { pageScroll.smoothScrollTo(0, 0); return; }
        page = target; clearListeners(); latestNewsHolder = null; newsTileBadge = null;
        content.removeAllViews();
        content.setLayoutDirection(page.equals("Settings") ? View.LAYOUT_DIRECTION_LTR : View.LAYOUT_DIRECTION_INHERIT);
        pageScroll.scrollTo(0, 0);
        renderPage();
        stagger(content, 9);
        styleNav(true);
    }
    private void bottomNav() {
        View divider = new View(this); divider.setBackgroundColor(line); root.addView(divider, lp(-1, 1));
        LinearLayout nav = row(); nav.setGravity(Gravity.CENTER); nav.setBackgroundColor(surface); pad(nav, 4, 8, 4, 8);
        int[] icons = {R.drawable.ic_home, R.drawable.ic_chat, R.drawable.ic_news, R.drawable.ic_person, R.drawable.ic_settings};
        String[] labels = {"الرئيسية", "الدردشة", "الإعلانات", "ملفي", "الإعدادات"};
        navPills = new FrameLayout[navTabs.length]; navIcons = new ImageView[navTabs.length]; navCaptions = new TextView[navTabs.length];
        for (int i = 0; i < navTabs.length; i++) {
            final String tab = navTabs[i];
            LinearLayout item = column(); item.setGravity(Gravity.CENTER);
            FrameLayout pill = new FrameLayout(this);
            ImageView ic = icon(icons[i], muted);
            pill.addView(ic, new FrameLayout.LayoutParams(dp(23), dp(23), Gravity.CENTER));
            if (tab.equals("News")) {
                View dot = new View(this); dot.setBackground(bordered(danger, 6, surface, 2));
                FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(12), dp(12), Gravity.TOP | Gravity.END);
                dotParams.topMargin = dp(1); dotParams.setMarginEnd(dp(13));
                pill.addView(dot, dotParams); newsDot = dot; dot.setVisibility(newsUnread > 0 ? View.VISIBLE : View.GONE);
            }
            item.addView(pill, lp(54, 30));
            TextView caption = text(labels[i], 11, muted, false); caption.setGravity(Gravity.CENTER); caption.setSingleLine(true);
            top(item, caption, 3);
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
            pressable(item);
            item.setOnClickListener(v -> goTo(tab));
            navPills[i] = pill; navIcons[i] = ic; navCaptions[i] = caption;
        }
        root.addView(nav, lp(-1, -2));
        styleNav(false);
    }
    private void styleNav(boolean animate) {
        if (navPills == null) return;
        for (int i = 0; i < navTabs.length; i++) {
            boolean active = page.equals(navTabs[i]);
            navPills[i].setBackground(active ? shape(tint(accent, .18f), 16) : null);
            navIcons[i].setColorFilter(active ? accentText(accent) : muted, PorterDuff.Mode.SRC_IN);
            navCaptions[i].setTextColor(active ? accentText(accent) : muted);
            navCaptions[i].setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
            if (active && animate) pop(navPills[i]);
        }
    }
    private void updateNewsDot() {
        boolean show = newsUnread > 0;
        if (newsDot != null) {
            if (show && newsDot.getVisibility() != View.VISIBLE) { newsDot.setVisibility(View.VISIBLE); pop(newsDot); }
            else if (!show) newsDot.setVisibility(View.GONE);
        }
        if (newsTileBadge != null) {
            if (show) { newsTileBadge.setText(newsUnread > 9 ? "9+" : String.valueOf(newsUnread)); if (newsTileBadge.getVisibility() != View.VISIBLE) { newsTileBadge.setVisibility(View.VISIBLE); pop(newsTileBadge); } }
            else newsTileBadge.setVisibility(View.GONE);
        }
    }
    private void renderPage() {
        if (page.equals("Home")) homePage();
        else if (page.equals("News")) newsPage();
        else if (page.equals("Chat")) chatPage();
        else if (page.equals("Settings")) settingsPage();
        else if (page.equals("Approvals")) approvalsPage();
        else if (page.equals("Pending")) pendingApprovalPage();
        else if (page.equals("Rejected")) rejectedApprovalPage();
        else profilePage();
    }
    private String initial(String value) { return value == null || value.isEmpty() ? "ك" : value.substring(0, 1).toUpperCase(); }
    private String firstName() { String n = user == null ? "" : user.trim(); int sp = n.indexOf(' '); return sp > 0 ? n.substring(0, sp) : n; }
    private String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5 && h < 12) return "صباح الخير";
        if (h >= 12 && h < 17) return "طاب يومك";
        return "مساء الخير";
    }
    private void pageHeader(String title, String subtitle) {
        content.addView(text(title, 30, ink, true));
        top(content, text(subtitle, 14, muted, false), 6);
    }
    private void sectionLabel(String title) { top(content, text(title, 18, ink, true), 28); }

    // ---- Home ----
    private void homePage() {
        LinearLayout head = row(); LinearLayout b = column();
        b.addView(text(greeting() + "،", 14, muted, false)); top(b, text(firstName(), 27, ink, true), 2);
        head.addView(b, weighted(0));
        TextView avatar = text(initial(user), 18, accentText(accent), true); avatar.setGravity(Gravity.CENTER); avatar.setBackground(ripple(shape(tint(accent, .18f), 24)));
        pressable(avatar); avatar.setOnClickListener(v -> goTo("Profile"));
        head.addView(avatar, lp(48, 48)); content.addView(head);
        classHero();
        sectionLabel("مساحتك الكشفية");
        LinearLayout tiles = row(); tiles.setBaselineAligned(false);
        tile(R.drawable.ic_chat, "دردشة الفئة", "تحدّث مع أعضاء فئتك", "Chat", tiles, 0);
        FrameLayout newsTile = tile(R.drawable.ic_news, "الإعلانات", "أخبار الفرع وفئتك", "News", tiles, 12);
        newsTileBadge = text("", 11, Color.WHITE, true); newsTileBadge.setGravity(Gravity.CENTER); newsTileBadge.setBackground(shape(danger, 12)); pad(newsTileBadge, 7, 2, 7, 2);
        newsTileBadge.setMinWidth(dp(22));
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END); badgeParams.setMargins(dp(12), dp(12), dp(12), 0);
        newsTile.addView(newsTileBadge, badgeParams); newsTileBadge.setVisibility(View.GONE);
        top(content, tiles, 12);
        updateNewsDot();
        if (isAdmin) {
            LinearLayout link = quickLink(R.drawable.ic_person, "طلبات الانضمام", "مراجعة الحسابات الجديدة", "Approvals");
            final TextView count = text("", 12, Color.WHITE, true); count.setGravity(Gravity.CENTER); count.setBackground(shape(danger, 12)); pad(count, 8, 3, 8, 3);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2); cp.setMarginEnd(dp(8)); link.addView(count, 2, cp); count.setVisibility(View.GONE);
            db.collection("users").whereEqualTo("approvalStatus", "pending").get().addOnSuccessListener(snap -> {
                if (snap.size() > 0) { count.setText(String.valueOf(snap.size())); count.setVisibility(View.VISIBLE); pop(count); }
            });
        }
        latestNewsHolder = column(); content.addView(latestNewsHolder, lp(-1, -2));
        renderLatestNews();
    }
    private void renderLatestNews() {
        if (latestNewsHolder == null) return;
        latestNewsHolder.removeAllViews();
        if (latestNews == null) return;
        top(latestNewsHolder, text("آخر إعلان", 18, ink, true), 26);
        top(latestNewsHolder, newsCard(latestNews, isUnseen(latestNews)), 12);
    }
    private void classHero() {
        int idx = classIndex(scoutClassId);
        FrameLayout wrap = new FrameLayout(this);
        wrap.setBackground(gradient(heroBg, blend(vivid(classColors[idx]), heroBg, .42f), 26)); wrap.setClipToOutline(true);
        GradientDrawable circle = new GradientDrawable(); circle.setShape(GradientDrawable.OVAL); circle.setColor(Color.argb(28, 255, 255, 255));
        View bubble = new View(this); bubble.setBackground(circle);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(170), dp(170), Gravity.TOP | Gravity.END); bp.topMargin = -dp(60); bp.setMarginEnd(-dp(50));
        wrap.addView(bubble, bp);
        LinearLayout hero = column(); pad(hero, 20, 20, 20, 20);
        LinearLayout head = row(); head.addView(logoView(idx, 68));
        LinearLayout details = column(); head.addView(details, weighted(14));
        TextView badge = text("فئتك الكشفية", 11, onColor(classColors[idx]), true); badge.setBackground(shape(vivid(classColors[idx]), 12)); badge.setTextColor(onColor(vivid(classColors[idx]))); pad(badge, 10, 4, 10, 4);
        details.addView(badge, new LinearLayout.LayoutParams(-2, -2));
        top(details, text(classArabic[idx], 21, Color.WHITE, true), 8);
        top(details, text(classAges[idx], 12, heroSub, false), 3);
        hero.addView(head);
        top(hero, text(classTaglines[idx], 15, Color.WHITE, true), 16);
        top(hero, text(organizationArabic + " · " + branchArabic, 12, heroSub, false), 5);
        wrap.addView(hero, new FrameLayout.LayoutParams(-1, -2));
        top(content, wrap, 22);
    }
    private FrameLayout tile(int iconRes, String title, String subtitle, final String target, LinearLayout rowParent, int startMargin) {
        FrameLayout t = new FrameLayout(this); t.setBackground(ripple(bordered(surface, 22, line))); elev(t);
        LinearLayout inner = column(); pad(inner, 16, 16, 16, 16);
        inner.addView(iconBox(iconRes, accentText(accent), tint(accent, .18f), 24, 16), lp(48, 48));
        top(inner, text(title, 16, ink, true), 14);
        top(inner, text(subtitle, 12, muted, false), 3);
        t.addView(inner, new FrameLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1f); p.setMarginStart(dp(startMargin));
        rowParent.addView(t, p);
        pressable(t); t.setOnClickListener(v -> goTo(target));
        return t;
    }
    private LinearLayout quickLink(int iconRes, String title, String subtitle, final String target) {
        LinearLayout c = row(); c.setBackground(ripple(bordered(surface, 20, line))); elev(c); pad(c, 14, 14, 14, 14); top(content, c, 12);
        c.addView(iconBox(iconRes, accentText(accent), tint(accent, .18f), 22, 14), lp(46, 46));
        LinearLayout words = column(); words.addView(text(title, 16, ink, true)); top(words, text(subtitle, 13, muted, false), 3);
        c.addView(words, weighted(14)); c.addView(icon(R.drawable.ic_chevron, muted), lp(22, 22));
        pressable(c); c.setOnClickListener(v -> goTo(target));
        return c;
    }

    // ---- Chat list ----
    private void chatPage() {
        pageHeader("الدردشة", "اختر مساحة للحديث مع الأعضاء.");
        roomCard(-1, "الصالة العامة", "مفتوحة لجميع الأعضاء", "الجميع", brand, "global");
        if (isAdmin) {
            for (int i = 0; i < MEMBER_CLASS_COUNT; i++) roomCard(i, classArabic[i], classAges[i], "مشرف", classColors[i], classId(i));
            roomCard(-2, "دردشة الإدارة", "مساحة خاصة بالمشرفين", "إدارة", brand, "admins");
        } else { int idx = classIndex(scoutClassId); roomCard(idx, classArabic[idx], classAges[idx], "فئتي", classColors[idx], classId(idx)); }
    }
    private String shortTime(java.util.Date d) {
        Calendar now = Calendar.getInstance(), then = Calendar.getInstance(); then.setTime(d);
        boolean same = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR);
        return new SimpleDateFormat(same ? "h:mm a" : "d MMM", AR).format(d);
    }
    private void roomCard(int classIdx, final String title, String subtitle, String tag, int color, final String roomId) {
        LinearLayout c = row(); c.setBackground(ripple(bordered(surface, 22, line))); elev(c); pad(c, 14, 14, 14, 14); top(content, c, 12);
        if (classIdx >= 0) c.addView(logoView(classIdx, 52));
        else c.addView(iconBox(roomId.equals("admins") ? R.drawable.ic_person : R.drawable.ic_chat, accentText(color), tint(color, .16f), 24, 18), lp(52, 52));
        LinearLayout words = column(); c.addView(words, weighted(14));
        LinearLayout titleRow = row();
        titleRow.addView(oneLine(text(title, 17, ink, true)), weighted(0));
        final TextView time = text("", 11, muted, false); titleRow.addView(time);
        words.addView(titleRow);
        LinearLayout previewRow = row();
        final TextView preview = oneLine(text(subtitle, 13, muted, false));
        previewRow.addView(preview, weighted(0));
        TextView badge = text(tag, 10, accentText(color), true); badge.setBackground(shape(tint(color, .16f), 20)); pad(badge, 9, 4, 9, 4);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, -2); badgeParams.setMarginStart(dp(8)); previewRow.addView(badge, badgeParams);
        top(words, previewRow, 5);
        pressable(c);
        c.setOnClickListener(v -> { currentRoomId = roomId; currentRoomTitle = title; page = "Room"; showApp(); });
        // Live preview of the latest message in this room.
        ListenerRegistration reg = db.collection("rooms").document(roomId).collection("messages").orderBy("createdAt", Query.Direction.DESCENDING).limit(1)
            .addSnapshotListener((snap, error) -> {
                if (error != null || snap == null || snap.isEmpty()) return;
                DocumentSnapshot m = snap.getDocuments().get(0);
                String author = m.getString("authorName"), body = m.getString("text");
                String message = (body != null && !body.isEmpty()) ? body.replace('\n', ' ') : (m.getString("mediaType") != null ? "📷 صورة" : "");
                String first = author == null ? "" : author.trim().split("\\s+")[0];
                preview.setText((first.isEmpty() ? "" : first + ": ") + message);
                Timestamp ts = m.getTimestamp("createdAt");
                time.setText(ts == null ? "" : shortTime(ts.toDate()));
            });
        previewListeners.add(reg);
    }

    // ---- Chat room ----
    private boolean roomStickBottom = true;
    private final HashMap<String, View> mediaViews = new HashMap<>();

    private void roomScreen() {
        int idx = currentRoomId.equals("global") ? -1 : (currentRoomId.equals("admins") ? -2 : classIndex(currentRoomId));
        root.setAlpha(0f); root.setTranslationX(-dp(28));
        root.animate().alpha(1f).translationX(0f).setDuration(260).setInterpolator(new DecelerateInterpolator(1.8f)).start();
        knownMessageIds.clear(); mediaViews.clear(); roomFirstLoad = true; roomStickBottom = true;

        LinearLayout header = row(); header.setBackgroundColor(surface); pad(header, 6, 8, 16, 8);
        FrameLayout back = iconBox(R.drawable.ic_back, ink, Color.TRANSPARENT, 24, 22); back.setContentDescription("رجوع");
        back.setOnClickListener(v -> { page = "Chat"; showApp(); });
        header.addView(back, lp(44, 44));
        if (idx >= 0) { LinearLayout.LayoutParams lg = lp(42, 42); lg.setMarginStart(dp(4)); header.addView(logoView(idx, 42), lg); }
        else { LinearLayout.LayoutParams lg = lp(42, 42); lg.setMarginStart(dp(4)); header.addView(iconBox(idx == -2 ? R.drawable.ic_person : R.drawable.ic_chat, accentText(brand), tint(brand, .16f), 22, 14), lg); }
        LinearLayout titles = column(); titles.addView(oneLine(text(currentRoomTitle, 17, ink, true)));
        String roomSubtitle = currentRoomId.equals("global") ? "مفتوحة لجميع الأعضاء" : (currentRoomId.equals("admins") ? "للمشرفين فقط" : "دردشة خاصة بأعضاء هذه الفئة");
        top(titles, oneLine(text(roomSubtitle, 12, muted, false)), 1);
        header.addView(titles, weighted(12));
        root.addView(header, lp(-1, -2));
        View divider = new View(this); divider.setBackgroundColor(line); root.addView(divider, lp(-1, 1));

        FrameLayout body = new FrameLayout(this);
        final ScrollView messagesScroll = new ScrollView(this); messagesScroll.setFillViewport(true); messagesScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        roomScroll = messagesScroll;
        roomMessages = column(); pad(roomMessages, 12, 12, 12, 12); messagesScroll.addView(roomMessages);
        body.addView(messagesScroll, new FrameLayout.LayoutParams(-1, -1));

        newMessagePill = text("رسالة جديدة ↓", 13, onColor(solid()), true); newMessagePill.setBackground(shape(solid(), 22)); pad(newMessagePill, 16, 9, 16, 9);
        newMessagePill.setElevation(dp(6)); newMessagePill.setVisibility(View.GONE);
        FrameLayout.LayoutParams pillParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL); pillParams.bottomMargin = dp(12);
        body.addView(newMessagePill, pillParams);
        newMessagePill.setOnClickListener(v -> { scrollToBottom(messagesScroll, true); hideNewPill(); });
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        messagesScroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> { roomStickBottom = nearBottom(messagesScroll); if (roomStickBottom) hideNewPill(); });
        messagesScroll.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            if ((b - t) < (oldB - oldT) && roomStickBottom) scrollToBottom(messagesScroll, false);
        });
        buildComposer();
        listenMessages(messagesScroll);
    }
    private boolean nearBottom(ScrollView s) {
        View c = s.getChildAt(0); if (c == null) return true;
        return c.getBottom() - (s.getScrollY() + s.getHeight()) < dp(140);
    }
    private void scrollToBottom(final ScrollView s, final boolean smooth) {
        s.post(() -> {
            View c = s.getChildAt(0); if (c == null) return;
            int target = Math.max(0, c.getBottom() - s.getHeight());
            if (smooth) s.smoothScrollTo(0, target); else s.scrollTo(0, target);
        });
    }
    private void showNewPill() {
        if (newMessagePill == null || newMessagePill.getVisibility() == View.VISIBLE) return;
        newMessagePill.setVisibility(View.VISIBLE); pop(newMessagePill);
    }
    private void hideNewPill() { if (newMessagePill != null && newMessagePill.getVisibility() == View.VISIBLE) newMessagePill.setVisibility(View.GONE); }

    private void buildComposer() {
        LinearLayout bar = column(); bar.setBackgroundColor(bg); pad(bar, 10, 6, 10, 10);
        final TextView counter = text("", 11, muted, false); counter.setVisibility(View.GONE); pad(counter, 12, 0, 12, 4); bar.addView(counter, lp(-1, -2));

        final LinearLayout pill = row(); pill.setGravity(Gravity.BOTTOM); pill.setBackground(bordered(surface, 28, line)); pad(pill, 4, 4, 4, 4);
        FrameLayout attach = iconBox(R.drawable.ic_attach, muted, Color.TRANSPARENT, 24, 22); attach.setContentDescription("إرفاق صورة");
        attach.setOnClickListener(v -> pickChatImage());
        pill.addView(attach, lp(44, 44));

        messageInput = new EditText(this);
        messageInput.setBackground(null); messageInput.setHint("اكتب رسالة…"); messageInput.setHintTextColor(muted); messageInput.setTextColor(ink); messageInput.setTextSize(16);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageInput.setSingleLine(false); messageInput.setHorizontallyScrolling(false); messageInput.setMinHeight(dp(44)); messageInput.setMaxLines(5); messageInput.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        messageInput.setPadding(dp(6), dp(10), dp(6), dp(10)); messageInput.setLineSpacing(0, 1.12f);
        messageInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_MESSAGE)});
        messageInput.setOnFocusChangeListener((v, focus) -> pill.setBackground(focus ? bordered(surface, 28, solid(), 2) : bordered(surface, 28, line)));
        pill.addView(messageInput, new LinearLayout.LayoutParams(0, -2, 1f));

        final FrameLayout send = iconBox(R.drawable.ic_send, onColor(solid()), solid(), 22, 22);
        send.getChildAt(0).setScaleX(-1f); send.setContentDescription("إرسال"); send.setAlpha(.4f); send.setScaleX(.9f); send.setScaleY(.9f); send.setEnabled(false);
        pressable(send);
        send.setOnClickListener(v -> { haptic(v); sendMessage(); });
        pill.addView(send, lp(44, 44));
        bar.addView(pill, lp(-1, -2));
        root.addView(bar, lp(-1, -2));

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                boolean has = s.toString().trim().length() > 0;
                if (has != send.isEnabled()) {
                    send.setEnabled(has);
                    send.animate().alpha(has ? 1f : .4f).scaleX(has ? 1f : .9f).scaleY(has ? 1f : .9f).setDuration(160).start();
                }
                if (s.length() >= MAX_MESSAGE - 200) { counter.setVisibility(View.VISIBLE); counter.setText(s.length() + " / " + MAX_MESSAGE); } else counter.setVisibility(View.GONE);
            }
        });
    }

    /** LinearLayout that caps its own width so chat bubbles never span the full screen. */
    private class BubbleLayout extends LinearLayout {
        private final int maxWidth;
        BubbleLayout(int max) { super(MainActivity.this); maxWidth = max; }
        @Override protected void onMeasure(int w, int h) {
            if (View.MeasureSpec.getMode(w) == View.MeasureSpec.UNSPECIFIED || View.MeasureSpec.getSize(w) > maxWidth) w = View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST);
            super.onMeasure(w, h);
        }
    }

    private int dayKey(long millis) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(millis); return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR); }
    private TextView dateChip(long millis) {
        int day = dayKey(millis), today = dayKey(System.currentTimeMillis()), yesterday = dayKey(System.currentTimeMillis() - 86400000L);
        String label = day == today ? "اليوم" : (day == yesterday ? "أمس" : new SimpleDateFormat("d MMMM yyyy", AR).format(new java.util.Date(millis)));
        TextView t = text(label, 11, muted, true); t.setGravity(Gravity.CENTER); t.setBackground(shape(tint(muted, .14f), 12)); pad(t, 12, 4, 12, 4);
        return t;
    }
    private int nameColor(String id) {
        int h = Math.abs((id == null ? 0 : id.hashCode()) % 360);
        return Color.HSVToColor(new float[]{h, dark ? .45f : .70f, dark ? .98f : .62f});
    }

    private View buildBubble(final DocumentSnapshot doc, boolean mine, boolean grouped, int maxBubble, int mineBg, SimpleDateFormat timeFormat) {
        final String body = doc.getString("text");
        String authorId = doc.getString("authorId"), author = doc.getString("authorName");
        BubbleLayout bubble = new BubbleLayout(maxBubble); bubble.setOrientation(LinearLayout.VERTICAL);
        float r = dp(18), s = dp(5), tl = r, tr = r, br = r, bl = r;
        if (mine) { if (grouped) tr = s; br = s; } else { if (grouped) tl = s; bl = s; }
        GradientDrawable bd = new GradientDrawable(); bd.setColor(mine ? mineBg : surface); if (!mine) bd.setStroke(dp(1), line);
        bd.setCornerRadii(new float[]{tl, tl, tr, tr, br, br, bl, bl});
        bubble.setBackground(bd); pad(bubble, 13, 9, 13, 8);
        final View.OnLongClickListener menu = v -> { showMessageMenu(doc, body); return true; };
        bubble.setOnLongClickListener(menu);
        if (!mine && !grouped) bubble.addView(text(author == null ? "عضو" : author, 12, nameColor(authorId), true));
        Blob media = doc.getBlob("mediaData"); String mediaType = doc.getString("mediaType");
        if (media != null && mediaType != null) {
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(maxBubble - dp(26), dp(200)); pp.topMargin = dp((mine || grouped) ? 0 : 6);
            View preview = chatMedia(doc.getId(), media, mediaType); preview.setOnLongClickListener(menu);
            bubble.addView(preview, pp);
        }
        if (body != null && !body.isEmpty()) {
            TextView m = text(body, 15, ink, false);
            m.setSingleLine(false); m.setHorizontallyScrolling(false); m.setMaxLines(100); m.setLineSpacing(dp(2), 1.1f);
            m.setGravity(Gravity.START); m.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
            Linkify.addLinks(m, Linkify.WEB_URLS); m.setLinkTextColor(accentText(accent)); m.setOnLongClickListener(menu);
            LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-2, -2);
            messageParams.topMargin = dp(mine || media != null ? (media != null ? 6 : 0) : 3);
            bubble.addView(m, messageParams);
        }
        Timestamp ts = doc.getTimestamp("createdAt");
        TextView time = text(ts == null ? "…" : timeFormat.format(ts.toDate()), 10, muted, false);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2); tp.topMargin = dp(3); tp.gravity = Gravity.END; bubble.addView(time, tp);
        return bubble;
    }
    /** Decoded images / GIFs are cached per message so new snapshots don't re-decode them. */
    private View chatMedia(String id, Blob media, String type) {
        View cached = mediaViews.get(id);
        if (cached != null) { ViewGroup old = (ViewGroup) cached.getParent(); if (old != null) old.removeView(cached); return cached; }
        View created = blobPreview(media, type, 12, "chat_" + id);
        mediaViews.put(id, created); return created;
    }

    private void listenMessages(final ScrollView messagesScroll) {
        int[] skeletonWidths = {200, 250, 170, 230};
        for (int i = 0; i < skeletonWidths.length; i++) {
            LinearLayout.LayoutParams sp = lp(skeletonWidths[i], i % 2 == 0 ? 46 : 64); sp.topMargin = dp(8); sp.gravity = i % 2 == 0 ? Gravity.END : Gravity.START;
            roomMessages.addView(skeleton(46, 18), sp);
        }
        chatListener = db.collection("rooms").document(currentRoomId).collection("messages").orderBy("createdAt", Query.Direction.DESCENDING).limit(60).addSnapshotListener((snapshot, error) -> {
            if (error != null) { toast("تعذّر تحميل الرسائل."); return; }
            if (snapshot == null || roomMessages == null) return;
            boolean wasNear = roomFirstLoad || nearBottom(messagesScroll);
            roomMessages.removeAllViews();
            if (snapshot.isEmpty()) {
                LinearLayout empty = column(); empty.setGravity(Gravity.CENTER); pad(empty, 20, 60, 20, 20);
                empty.addView(iconBox(R.drawable.ic_chat, accentText(accent), tint(accent, .16f), 28, 30), lp(64, 64));
                TextView t = text(currentRoomId.equals("global") ? "لا توجد رسائل بعد" : (currentRoomId.equals("admins") ? "ابدأ نقاش المشرفين" : "ابدأ الحديث مع فئتك"), 16, ink, true); t.setGravity(Gravity.CENTER); top(empty, t, 14);
                TextView s = text("كن أول من يكتب هنا.", 13, muted, false); s.setGravity(Gravity.CENTER); top(empty, s, 4);
                roomMessages.addView(empty, lp(-1, -2)); fadeUp(empty, 0);
                roomFirstLoad = false; return;
            }
            String myUid = auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid();
            int maxBubble = (int) (getResources().getDisplayMetrics().widthPixels * 0.78f);
            int mineBg = dark ? blend(accent, surface, .32f) : blend(accent, Color.WHITE, .20f);
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", AR);
            ArrayList<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
            Collections.reverse(docs);
            boolean newMine = false, newOther = false;
            int lastDay = -1; String prevAuthor = null; long prevTime = 0;
            for (int i = 0; i < docs.size(); i++) {
                DocumentSnapshot doc = docs.get(i);
                String id = doc.getId(), authorId = doc.getString("authorId");
                Timestamp ts = doc.getTimestamp("createdAt");
                long millis = ts == null ? System.currentTimeMillis() : ts.toDate().getTime();
                boolean mine = myUid.equals(authorId);
                boolean fresh = !roomFirstLoad && !knownMessageIds.contains(id);
                knownMessageIds.add(id);
                if (fresh) { if (mine) newMine = true; else newOther = true; }
                int day = dayKey(millis);
                if (day != lastDay) {
                    LinearLayout.LayoutParams dc = new LinearLayout.LayoutParams(-2, -2); dc.gravity = Gravity.CENTER_HORIZONTAL; dc.topMargin = dp(10); dc.bottomMargin = dp(4);
                    roomMessages.addView(dateChip(millis), dc); lastDay = day; prevAuthor = null;
                }
                boolean grouped = prevAuthor != null && prevAuthor.equals(authorId) && Math.abs(millis - prevTime) < 5 * 60 * 1000L;
                View bubble = buildBubble(doc, mine, grouped, maxBubble, mineBg, timeFormat);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2); bp.topMargin = dp(grouped ? 2 : 8); bp.gravity = mine ? Gravity.START : Gravity.END;
                roomMessages.addView(bubble, bp);
                if (fresh) fadeUp(bubble, 0);
                prevAuthor = authorId; prevTime = millis;
            }
            if (roomFirstLoad) scrollToBottom(messagesScroll, false);
            else if (newMine || (wasNear && newOther)) scrollToBottom(messagesScroll, true);
            else if (newOther) showNewPill();
            roomFirstLoad = false;
        });
    }
    private void showMessageMenu(final DocumentSnapshot doc, final String body) {
        final ArrayList<String> labels = new ArrayList<>(); final ArrayList<Integer> actions = new ArrayList<>();
        if (body != null && !body.isEmpty()) { labels.add("نسخ النص"); actions.add(0); }
        if (isAdmin) { labels.add("حذف الرسالة"); actions.add(1); }
        if (labels.isEmpty()) return;
        if (roomMessages != null) haptic(roomMessages);
        showStyled(new AlertDialog.Builder(this, dialogTheme()).setItems(labels.toArray(new String[0]), (d, which) -> {
            if (actions.get(which) == 0) copyText(body); else confirmDeleteMessage(doc.getReference());
        }));
    }
    private void copyText(String value) {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) { cm.setPrimaryClip(ClipData.newPlainText("message", value)); toast("تم نسخ النص"); }
    }
    private void sendMessage() {
        FirebaseUser account = auth.getCurrentUser(); String body = messageInput == null ? "" : messageInput.getText().toString().trim();
        if (account == null || body.isEmpty()) return;
        if (body.length() > MAX_MESSAGE) { toast("الحد الأقصى للرسالة 1000 حرف."); return; }
        Map<String, Object> message = new HashMap<>();
        message.put("authorId", account.getUid()); message.put("authorName", user); message.put("text", body); message.put("createdAt", FieldValue.serverTimestamp());
        final EditText input = messageInput; input.setText("");
        db.collection("rooms").document(currentRoomId).collection("messages").add(message).addOnFailureListener(error -> { toast("تعذّر إرسال الرسالة."); if (input.getText().length() == 0) input.setText(body); });
    }
    private void pickChatImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("image/*");
        try { startActivityForResult(intent, PICK_CHAT_IMAGE); } catch (Exception error) { toast("تعذّر فتح معرض الصور."); }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ANNOUNCEMENT_IMAGE) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
            Uri imageUri = data.getData(); String imageMime = getContentResolver().getType(imageUri);
            if (imageMime == null || !imageMime.startsWith("image/")) { toast("اختر صورة صالحة."); return; }
            if (announcementImageStatus != null) announcementImageStatus.setText("جارٍ ضغط الصورة…");
            new Thread(() -> {
                try {
                    byte[] source; try (InputStream input = getContentResolver().openInputStream(imageUri)) { if (input == null) throw new IllegalArgumentException("Cannot read image"); source = readLimited(input, MAX_SOURCE_BYTES); }
                    byte[] compressed = compressImage(source);
                    runOnUiThread(() -> { announcementImage = compressed; updateAnnouncementImageUi(); });
                } catch (Exception error) { runOnUiThread(() -> { updateAnnouncementImageUi(); toast("تعذّر قراءة الصورة أو ضغطها إلى أقل من 700 كيلوبايت."); }); }
            }).start();
            return;
        }
        if (requestCode != PICK_CHAT_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData(); String mime = getContentResolver().getType(uri);
        if (mime == null || !mime.startsWith("image/")) { toast("اختر صورة أو GIF."); return; }
        String caption = messageInput == null ? "" : messageInput.getText().toString().trim();
        FirebaseUser account = auth.getCurrentUser(); if (account == null) return;
        final String roomId = currentRoomId; toast("جارٍ تجهيز الصورة…");
        new Thread(() -> {
            try {
                byte[] bytes; String storedType;
                try (InputStream input = getContentResolver().openInputStream(uri)) { if (input == null) throw new IllegalArgumentException("Cannot read image"); bytes = readLimited(input, MAX_SOURCE_BYTES); }
                if ("image/gif".equalsIgnoreCase(mime)) { storedType = "image/gif"; if (bytes.length > MAX_MEDIA_BYTES) throw new IllegalArgumentException("TOO_LARGE"); }
                else { bytes = compressImage(bytes); storedType = "image/jpeg"; }
                if (bytes.length > MAX_MEDIA_BYTES) throw new IllegalArgumentException("TOO_LARGE");
                Map<String, Object> message = new HashMap<>();
                message.put("authorId", account.getUid()); message.put("authorName", user); message.put("text", caption); message.put("mediaData", Blob.fromBytes(bytes)); message.put("mediaType", storedType); message.put("createdAt", FieldValue.serverTimestamp());
                db.collection("rooms").document(roomId).collection("messages").add(message)
                    .addOnSuccessListener(ref -> { if (messageInput != null) messageInput.setText(""); })
                    .addOnFailureListener(error -> toast("تعذّر إرسال الصورة. تحقق من الاتصال."));
            } catch (IllegalArgumentException error) { runOnUiThread(() -> toast("الصورة كبيرة جدًا. اختر صورة أصغر أو GIF أقل من 700 كيلوبايت.")); }
            catch (Exception error) { runOnUiThread(() -> toast("تعذّر قراءة الصورة أو ضغطها.")); }
        }).start();
    }
    private byte[] readLimited(InputStream input, int limit) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int count, total = 0;
        while ((count = input.read(buffer)) != -1) { total += count; if (total > limit) throw new IllegalArgumentException("SOURCE_TOO_LARGE"); output.write(buffer, 0, count); }
        return output.toByteArray();
    }
    private byte[] compressImage(byte[] source) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true; BitmapFactory.decodeByteArray(source, 0, source.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IllegalArgumentException("INVALID_IMAGE");
        int sample = 1; while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > 1600) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize = sample;
        Bitmap bitmap = BitmapFactory.decodeByteArray(source, 0, source.length, options); if (bitmap == null) throw new IllegalArgumentException("INVALID_IMAGE");
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(); int quality = 82;
            do { output.reset(); bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output); quality -= 10; } while (output.size() > MAX_MEDIA_BYTES && quality >= 42);
            if (output.size() > MAX_MEDIA_BYTES) throw new IllegalArgumentException("TOO_LARGE");
            return output.toByteArray();
        } finally { bitmap.recycle(); }
    }
    /** Decodes a downscaled copy (RGB_565 halves memory; stored photos are always JPEG). */
    private Bitmap decodeScaled(byte[] bytes, int maxSide) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true; BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            int sample = 1; while (Math.max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize = sample; options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } catch (Throwable error) { return null; }
    }
    private View mediaPreview(final byte[] bytes, final String type, int radius) {
        View v;
        if ("image/gif".equals(type)) v = new AnimatedGifView(bytes);
        else {
            ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bitmap = decodeScaled(bytes, 900);
            if (bitmap != null) image.setImageBitmap(bitmap); else image.setContentDescription("صورة مرفقة");
            v = image;
        }
        v.setBackground(shape(tint(accent, .10f), radius)); v.setClipToOutline(true);
        v.setOnClickListener(x -> showImageFullscreen(bytes, type));
        return v;
    }
    private class AnimatedGifView extends View {
        private final Movie movie; private final long started = android.os.SystemClock.uptimeMillis();
        AnimatedGifView(byte[] bytes) { super(MainActivity.this); movie = Movie.decodeStream(new ByteArrayInputStream(bytes)); setBackground(shape(tint(accent, .10f), 12)); setContentDescription("صورة متحركة GIF"); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas); if (movie == null) return;
            int duration = movie.duration(); if (duration <= 0) duration = 1000;
            movie.setTime((int) ((android.os.SystemClock.uptimeMillis() - started) % duration));
            float scale = Math.min((float) getWidth() / movie.width(), (float) getHeight() / movie.height());
            float left = (getWidth() - movie.width() * scale) / 2f, top = (getHeight() - movie.height() * scale) / 2f;
            canvas.save(); canvas.translate(left, top); canvas.scale(scale, scale); movie.draw(canvas, 0, 0); canvas.restore(); postInvalidateDelayed(40);
        }
    }

    private void confirmDeleteMessage(DocumentReference ref) {
        showStyled(new AlertDialog.Builder(this, dialogTheme()).setTitle("حذف الرسالة")
            .setMessage("هل تريد حذف هذه الرسالة من الدردشة؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حذف", (d, w) -> ref.delete().addOnFailureListener(error -> toast("تعذّر حذف الرسالة."))));
    }
    private void showImageFullscreen(byte[] bytes, String type) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        final FrameLayout frame = new FrameLayout(this); frame.setBackgroundColor(Color.BLACK);
        View image;
        if ("image/gif".equals(type)) image = new AnimatedGifView(bytes);
        else {
            ImageView photo = new ImageView(this); photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length); if (bitmap != null) photo.setImageBitmap(bitmap);
            image = photo;
        }
        frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
        TextView close = text("×", 32, Color.WHITE, true); close.setGravity(Gravity.CENTER); close.setBackground(shape(Color.argb(150, 0, 0, 0), 30));
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END); cp.setMargins(0, dp(24), dp(16), 0); frame.addView(close, cp);
        close.setOnClickListener(v -> dialog.dismiss());
        frame.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(frame); dialog.setCancelable(true); dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(-1, -1);
        frame.setAlpha(0f); frame.animate().alpha(1f).setDuration(220).start();
    }

    // ---- Announcements ----
    private class AnnouncementDraft {
        String id = "", title = "", subtitle = "", body = "", audience = "all", author = "";
        byte[] image;
        Timestamp createdAt;
    }
    private final java.util.concurrent.ExecutorService imageExecutor = java.util.concurrent.Executors.newFixedThreadPool(2);
    private boolean publishing = false;

    private String newsKey() { FirebaseUser a = auth.getCurrentUser(); return "news_seen_" + (a == null ? "anon" : a.getUid()); }
    private long newsSeenAt() { return prefs.getLong(newsKey(), 0); }
    private boolean isUnseen(DocumentSnapshot doc) { Timestamp ts = doc.getTimestamp("createdAt"); return ts != null && ts.toDate().getTime() > newsSeenAt(); }
    private void markNewsSeen(long newestMillis) {
        if (newestMillis > 0 && newestMillis > newsSeenAt()) prefs.edit().putLong(newsKey(), newestMillis).apply();
        if (newsUnread != 0) { newsUnread = 0; updateNewsDot(); }
    }
    private Query announcementsQuery() {
        Query q = db.collection("announcements");
        if (isAdmin) return q.orderBy("createdAt", Query.Direction.DESCENDING).limit(100);
        return q.whereIn("audience", Arrays.asList("all", scoutClassId));
    }
    private ArrayList<DocumentSnapshot> sortedByDate(QuerySnapshot snapshot) {
        ArrayList<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
        Collections.sort(docs, (a, b) -> { Timestamp ta = a.getTimestamp("createdAt"), tb = b.getTimestamp("createdAt"); if (ta == null) return tb == null ? 0 : -1; if (tb == null) return 1; return tb.compareTo(ta); });
        return docs;
    }
    /** Watches announcements in the background so the bottom bar can show a "new" dot and Home can show the latest one. */
    private void startNewsWatcher() {
        if (newsWatcher != null) { newsWatcher.remove(); newsWatcher = null; }
        FirebaseUser account = auth.getCurrentUser();
        if (account == null || !(isAdmin || "approved".equals(approvalStatus))) return;
        newsWatcher = announcementsQuery().addSnapshotListener((snap, error) -> {
            if (error != null || snap == null) return;
            ArrayList<DocumentSnapshot> docs = sortedByDate(snap);
            long newest = 0;
            for (DocumentSnapshot d : docs) { Timestamp ts = d.getTimestamp("createdAt"); if (ts != null) newest = Math.max(newest, ts.toDate().getTime()); }
            String key = newsKey();
            if (!prefs.contains(key)) prefs.edit().putLong(key, Math.max(newest, 1L)).apply();
            long seen = prefs.getLong(key, 0);
            int unread = 0;
            for (DocumentSnapshot d : docs) { Timestamp ts = d.getTimestamp("createdAt"); if (ts != null && ts.toDate().getTime() > seen) unread++; }
            latestNews = docs.isEmpty() ? null : docs.get(0);
            newsUnread = page.equals("News") ? 0 : unread;
            updateNewsDot();
            if (page.equals("Home")) renderLatestNews();
        });
    }

    private void newsPage() {
        newsSeenBefore = prefs.contains(newsKey()) ? newsSeenAt() : Long.MAX_VALUE;
        pageHeader("الإعلانات", "آخر أخبار فرع السلام الكشفي.");
        if (isAdmin) {
            TextView b = button("+  إعلان جديد", solid(), onColor(solid())); top(content, b, 18);
            b.setOnClickListener(v -> { AnnouncementDraft d = new AnnouncementDraft(); announcementImage = null; showAnnouncementDialog(d); });
        }
        LinearLayout feed = column(); top(content, feed, 14);
        skeletonList(feed, 3, 200);
        listenAnnouncements(feed);
    }
    private int audienceColor(String audience) { for (int i = 0; i < classIds.length; i++) if (classIds[i].equals(audience)) return classColors[i]; return brand; }
    private String audienceName(String audience) {
        if ("all".equals(audience)) return "الجميع";
        for (int i = 0; i < classIds.length; i++) if (classIds[i].equals(audience)) return classArabic[i];
        return "إعلان قديم";
    }
    private String shortDate(Timestamp ts) { return new SimpleDateFormat("d MMM yyyy", AR).format(ts.toDate()); }
    private String longDate(Timestamp ts) { return new SimpleDateFormat("d MMM yyyy · h:mm a", AR).format(ts.toDate()); }

    private void listenAnnouncements(final LinearLayout feed) {
        final boolean[] first = {true};
        announcementListener = announcementsQuery().addSnapshotListener((snapshot, error) -> {
            if (error != null) { feed.removeAllViews(); toast("تعذّر تحميل الإعلانات."); return; }
            if (snapshot == null) return;
            feed.removeAllViews();
            ArrayList<DocumentSnapshot> docs = sortedByDate(snapshot);
            int shown = 0; long newest = 0;
            for (DocumentSnapshot doc : docs) {
                String audience = doc.getString("audience");
                if (!isAdmin && !("all".equals(audience) || scoutClassId.equals(audience))) continue;
                Timestamp created = doc.getTimestamp("createdAt");
                if (created != null) newest = Math.max(newest, created.toDate().getTime());
                boolean isNew = created != null && created.toDate().getTime() > newsSeenBefore;
                LinearLayout.LayoutParams cp = lp(-1, -2); cp.topMargin = dp(shown == 0 ? 0 : 14);
                feed.addView(newsCard(doc, isNew), cp);
                shown++;
            }
            if (shown == 0) emptyStateIn(feed, R.drawable.ic_news, "لا توجد إعلانات بعد", isAdmin ? "ستظهر إعلانات فرع السلام هنا." : "ستظهر هنا إعلانات الفرع والإعلانات الموجهة لفئتك.");
            else if (first[0]) stagger(feed, 6);
            first[0] = false;
            markNewsSeen(newest);
        });
    }

    /** Image that decodes on a background thread so long feeds never freeze the screen. */
    private View blobPreview(final Blob blob, final String type, int radius, final String cacheKey) {
        if ("image/gif".equals(type)) return mediaPreview(blob.toBytes(), type, radius);
        final ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(shape(tint(accent, .10f), radius)); image.setClipToOutline(true);
        Bitmap cached = bitmapCache.get(cacheKey);
        if (cached != null) image.setImageBitmap(cached);
        else {
            try {
                imageExecutor.execute(() -> {
                    final Bitmap bmp = decodeScaled(blob.toBytes(), 800);
                    if (bmp != null) bitmapCache.put(cacheKey, bmp);
                    runOnUiThread(() -> { if (bmp != null) { image.setImageBitmap(bmp); image.setAlpha(0f); image.animate().alpha(1f).setDuration(220).start(); } });
                });
            } catch (Exception ignored) { }
        }
        image.setOnClickListener(x -> showImageFullscreen(blob.toBytes(), type));
        return image;
    }

    private View newsCard(final DocumentSnapshot doc, boolean isNew) {
        String audience = doc.getString("audience"); int color = audienceColor(audience);
        LinearLayout c = column(); c.setBackground(bordered(surface, 22, line)); c.setClipToOutline(true); elev(c);
        Blob blob = doc.getBlob("imageData"); String type = doc.getString("imageType");
        if (blob != null && type != null) c.addView(blobPreview(blob, type, 0, "ann_" + doc.getId()), lp(-1, 170));
        LinearLayout inner = column(); pad(inner, 16, 14, 16, 14); c.addView(inner, lp(-1, -2));
        LinearLayout meta = row();
        TextView badge = text(audienceName(audience), 11, accentText(color), true); badge.setBackground(shape(tint(color, .16f), 20)); pad(badge, 10, 5, 10, 5); meta.addView(badge);
        if (isNew) {
            TextView chip = text("جديد", 11, Color.WHITE, true); chip.setBackground(shape(danger, 20)); pad(chip, 9, 5, 9, 5);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-2, -2); chipParams.setMarginStart(dp(6)); meta.addView(chip, chipParams);
        }
        meta.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        Timestamp created = doc.getTimestamp("createdAt");
        if (created != null) meta.addView(text(shortDate(created), 11, muted, false));
        inner.addView(meta);
        top(inner, text(doc.getString("title") == null ? "إعلان" : doc.getString("title"), 19, ink, true), 10);
        String subtitle = doc.getString("subtitle");
        if (subtitle != null && !subtitle.isEmpty()) top(inner, text(subtitle, 14, accentText(accent), true), 4);
        String full = doc.getString("body");
        if (full != null && !full.isEmpty()) { TextView preview = text(full, 14, muted, false); preview.setMaxLines(2); preview.setEllipsize(TextUtils.TruncateAt.END); top(inner, preview, 6); }
        LinearLayout more = row(); more.addView(text("اقرأ المزيد", 13, accentText(accent), true));
        more.addView(icon(R.drawable.ic_chevron, accentText(accent)), lp(18, 18));
        top(inner, more, 10);
        rippleOver(c, 22); pressable(c);
        c.setOnClickListener(v -> showAnnouncementDetails(doc));
        return c;
    }

    /** Shared layout for the details sheet and the pre-publish preview. */
    private LinearLayout announcementView(String audience, Blob blob, byte[] draftImage, String imageType, String cacheKey, String title, String subtitle, String bodyText, String metaText, final Runnable onClose) {
        LinearLayout v = column(); v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); pad(v, 20, 6, 20, 22);
        int color = audienceColor(audience);
        LinearLayout topRow = row();
        TextView badge = text(audienceName(audience), 12, accentText(color), true); badge.setBackground(shape(tint(color, .16f), 20)); pad(badge, 12, 6, 12, 6);
        topRow.addView(badge);
        topRow.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        TextView close = text("×", 26, muted, false); close.setGravity(Gravity.CENTER); close.setBackground(ripple(shape(tint(muted, .14f), 20)));
        close.setContentDescription("إغلاق"); close.setOnClickListener(x -> onClose.run());
        topRow.addView(close, lp(40, 40));
        v.addView(topRow);
        if (blob != null && imageType != null) top(v, blobPreview(blob, imageType, 18, cacheKey), 12);
        else if (draftImage != null) top(v, mediaPreview(draftImage, "image/jpeg", 18), 12);
        if (v.getChildCount() > 1) v.getChildAt(1).getLayoutParams().height = dp(220);
        top(v, text(title, 24, ink, true), 16);
        if (subtitle != null && !subtitle.isEmpty()) top(v, text(subtitle, 17, accentText(accent), true), 8);
        TextView bodyView = text(bodyText == null ? "" : bodyText, 16, ink, false); bodyView.setLineSpacing(dp(4), 1.12f);
        Linkify.addLinks(bodyView, Linkify.WEB_URLS); bodyView.setLinkTextColor(accentText(accent));
        top(v, bodyView, 14);
        if (metaText != null && !metaText.isEmpty()) top(v, text(metaText, 12, muted, false), 18);
        return v;
    }
    private void showAnnouncementDetails(final DocumentSnapshot doc) {
        final Sheet[] sheet = new Sheet[1];
        final String title = doc.getString("title") == null ? "إعلان" : doc.getString("title");
        final String subtitle = doc.getString("subtitle") == null ? "" : doc.getString("subtitle");
        final String bodyText = doc.getString("body") == null ? "" : doc.getString("body");
        Timestamp created = doc.getTimestamp("createdAt"); String author = doc.getString("createdByName");
        String meta = (created == null ? "" : longDate(created)) + (author == null ? "" : (created == null ? "" : " · ") + "بواسطة " + author);
        LinearLayout view = announcementView(doc.getString("audience"), doc.getBlob("imageData"), null, doc.getString("imageType"), "ann_" + doc.getId(), title, subtitle, bodyText, meta, () -> sheet[0].close());
        LinearLayout actions = row(); top(view, actions, 20);
        TextView share = button("مشاركة", tint(accent, .16f), accentText(accent)); actions.addView(share, new LinearLayout.LayoutParams(0, -2, 1f));
        share.setOnClickListener(v -> shareAnnouncement(title, subtitle, bodyText));
        if (isAdmin) {
            TextView edit = button("تعديل", solid(), onColor(solid())); actions.addView(edit, weighted(8));
            TextView delete = button("حذف", tint(danger, .16f), danger); actions.addView(delete, weighted(8));
            edit.setOnClickListener(v -> { sheet[0].close(); editAnnouncement(doc); });
            delete.setOnClickListener(v -> { sheet[0].close(); confirmDeleteAnnouncement(doc.getId()); });
        }
        sheet[0] = new Sheet(view); sheet[0].show();
    }
    private void shareAnnouncement(String title, String subtitle, String body) {
        String value = title + (subtitle.isEmpty() ? "" : "\n" + subtitle) + "\n\n" + body + "\n\n— " + organizationArabic + " · " + branchArabic;
        Intent send = new Intent(Intent.ACTION_SEND); send.setType("text/plain"); send.putExtra(Intent.EXTRA_TEXT, value);
        try { startActivity(Intent.createChooser(send, "مشاركة الإعلان")); } catch (Exception error) { toast("تعذّرت المشاركة."); }
    }
    private void editAnnouncement(DocumentSnapshot doc) {
        AnnouncementDraft d = new AnnouncementDraft(); d.id = doc.getId();
        d.title = doc.getString("title") == null ? "" : doc.getString("title"); d.subtitle = doc.getString("subtitle") == null ? "" : doc.getString("subtitle");
        d.body = doc.getString("body") == null ? "" : doc.getString("body");
        d.audience = doc.getString("audience") == null ? "all" : doc.getString("audience"); d.author = doc.getString("createdByName"); d.createdAt = doc.getTimestamp("createdAt");
        Blob image = doc.getBlob("imageData"); d.image = image == null ? null : image.toBytes(); announcementImage = d.image; showAnnouncementDialog(d);
    }
    private void updateAnnouncementImageUi() {
        if (announcementImageStatus != null) announcementImageStatus.setText(announcementImage == null ? "لا توجد صورة مرفقة" : "تمت إضافة الصورة وضغطها");
        if (announcementThumb != null) {
            if (announcementImage == null) announcementThumb.setVisibility(View.GONE);
            else {
                Bitmap bitmap = decodeScaled(announcementImage, 600);
                if (bitmap != null) { announcementThumb.setImageBitmap(bitmap); announcementThumb.setVisibility(View.VISIBLE); }
            }
        }
    }
    private void showAnnouncementDialog(final AnnouncementDraft draft) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = column(); form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); pad(form, 18, 8, 18, 8); scroll.addView(form);
        final EditText title = field("العنوان الرئيسي"); title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)}); title.setText(draft.title); form.addView(box(title, null));
        final EditText subtitle = field("العنوان الفرعي"); subtitle.setFilters(new InputFilter[]{new InputFilter.LengthFilter(180)}); subtitle.setText(draft.subtitle); top(form, box(subtitle, null), 9);
        final EditText body = new EditText(this); body.setHint("اكتب نص الإعلان…"); body.setHintTextColor(muted); body.setTextColor(ink); body.setTextSize(16);
        body.setBackground(bordered(surface, 14, line)); body.setPadding(dp(16), dp(14), dp(16), dp(14)); body.setMinLines(4); body.setGravity(Gravity.TOP | Gravity.START);
        body.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); body.setSingleLine(false);
        body.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4000)}); body.setText(draft.body); top(form, body, 10);
        top(form, text("الصورة (اختياري)", 13, muted, true), 16);
        announcementThumb = new ImageView(this); announcementThumb.setScaleType(ImageView.ScaleType.CENTER_CROP); announcementThumb.setBackground(shape(tint(accent, .10f), 14)); announcementThumb.setClipToOutline(true);
        LinearLayout.LayoutParams thumbParams = lp(-1, 140); thumbParams.topMargin = dp(8); form.addView(announcementThumb, thumbParams);
        LinearLayout imageActions = row(); top(form, imageActions, 10);
        TextView pick = button("إضافة صورة", solid(), onColor(solid())); imageActions.addView(pick, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView clear = button("إزالة الصورة", tint(muted, .14f), ink); imageActions.addView(clear, weighted(8));
        announcementImageStatus = text("", 12, muted, false); top(form, announcementImageStatus, 6);
        updateAnnouncementImageUi();
        pick.setOnClickListener(v -> pickAnnouncementImage());
        clear.setOnClickListener(v -> { announcementImage = null; draft.image = null; updateAnnouncementImageUi(); });

        top(form, text("الجمهور المستهدف", 13, muted, true), 16);
        final String[] options = new String[1 + classIds.length], audiences = new String[1 + classIds.length]; options[0] = "الجميع"; audiences[0] = "all";
        for (int i = 0; i < classIds.length; i++) { options[i + 1] = classArabic[i]; audiences[i + 1] = classId(i); }
        final int[] selected = {0}; for (int i = 0; i < audiences.length; i++) if (audiences[i].equals(draft.audience)) selected[0] = i;
        top(form, chips(options, selected), 8);

        final AlertDialog dialog = new AlertDialog.Builder(this, dialogTheme()).setTitle(draft.id.isEmpty() ? "إعلان جديد" : "تعديل الإعلان").setView(scroll).setNegativeButton("إلغاء", null).setPositiveButton("معاينة", null).create();
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            draft.title = title.getText().toString().trim(); draft.subtitle = subtitle.getText().toString().trim(); draft.body = body.getText().toString().trim(); draft.audience = audiences[selected[0]]; draft.image = announcementImage;
            if (draft.title.isEmpty() || draft.body.isEmpty()) { toast("أضف العنوان الرئيسي ونص الإعلان."); return; }
            hideKeyboard(); dialog.dismiss(); showAnnouncementPreview(draft);
        }));
        dialog.show(); styleDialog(dialog);
    }
    private void pickAnnouncementImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("image/*");
        try { startActivityForResult(intent, PICK_ANNOUNCEMENT_IMAGE); } catch (Exception error) { toast("تعذّر فتح معرض الصور."); }
    }
    private void showAnnouncementPreview(final AnnouncementDraft draft) {
        final Sheet[] sheet = new Sheet[1];
        LinearLayout view = announcementView(draft.audience, null, draft.image, "image/jpeg", "draft", draft.title, draft.subtitle, draft.body, "معاينة · ستظهر هكذا للأعضاء", () -> sheet[0].close());
        LinearLayout actions = row(); top(view, actions, 20);
        TextView backToEdit = button("رجوع للتعديل", tint(accent, .16f), accentText(accent)); actions.addView(backToEdit, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView publish = button(draft.id.isEmpty() ? "نشر" : "حفظ التعديل", solid(), onColor(solid())); actions.addView(publish, weighted(8));
        backToEdit.setOnClickListener(v -> { sheet[0].close(); showAnnouncementDialog(draft); });
        publish.setOnClickListener(v -> { if (publishing) return; sheet[0].close(); postAnnouncement(draft); });
        sheet[0] = new Sheet(view); sheet[0].show();
    }
    private void postAnnouncement(final AnnouncementDraft draft) {
        FirebaseUser account = auth.getCurrentUser(); if (account == null || !isAdmin) { toast("النشر متاح للمشرفين فقط."); return; }
        if (publishing) return;
        Map<String, Object> a = new HashMap<>(); a.put("title", draft.title); a.put("subtitle", draft.subtitle); a.put("body", draft.body); a.put("audience", draft.audience);
        if (draft.image != null) { a.put("imageData", Blob.fromBytes(draft.image)); a.put("imageType", "image/jpeg"); }
        else { a.put("imageData", FieldValue.delete()); a.put("imageType", FieldValue.delete()); }
        DocumentReference ref;
        if (draft.id.isEmpty()) { a.put("createdBy", account.getUid()); a.put("createdByName", user); a.put("createdAt", FieldValue.serverTimestamp()); ref = db.collection("announcements").document(); }
        else ref = db.collection("announcements").document(draft.id);
        publishing = true; toast(draft.id.isEmpty() ? "جارٍ نشر الإعلان…" : "جارٍ حفظ التعديل…");
        ref.set(a, SetOptions.merge()).addOnCompleteListener(task -> {
            publishing = false;
            if (task.isSuccessful()) { bitmapCache.remove("ann_" + ref.getId()); toast(draft.id.isEmpty() ? "تم نشر الإعلان." : "تم حفظ التعديل."); }
            else toast("تعذّر حفظ الإعلان. تأكد من نشر قواعد Firestore.");
        });
    }
    private void confirmDeleteAnnouncement(final String id) {
        showStyled(new AlertDialog.Builder(this, dialogTheme()).setTitle("حذف الإعلان؟").setMessage("سيختفي الإعلان من قائمة الجميع.")
            .setNegativeButton("إلغاء", null).setPositiveButton("حذف", (d, w) -> db.collection("announcements").document(id).delete()
                .addOnSuccessListener(unused -> toast("تم حذف الإعلان."))
                .addOnFailureListener(error -> toast("تعذّر حذف الإعلان."))));
    }
    private void emptyStateIn(LinearLayout parent, int iconRes, String title, String subtitle) {
        LinearLayout c = card(22); c.setGravity(Gravity.CENTER); pad(c, 24, 34, 24, 34);
        c.addView(iconBox(iconRes, accentText(accent), tint(accent, .16f), 28, 30), lp(64, 64));
        TextView h = text(title, 18, ink, true); h.setGravity(Gravity.CENTER); top(c, h, 16);
        TextView sub = text(subtitle, 14, muted, false); sub.setGravity(Gravity.CENTER); top(c, sub, 6);
        LinearLayout.LayoutParams p = lp(-1, -2); p.topMargin = dp(12); parent.addView(c, p);
        fadeUp(c, 0);
    }

    // ---- Profile ----
    private void profilePage() {
        int idx = classIndex(scoutClassId);
        pageHeader("ملفي الشخصي", organizationArabic + " · " + branchArabic);
        FrameLayout banner = new FrameLayout(this);
        banner.setBackground(gradient(heroBg, blend(vivid(classColors[idx]), heroBg, .42f), 26)); banner.setClipToOutline(true);
        LinearLayout inner = column(); inner.setGravity(Gravity.CENTER_HORIZONTAL); pad(inner, 20, 24, 20, 22);
        inner.addView(logoView(idx, 84));
        TextView name = text(user, 22, Color.WHITE, true); name.setGravity(Gravity.CENTER); top(inner, name, 14);
        TextView cls = text(classArabic[idx], 14, heroSub, true); cls.setGravity(Gravity.CENTER); top(inner, cls, 4);
        TextView rankChip = text(rankLabel(rank), 12, onColor(vivid(classColors[idx])), true); rankChip.setBackground(shape(vivid(classColors[idx]), 20)); pad(rankChip, 14, 5, 14, 5);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-2, -2); rp.topMargin = dp(12); rp.gravity = Gravity.CENTER_HORIZONTAL; inner.addView(rankChip, rp);
        banner.addView(inner, new FrameLayout.LayoutParams(-1, -2)); top(content, banner, 20);
        LinearLayout c = card(22); pad(c, 18, 4, 18, 8); top(content, c, 14);
        infoRow(c, "الفئة العمرية", classAges[idx], false);
        infoRow(c, "البريد الإلكتروني", emailAddress == null ? "" : emailAddress, true);
        infoRow(c, "الجنس", genderLabel(gender), true);
        infoRow(c, "الرتبة", rankLabel(rank), true);
        TextView switcher = button("تبديل الحساب", solid(), onColor(solid())); top(content, switcher, 16); switcher.setOnClickListener(v -> switchAccount());
        TextView out = text("تسجيل الخروج", 16, danger, true);
        out.setGravity(Gravity.CENTER); out.setBackground(ripple(bordered(surface, 16, line))); pad(out, 16, 16, 16, 16); top(content, out, 12); pressable(out);
        out.setOnClickListener(v -> confirmSignOut());
    }
    private void confirmSignOut() {
        showStyled(new AlertDialog.Builder(this, dialogTheme()).setTitle("تسجيل الخروج؟").setMessage("ستحتاج إلى تسجيل الدخول مرة أخرى للوصول إلى حسابك.")
            .setNegativeButton("إلغاء", null).setPositiveButton("خروج", (d, w) -> switchAccount()));
    }
    private void infoRow(LinearLayout parent, String label, String value, boolean divider) {
        if (divider) { View d = new View(this); d.setBackgroundColor(line); LinearLayout.LayoutParams dv = lp(-1, 1); dv.topMargin = dp(12); parent.addView(d, dv); }
        top(parent, text(label, 12, muted, true), divider ? 10 : 14);
        TextView v = text(value, 15, ink, false); v.setPadding(0, 0, 0, dp(6)); top(parent, v, 3);
    }

    // ---- Settings (English) ----
    private void settingsPage() {
        pageHeader("Settings", "Choose how the app looks on your device.");
        sectionLabel("Appearance");
        String mode = prefs.getString("theme", "system");
        themeOption("system", mode, "System default", "Match your phone's light or dark setting");
        themeOption("light", mode, "Light", "Bright and clean");
        themeOption("dark", mode, "Dark", "Easier on the eyes at night");
        sectionLabel("Social media");
        TextView facebook = button("Facebook فرع السلام", solid(), onColor(solid())); top(content, facebook, 10);
        facebook.setOnClickListener(v -> openSocial("https://www.facebook.com/share/1J2UtRoCbt/"));
        sectionLabel("Account");
        TextView switcher = button("تبديل الحساب", tint(accent, .16f), accentText(accent)); top(content, switcher, 10); switcher.setOnClickListener(v -> switchAccount());
        TextView forget = button("Clear saved accounts", tint(muted, .14f), ink); top(content, forget, 10);
        forget.setOnClickListener(v -> showStyled(new AlertDialog.Builder(this, dialogTheme()).setTitle("Clear saved accounts?")
            .setMessage("Saved email addresses on this device will be removed. Your account is not affected.")
            .setNegativeButton("Cancel", null).setPositiveButton("Clear", (d, w) -> { prefs.edit().remove("saved_accounts").remove("last_email").apply(); toast("Saved accounts cleared."); })));
        sectionLabel("About");
        LinearLayout about = card(20); pad(about, 18, 16, 18, 16); top(content, about, 12);
        about.addView(text(organizationArabic + " – " + branchArabic, 16, ink, true));
        String version = "1.0";
        try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) { }
        top(about, text("Version " + version, 13, muted, false), 6);
        TextView out = text("Sign out", 16, danger, true);
        out.setGravity(Gravity.CENTER); out.setBackground(ripple(bordered(surface, 16, line))); pad(out, 16, 16, 16, 16); top(content, out, 24); pressable(out);
        out.setOnClickListener(v -> confirmSignOut());
    }
    private void openSocial(String url) { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception error) { toast("تعذّر فتح الرابط."); } }
    private void themeOption(final String value, String current, String title, String subtitle) {
        boolean sel = value.equals(current);
        LinearLayout c = row(); pad(c, 16, 14, 16, 14); top(content, c, 10);
        c.setBackground(ripple(sel ? bordered(tint(accent, .12f), 18, solid(), 2) : bordered(surface, 18, line)));
        LinearLayout words = column(); words.addView(text(title, 16, ink, true)); top(words, text(subtitle, 13, muted, false), 3);
        c.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        FrameLayout mark = iconBox(R.drawable.ic_check, onColor(solid()), solid(), 14, 12); mark.setVisibility(sel ? View.VISIBLE : View.INVISIBLE);
        c.addView(mark, lp(24, 24));
        pressable(c);
        c.setOnClickListener(v -> { prefs.edit().putString("theme", value).apply(); applyTheme(); if (auth.getCurrentUser() == null || user.isEmpty()) accent = defaultAccent(); showApp(); });
    }

    // ---- Account approval ----
    /** While waiting for approval, react immediately when an admin changes the account status. */
    private void watchOwnProfile() {
        FirebaseUser account = auth.getCurrentUser(); if (account == null) return;
        chatListener = db.collection("users").document(account.getUid()).addSnapshotListener((snap, error) -> {
            if (error != null || snap == null || !snap.exists()) return;
            String status = snap.getString("approvalStatus");
            if (status != null && !status.equals(approvalStatus)) loadProfile();
        });
    }
    private void pendingApprovalPage() {
        pageHeader("طلبك قيد المراجعة", organizationArabic + " · " + branchArabic);
        LinearLayout card = card(24); card.setGravity(Gravity.CENTER_HORIZONTAL); pad(card, 20, 26, 20, 22); top(content, card, 22);
        FrameLayout badge = iconBox(R.drawable.ic_person, accentText(accent), tint(accent, .16f), 30, 34);
        card.addView(badge, lp(68, 68)); pulse(badge);
        TextView thanks = text("شكرًا لانضمامك", 20, ink, true); thanks.setGravity(Gravity.CENTER); top(card, thanks, 16);
        TextView info = text("سيتم تفعيل حسابك بعد موافقة إدارة الفرع. ستتحدّث هذه الصفحة تلقائيًا فور الموافقة.", 15, muted, false); info.setGravity(Gravity.CENTER); top(card, info, 8);
        final TextView refresh = button("تحقق من الحالة", solid(), onColor(solid())); top(content, refresh, 16);
        refresh.setOnClickListener(v -> { refresh.setText("جارٍ التحقق…"); refresh.setAlpha(.7f); loadProfile(); });
        TextView logout = button("تسجيل الخروج", surface, ink); logout.setBackground(ripple(bordered(surface, 16, line))); top(content, logout, 10); logout.setOnClickListener(v -> switchAccount());
        watchOwnProfile();
    }
    private void rejectedApprovalPage() {
        pageHeader("تعذّر قبول الطلب", organizationArabic + " · " + branchArabic);
        LinearLayout c = card(22); pad(c, 20, 18, 20, 18); top(content, c, 20);
        c.addView(text("سبب الرفض", 16, ink, true));
        top(c, text(rejectionReason.isEmpty() ? "لم يذكر المشرف سببًا." : rejectionReason, 15, muted, false), 8);
        final EditText appeal = new EditText(this); appeal.setHint("اكتب توضيحًا أو اطلب إعادة مراجعة الطلب"); appeal.setHintTextColor(muted); appeal.setTextColor(ink); appeal.setTextSize(15);
        appeal.setMinLines(3); appeal.setGravity(Gravity.TOP | Gravity.START); appeal.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); appeal.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1000)});
        if (!appealMessage.isEmpty()) appeal.setText(appealMessage);
        top(content, box(appeal, null), 16);
        final TextView send = button("إرسال طلب مراجعة", solid(), onColor(solid())); top(content, send, 10);
        send.setOnClickListener(v -> {
            String value = appeal.getText().toString().trim(); if (value.isEmpty()) { toast("اكتب رسالة المراجعة أولًا."); return; }
            if (!send.isEnabled()) return;
            send.setEnabled(false); send.setAlpha(.7f); hideKeyboard();
            db.collection("users").document(auth.getCurrentUser().getUid()).update("appealMessage", value, "appealAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(unused -> { appealMessage = value; send.setEnabled(true); send.setAlpha(1f); toast("تم إرسال طلب المراجعة إلى الإدارة."); })
                .addOnFailureListener(error -> { send.setEnabled(true); send.setAlpha(1f); toast("تعذّر إرسال الطلب. تحقق من الاتصال."); });
        });
        TextView refresh = text("تحقق من حالة الطلب", 14, accentText(accent), true); refresh.setGravity(Gravity.CENTER); pad(refresh, 8, 10, 8, 10); top(content, refresh, 14); refresh.setOnClickListener(v -> loadProfile());
        TextView logout = text("تسجيل الخروج", 14, muted, true); logout.setGravity(Gravity.CENTER); pad(logout, 8, 10, 8, 10); top(content, logout, 4); logout.setOnClickListener(v -> switchAccount());
        watchOwnProfile();
    }
    private void approvalsPage() {
        pageHeader("طلبات الانضمام", "راجع الطلبات الجديدة وطلبات إعادة المراجعة.");
        final LinearLayout list = column(); top(content, list, 14);
        skeletonList(list, 3, 150);
        db.collection("users").whereIn("approvalStatus", Arrays.asList("pending", "rejected")).get()
            .addOnSuccessListener(snapshot -> {
                list.removeAllViews();
                if (snapshot.isEmpty()) { emptyStateIn(list, R.drawable.ic_person, "لا توجد طلبات معلّقة", "ستظهر طلبات الأعضاء هنا."); return; }
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    final LinearLayout c = card(20); pad(c, 16, 15, 16, 15); top(list, c, 10);
                    String applicant = doc.getString("name") == null ? "عضو" : doc.getString("name");
                    String cls = doc.getString("scoutClass");
                    LinearLayout head = row();
                    TextView avatar = text(initial(applicant), 16, accentText(audienceColor(cls)), true); avatar.setGravity(Gravity.CENTER); avatar.setBackground(shape(tint(audienceColor(cls), .18f), 22));
                    head.addView(avatar, lp(44, 44));
                    LinearLayout who = column(); who.addView(text(applicant, 17, ink, true));
                    top(who, text(doc.getString("email") == null ? "" : doc.getString("email"), 13, muted, false), 2);
                    head.addView(who, weighted(12)); c.addView(head);
                    boolean wasRejected = "rejected".equals(doc.getString("approvalStatus"));
                    top(c, text("الفئة: " + audienceName(cls) + " · الحالة: " + (wasRejected ? "مرفوض" : "قيد المراجعة"), 13, accentText(audienceColor(cls)), true), 10);
                    String reason = doc.getString("rejectionReason"), appeal = doc.getString("appealMessage");
                    if (reason != null && !reason.isEmpty()) top(c, text("السبب: " + reason, 13, muted, false), 5);
                    if (appeal != null && !appeal.isEmpty()) top(c, text("طلب المراجعة: " + appeal, 13, muted, false), 5);
                    LinearLayout actions = row(); top(c, actions, 12);
                    final TextView accept = button("قبول", green, Color.WHITE); actions.addView(accept, new LinearLayout.LayoutParams(0, -2, 1f));
                    final TextView refuse = button("رفض", Color.rgb(190, 55, 55), Color.WHITE); actions.addView(refuse, weighted(8));
                    final String uid = doc.getId();
                    accept.setOnClickListener(v -> updateApplicant(uid, "approved", "", c, list, accept, refuse));
                    refuse.setOnClickListener(v -> askRejectionReason(uid, c, list, accept, refuse));
                }
                stagger(list, 8);
            }).addOnFailureListener(error -> { list.removeAllViews(); toast("تعذّر تحميل الطلبات. تحقق من قواعد Firestore."); });
    }
    private void askRejectionReason(final String uid, final View card, final LinearLayout list, final TextView accept, final TextView refuse) {
        final EditText reason = field("سبب الرفض"); reason.setSingleLine(false); reason.setMinLines(2); reason.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
        showStyled(new AlertDialog.Builder(this, dialogTheme()).setTitle("رفض طلب العضوية").setView(box(reason, null)).setNegativeButton("إلغاء", null)
            .setPositiveButton("رفض", (d, w) -> { String value = reason.getText().toString().trim(); if (value.isEmpty()) { toast("اكتب سبب الرفض ليظهر لصاحب الطلب."); return; } updateApplicant(uid, "rejected", value, card, list, accept, refuse); }));
    }
    private void updateApplicant(String uid, String status, String reason, final View card, final LinearLayout list, final TextView accept, final TextView refuse) {
        accept.setEnabled(false); refuse.setEnabled(false); card.setAlpha(.6f);
        Map<String, Object> change = new HashMap<>(); change.put("approvalStatus", status); change.put("rejectionReason", reason); change.put("reviewedAt", FieldValue.serverTimestamp()); change.put("reviewedBy", auth.getCurrentUser().getUid());
        db.collection("users").document(uid).update(change).addOnSuccessListener(unused -> {
            toast("تم تحديث حالة الطلب.");
            card.animate().alpha(0f).translationX(-dp(60)).setDuration(240).withEndAction(() -> {
                list.removeView(card);
                if (list.getChildCount() == 0) emptyStateIn(list, R.drawable.ic_person, "لا توجد طلبات معلّقة", "ستظهر طلبات الأعضاء هنا.");
            }).start();
        }).addOnFailureListener(error -> {
            accept.setEnabled(true); refuse.setEnabled(true); card.setAlpha(1f);
            toast("تعذّر تحديث الطلب. تحقق من قواعد Firestore.");
        });
    }
}
