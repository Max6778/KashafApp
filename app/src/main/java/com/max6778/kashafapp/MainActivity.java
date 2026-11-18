package com.max6778.kashafapp;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_CHAT_IMAGE = 7104;
    private static final int MAX_MEDIA_BYTES = 700 * 1024;
    private static final int MAX_SOURCE_BYTES = 12 * 1024 * 1024;
    private static final int MAX_MESSAGE = 1000;
    private static final Locale AR = new Locale("ar", "MA");

    // ---- Theme colors (filled by applyTheme) ----
    private boolean dark;
    private int bg, surface, ink, muted, line, brand, green, heroBg, heroSub;

    private final String branchArabic = "فرع السلام", organizationArabic = "الكشفية الحسنية المغربية";
    private final String[] classIds = {"cubs", "wolf_cubs_brownies", "scouts_guides", "senior_scouts_guides", "rovers_rangers"};
    private final String[] classArabic = {"البراعم", "الأشبال والزهرات", "الكشافة والمرشدات", "الكشاف المتقدم والرائدات", "الجوالة والدليلات"};
    private final String[] classAges = {"من 5 إلى 7 سنوات", "من 7 إلى 12 سنة", "من 12 إلى 15 سنة", "من 15 إلى 18 سنة", "من 18 إلى 23 سنة"};
    private final String[] classTaglines = {"اكتشاف ومتعة", "معًا نتعلّم وننمو", "خدمة ومهارة", "قيادة ومبادرة", "مسؤولية وخدمة المجتمع"};
    private final int[] classColors = {Color.rgb(211, 166, 55), Color.rgb(52, 112, 190), Color.rgb(45, 137, 86), Color.rgb(202, 61, 68), Color.rgb(137, 119, 77)};
    private final int[] classLogos = {R.drawable.class_cubs, R.drawable.class_wolf_cubs, R.drawable.class_scouts_guides, R.drawable.class_senior, R.drawable.class_rovers};
    private final String[] genderLabels = {"أنثى", "ذكر"};
    private final String[] genderValues = {"female", "male"};

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;
    private int accent, selectedClass = -1;
    private String scoutClassId = "", user = "", emailAddress = "", gender = "", rank = "member", page = "Home";
    private boolean legacyClassSetup = false, isAdmin = false;
    private String currentRoomId = "global", currentRoomTitle = "الصالة العامة";
    private ListenerRegistration chatListener, announcementListener;
    private LinearLayout root, content, roomMessages;
    private EditText messageInput;

    // =====================================================================
    // Lifecycle
    // =====================================================================
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("kashaf_prefs", MODE_PRIVATE);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        applyTheme();
        accent = defaultAccent();
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (auth.getCurrentUser() != null) loadProfile(); else showAuth(false);
    }

    @Override protected void onDestroy() { clearListeners(); super.onDestroy(); }

    @Override public void onBackPressed() {
        if (root != null && page.equals("Room")) { page = "Chat"; showApp(); }
        else if (root != null && !page.equals("Home") && auth.getCurrentUser() != null && !user.isEmpty()) { page = "Home"; showApp(); }
        else super.onBackPressed();
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
            bg = Color.rgb(12, 17, 23); surface = Color.rgb(21, 28, 36); ink = Color.rgb(236, 241, 246); muted = Color.rgb(146, 160, 175);
            line = Color.rgb(38, 49, 61); brand = Color.rgb(186, 211, 240); green = Color.rgb(112, 201, 150);
            heroBg = Color.rgb(24, 44, 70); heroSub = Color.rgb(196, 208, 222);
        } else {
            bg = Color.rgb(245, 247, 245); surface = Color.WHITE; ink = Color.rgb(24, 40, 60); muted = Color.rgb(98, 110, 125);
            line = Color.rgb(224, 231, 227); brand = Color.rgb(20, 38, 61); green = Color.rgb(34, 102, 69);
            heroBg = Color.rgb(20, 38, 61); heroSub = Color.rgb(212, 222, 234);
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

    // =====================================================================
    // View helpers
    // =====================================================================
    private int dp(float n) { return (int) (n * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private GradientDrawable bordered(int color, int radius, int stroke) { return bordered(color, radius, stroke, 1); }
    private GradientDrawable bordered(int color, int radius, int stroke, int width) { GradientDrawable d = shape(color, radius); d.setStroke(dp(width), stroke); return d; }
    private Drawable ripple(Drawable d) { return new RippleDrawable(ColorStateList.valueOf(Color.argb(dark ? 50 : 40, 128, 128, 128)), d, null); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setLineSpacing(0, 1.12f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void pad(View v, int l, int t, int r, int b) { v.setPadding(dp(l), dp(t), dp(r), dp(b)); }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w > 0 ? dp(w) : w, h > 0 ? dp(h) : h); }
    private LinearLayout.LayoutParams weighted(int startMargin) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f); p.setMarginStart(dp(startMargin)); return p; }
    private void top(LinearLayout parent, View child, int margin) { LinearLayout.LayoutParams p = lp(-1, -2); p.topMargin = dp(margin); parent.addView(child, p); }
    private LinearLayout card(int radius) { LinearLayout c = column(); c.setBackground(bordered(surface, radius, line)); return c; }
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
        TextView b = text(label, 16, textColor, true); b.setGravity(Gravity.CENTER); b.setBackground(ripple(shape(fill, 16))); b.setMinHeight(dp(54)); pad(b, 18, 14, 18, 14); return b;
    }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show(); }

    private void applyInsets(View v) {
        v.setOnApplyWindowInsetsListener((view, insets) -> {
            int t, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                t = bars.top; b = Math.max(bars.bottom, ime.bottom);
            } else { t = insets.getSystemWindowInsetTop(); b = insets.getSystemWindowInsetBottom(); }
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
        LinearLayout b = row(); b.setBackground(bordered(surface, 14, line));
        b.addView(e, new LinearLayout.LayoutParams(0, dp(54), 1f));
        if (trailing != null) { pad(trailing, 14, 0, 14, 0); trailing.setMinimumHeight(dp(54)); b.addView(trailing, new LinearLayout.LayoutParams(-2, dp(54))); }
        e.setOnFocusChangeListener((v, focus) -> b.setBackground(focus ? bordered(surface, 14, accent, 2) : bordered(surface, 14, line)));
        return b;
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
            TextView chip = text(labels[i], 14, ink, false); chip.setGravity(Gravity.CENTER); chip.setSingleLine(true);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            if (i % 2 == 1) cp.setMarginStart(dp(8));
            current.addView(chip, cp); all.add(chip);
            chip.setOnClickListener(v -> { sel[0] = index; refresh.run(); });
        }
        refresh.run();
        return wrap;
    }
    private void styleChip(TextView chip, boolean selected) {
        chip.setBackground(ripple(selected ? bordered(tint(brand, .14f), 14, brand, 2) : bordered(surface, 14, line)));
        chip.setTextColor(selected ? brand : ink); chip.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    // =====================================================================
    // Auth
    // =====================================================================
    private void showAuth(boolean signup) {
        accent = signup && selectedClass >= 0 ? classColors[selectedClass] : defaultAccent();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(bg);
        LinearLayout form = column(); pad(form, 24, 28, 24, 32); scroll.addView(form); setContentView(scroll); applyInsets(scroll);
        FirebaseUser signedIn = auth.getCurrentUser();

        LinearLayout brandRow = row();
        ImageView logo = new ImageView(this); logo.setImageResource(R.mipmap.ic_launcher); brandRow.addView(logo, lp(48, 48));
        LinearLayout names = column(); names.addView(text(organizationArabic, 17, brand, true)); top(names, text(branchArabic, 13, green, true), 2);
        brandRow.addView(names, weighted(12)); form.addView(brandRow);
        top(form, text(signup ? "انضم إلى فرع السلام" : "مرحبًا بعودتك", 30, ink, true), 36);
        top(form, text(signup ? "أنشئ حسابك واختر فئتك الكشفية." : "سجّل الدخول للوصول إلى مجتمعك الكشفي.", 15, muted, false), 8);

        final EditText nameField = signup ? field("اسمك الكامل") : null;
        if (nameField != null) { if (legacyClassSetup && user != null) nameField.setText(user); labeled(form, "الاسم", box(nameField, null), 26); }

        final EditText emailField = field("name@example.com");
        emailField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setTextDirection(View.TEXT_DIRECTION_LTR); emailField.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        if (signup && signedIn != null && signedIn.getEmail() != null) { emailField.setText(signedIn.getEmail()); emailField.setEnabled(false); }
        labeled(form, "البريد الإلكتروني", box(emailField, null), signup ? 16 : 26);

        final EditText passField = signedIn == null ? field("كلمة المرور") : null;
        if (passField != null) {
            passField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passField.setTextDirection(View.TEXT_DIRECTION_LTR); passField.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            TextView toggle = text("إظهار", 13, accentText(accent), true); toggle.setGravity(Gravity.CENTER);
            final boolean[] visible = {false};
            toggle.setOnClickListener(v -> {
                visible[0] = !visible[0];
                passField.setTransformationMethod(visible[0] ? null : PasswordTransformationMethod.getInstance());
                passField.setSelection(passField.getText().length());
                toggle.setText(visible[0] ? "إخفاء" : "إظهار");
            });
            labeled(form, "كلمة المرور", box(passField, toggle), 16);
        }

        final int[] genderSel = {(legacyClassSetup || signedIn != null) ? genderPosition(gender) : -1};
        final TextView[] submitRef = new TextView[1];
        if (signup) {
            top(form, text("الجنس", 13, muted, true), 20); top(form, chips(genderLabels, genderSel), 7);
            top(form, text("الفئة الكشفية", 19, ink, true), 28);
            top(form, text("فئتك تحدّد لونك ودردشتك الخاصة.", 13, muted, false), 4);
            LinearLayout options = column(); top(form, options, 12);
            final LinearLayout[] rows = new LinearLayout[classIds.length]; final FrameLayout[] checks = new FrameLayout[classIds.length];
            for (int i = 0; i < classIds.length; i++) {
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
                option.setOnClickListener(v -> {
                    selectedClass = index; accent = classColors[index];
                    for (int j = 0; j < rows.length; j++) styleClassOption(rows[j], checks[j], j);
                    if (submitRef[0] != null) { submitRef[0].setBackground(ripple(shape(accent, 16))); submitRef[0].setTextColor(onColor(accent)); }
                });
                styleClassOption(option, check, i);
            }
            top(form, text("الرتبة عند التسجيل: عضو", 13, muted, true), 14);
        }

        TextView submit = button(signup ? (signedIn != null ? "إكمال الحساب" : "إنشاء الحساب") : "تسجيل الدخول", accent, onColor(accent));
        top(form, submit, signup ? 22 : 28); if (signup) submitRef[0] = submit;
        submit.setOnClickListener(v -> {
            String e = emailField.getText().toString().trim(), p = passField == null ? "" : passField.getText().toString();
            String n = nameField == null ? "" : nameField.getText().toString().trim();
            if (e.isEmpty() || (passField != null && p.isEmpty()) || (signup && n.isEmpty())) { toast("أكمل الحقول المطلوبة."); return; }
            if (passField != null && p.length() < 6) { toast("كلمة المرور يجب ألا تقل عن 6 أحرف."); return; }
            if (signup) {
                if (genderSel[0] < 0) { toast("اختر الجنس."); return; }
                if (selectedClass < 0) { toast("اختر فئتك الكشفية أولًا."); return; }
                createAccount(n, e, p, genderValues[genderSel[0]], selectedClass);
            } else {
                auth.signInWithEmailAndPassword(e, p).addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) loadProfile(); else toast("تعذّر تسجيل الدخول. تحقق من البريد وكلمة المرور.");
                });
            }
        });
        if (signedIn == null) {
            TextView toggle = text(signup ? "لديك حساب؟ سجّل الدخول" : "مستخدم جديد؟ أنشئ حسابًا", 14, accentText(accent), true);
            toggle.setGravity(Gravity.CENTER); pad(toggle, 8, 14, 8, 14); top(form, toggle, 12);
            toggle.setOnClickListener(v -> { selectedClass = -1; showAuth(!signup); });
        }
    }
    private void styleClassOption(LinearLayout option, FrameLayout check, int i) {
        boolean sel = i == selectedClass;
        option.setBackground(ripple(sel ? bordered(tint(classColors[i], .12f), 18, classColors[i], 2) : bordered(surface, 18, line)));
        check.setVisibility(sel ? View.VISIBLE : View.INVISIBLE);
    }

    private int genderPosition(String value) { for (int i = 0; i < genderValues.length; i++) if (genderValues[i].equals(value)) return i; return -1; }
    private String genderLabel(String value) { int p = genderPosition(value); return p < 0 ? "غير محدد" : genderLabels[p]; }
    private String rankLabel(String value) { if ("member".equals(value)) return "عضو"; if ("admin".equals(value)) return "مشرف"; return value; }
    private String classId(int index) { return classIds[index]; }
    private int classIndex(String id) { for (int i = 0; i < classIds.length; i++) if (classIds[i].equals(id)) return i; return 0; }

    private void createAccount(String name, String email, String password, String genderValue, int classPosition) {
        FirebaseUser current = auth.getCurrentUser();
        if (current != null && email.equalsIgnoreCase(current.getEmail())) { saveProfile(current, name, email, genderValue, classPosition); return; }
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) { toast("تعذّر إنشاء الحساب. تحقق من البريد أو جرّب كلمة مرور أخرى."); return; }
            saveProfile(auth.getCurrentUser(), name, email, genderValue, classPosition);
        });
    }
    private void saveProfile(FirebaseUser account, String name, String email, String genderValue, int classPosition) {
        if (account == null) { toast("الرجاء تسجيل الدخول من جديد."); return; }
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
            profile.put("rank", "member"); profile.put("createdAt", FieldValue.serverTimestamp());
            ref.set(profile).addOnSuccessListener(unused -> finishProfile(name, verifiedEmail, genderValue, classPosition, "member"))
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
        user = name; emailAddress = email; gender = genderValue; scoutClassId = classId(classPosition); rank = savedRank; accent = classColors[classPosition]; page = "Home"; loadAdminStatus();
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
            scoutClassId = snapshot.getString("scoutClass");
            if (scoutClassId == null || !Arrays.asList(classIds).contains(scoutClassId)) { legacyClassSetup = true; selectedClass = -1; toast("اختر فئتك الكشفية لإكمال ملفك الشخصي."); showAuth(true); return; }
            legacyClassSetup = false; accent = classColors[classIndex(scoutClassId)]; page = "Home"; loadAdminStatus();
        }).addOnFailureListener(error -> { toast("تعذّر تحميل ملفك الشخصي. تحقق من الاتصال."); showAuth(false); });
    }
    private void loadAdminStatus() {
        FirebaseUser account = auth.getCurrentUser();
        if (account == null) { isAdmin = false; showApp(); return; }
        db.collection("admins").document(account.getUid()).get().addOnSuccessListener(doc -> { isAdmin = doc.exists(); showApp(); }).addOnFailureListener(error -> { isAdmin = false; showApp(); });
    }
    private void clearListeners() {
        if (chatListener != null) { chatListener.remove(); chatListener = null; }
        if (announcementListener != null) { announcementListener.remove(); announcementListener = null; }
    }

    // =====================================================================
    // App shell
    // =====================================================================
    private void showApp() {
        clearListeners();
        root = column(); root.setBackgroundColor(bg); setContentView(root); applyInsets(root);
        if (page.equals("Room")) { roomScreen(); return; }
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        content = column(); pad(content, 20, 20, 20, 24); scroll.addView(content);
        if (page.equals("Settings")) content.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        renderPage();
        bottomNav();
    }
    private void bottomNav() {
        View divider = new View(this); divider.setBackgroundColor(line); root.addView(divider, lp(-1, 1));
        LinearLayout nav = row(); nav.setGravity(Gravity.CENTER); nav.setBackgroundColor(surface); pad(nav, 4, 8, 4, 8);
        final String[] tabs = {"Home", "Chat", "News", "Profile", "Settings"};
        int[] icons = {R.drawable.ic_home, R.drawable.ic_chat, R.drawable.ic_news, R.drawable.ic_person, R.drawable.ic_settings};
        String[] labels = {"الرئيسية", "الدردشة", "الإعلانات", "ملفي", "الإعدادات"};
        for (int i = 0; i < tabs.length; i++) {
            final String tab = tabs[i]; boolean active = page.equals(tab);
            LinearLayout item = column(); item.setGravity(Gravity.CENTER);
            FrameLayout pill = new FrameLayout(this); if (active) pill.setBackground(shape(tint(accent, .18f), 16));
            pill.addView(icon(icons[i], active ? accentText(accent) : muted), new FrameLayout.LayoutParams(dp(23), dp(23), Gravity.CENTER));
            item.addView(pill, lp(54, 30));
            TextView caption = text(labels[i], 11, active ? accentText(accent) : muted, active); caption.setGravity(Gravity.CENTER); caption.setSingleLine(true);
            top(item, caption, 3);
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
            item.setOnClickListener(v -> { page = tab; showApp(); });
        }
        root.addView(nav, lp(-1, -2));
    }
    private void renderPage() {
        content.removeAllViews();
        if (page.equals("Home")) homePage();
        else if (page.equals("News")) newsPage();
        else if (page.equals("Chat")) chatPage();
        else if (page.equals("Settings")) settingsPage();
        else profilePage();
    }
    private String initial(String value) { return value == null || value.isEmpty() ? "ك" : value.substring(0, 1).toUpperCase(); }
    private void pageHeader(String title, String subtitle) {
        content.addView(text(title, 28, ink, true));
        top(content, text(subtitle, 14, muted, false), 6);
    }
    private void sectionLabel(String title) { top(content, text(title, 18, ink, true), 28); }

    // ---- Home ----
    private void homePage() {
        LinearLayout head = row(); LinearLayout b = column();
        b.addView(text(organizationArabic, 15, brand, true)); top(b, text(branchArabic, 12, green, true), 2);
        head.addView(b, weighted(0));
        TextView avatar = text(initial(user), 17, accentText(accent), true); avatar.setGravity(Gravity.CENTER); avatar.setBackground(shape(tint(accent, .18f), 24));
        head.addView(avatar, lp(46, 46)); content.addView(head);
        top(content, text("مرحبًا، " + user, 27, ink, true), 26);
        top(content, text("أهلًا بك في مجتمع فرع السلام الكشفي.", 14, muted, false), 6);
        classHero();
        sectionLabel("مساحتك الكشفية");
        quickLink(R.drawable.ic_chat, "دردشة الفئة", "تحدّث مع أعضاء فئتك", "Chat");
        quickLink(R.drawable.ic_news, "الإعلانات", "أخبار الفرع وفئتك", "News");
    }
    private void classHero() {
        int idx = classIndex(scoutClassId);
        LinearLayout hero = column(); hero.setBackground(shape(heroBg, 26)); pad(hero, 20, 20, 20, 20); top(content, hero, 24);
        LinearLayout head = row(); head.addView(logoView(idx, 66));
        LinearLayout details = column(); head.addView(details, weighted(14));
        TextView badge = text("فئتك الكشفية", 11, onColor(classColors[idx]), true); badge.setBackground(shape(classColors[idx], 12)); pad(badge, 10, 4, 10, 4);
        details.addView(badge, new LinearLayout.LayoutParams(-2, -2));
        top(details, text(classArabic[idx], 21, Color.WHITE, true), 8);
        top(details, text(classAges[idx], 12, heroSub, false), 3);
        hero.addView(head);
        top(hero, text(classTaglines[idx], 15, Color.WHITE, true), 16);
        top(hero, text(organizationArabic + " · " + branchArabic, 12, heroSub, false), 5);
    }
    private void quickLink(int iconRes, String title, String subtitle, String target) {
        LinearLayout c = row(); c.setBackground(ripple(bordered(surface, 20, line))); pad(c, 14, 14, 14, 14); top(content, c, 12);
        c.addView(iconBox(iconRes, accentText(accent), tint(accent, .18f), 22, 14), lp(46, 46));
        LinearLayout words = column(); words.addView(text(title, 16, ink, true)); top(words, text(subtitle, 13, muted, false), 3);
        c.addView(words, weighted(14)); c.addView(icon(R.drawable.ic_chevron, muted), lp(22, 22));
        c.setOnClickListener(v -> { page = target; showApp(); });
    }

    // ---- Chat list ----
    private void chatPage() {
        pageHeader("الدردشة", "اختر مساحة للحديث مع الأعضاء.");
        roomCard(-1, "الصالة العامة", "مفتوحة لجميع الأعضاء", "الجميع", brand, "global");
        if (isAdmin) { for (int i = 0; i < classIds.length; i++) roomCard(i, classArabic[i], classAges[i], "مشرف", classColors[i], classId(i)); }
        else { int idx = classIndex(scoutClassId); roomCard(idx, classArabic[idx], classAges[idx], "فئتي", classColors[idx], classId(idx)); }
    }
    private void roomCard(int classIdx, String title, String subtitle, String tag, int color, String roomId) {
        LinearLayout c = row(); c.setBackground(ripple(bordered(surface, 20, line))); pad(c, 14, 14, 14, 14); top(content, c, 12);
        if (classIdx >= 0) c.addView(logoView(classIdx, 50));
        else c.addView(iconBox(R.drawable.ic_chat, accentText(color), tint(color, .16f), 24, 16), lp(50, 50));
        LinearLayout words = column(); words.addView(text(title, 17, ink, true)); top(words, text(subtitle, 13, muted, false), 3);
        c.addView(words, weighted(14));
        TextView badge = text(tag, 11, accentText(color), true); badge.setBackground(shape(tint(color, .16f), 20)); pad(badge, 11, 6, 11, 6); c.addView(badge);
        c.setOnClickListener(v -> { currentRoomId = roomId; currentRoomTitle = title; page = "Room"; showApp(); });
    }

    // ---- Chat room ----
    private void roomScreen() {
        int idx = currentRoomId.equals("global") ? -1 : classIndex(currentRoomId);
        LinearLayout header = row(); header.setBackgroundColor(surface); pad(header, 6, 8, 16, 8);
        FrameLayout back = iconBox(R.drawable.ic_back, ink, Color.TRANSPARENT, 24, 22);
        back.setOnClickListener(v -> { page = "Chat"; showApp(); });
        header.addView(back, lp(44, 44));
        if (idx >= 0) { LinearLayout.LayoutParams lg = lp(42, 42); lg.setMarginStart(dp(4)); header.addView(logoView(idx, 42), lg); }
        else { LinearLayout.LayoutParams lg = lp(42, 42); lg.setMarginStart(dp(4)); header.addView(iconBox(R.drawable.ic_chat, accentText(brand), tint(brand, .16f), 22, 14), lg); }
        LinearLayout titles = column(); titles.addView(text(currentRoomTitle, 17, ink, true));
        top(titles, text(currentRoomId.equals("global") ? "مفتوحة لجميع الأعضاء" : "دردشة خاصة بأعضاء هذه الفئة", 12, muted, false), 1);
        header.addView(titles, weighted(12));
        root.addView(header, lp(-1, -2));
        View divider = new View(this); divider.setBackgroundColor(line); root.addView(divider, lp(-1, 1));

        ScrollView messagesScroll = new ScrollView(this); messagesScroll.setFillViewport(true);
        roomMessages = column(); pad(roomMessages, 12, 12, 12, 12); messagesScroll.addView(roomMessages);
        root.addView(messagesScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        buildComposer();
        listenMessages(messagesScroll);
    }
    private void buildComposer() {
        LinearLayout bar = column(); bar.setBackgroundColor(bg); pad(bar, 10, 6, 10, 10);
        final TextView counter = text("", 11, muted, false); counter.setVisibility(View.GONE); pad(counter, 12, 0, 12, 4); bar.addView(counter, lp(-1, -2));

        LinearLayout pill = row(); pill.setGravity(Gravity.BOTTOM); pill.setBackground(bordered(surface, 28, line)); pad(pill, 4, 4, 4, 4);
        FrameLayout attach = iconBox(R.drawable.ic_attach, muted, Color.TRANSPARENT, 24, 22); attach.setContentDescription("إرفاق صورة");
        attach.setOnClickListener(v -> pickChatImage());
        pill.addView(attach, lp(44, 44));

        messageInput = new EditText(this);
        messageInput.setBackground(null); messageInput.setHint("اكتب رسالة…"); messageInput.setHintTextColor(muted); messageInput.setTextColor(ink); messageInput.setTextSize(16);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageInput.setMinHeight(dp(44)); messageInput.setMaxLines(5); messageInput.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        messageInput.setPadding(dp(6), dp(10), dp(6), dp(10)); messageInput.setLineSpacing(0, 1.12f);
        messageInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_MESSAGE)});
        pill.addView(messageInput, new LinearLayout.LayoutParams(0, -2, 1f));

        final FrameLayout send = iconBox(R.drawable.ic_send, onColor(accent), accent, 22, 22);
        send.getChildAt(0).setScaleX(-1f); send.setContentDescription("إرسال"); send.setAlpha(.4f); send.setEnabled(false);
        send.setOnClickListener(v -> sendMessage());
        pill.addView(send, lp(44, 44));
        bar.addView(pill, lp(-1, -2));
        root.addView(bar, lp(-1, -2));

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                boolean has = s.toString().trim().length() > 0;
                send.setEnabled(has); send.setAlpha(has ? 1f : .4f);
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

    private void listenMessages(ScrollView messagesScroll) {
        chatListener = db.collection("rooms").document(currentRoomId).collection("messages").orderBy("createdAt", Query.Direction.DESCENDING).limit(60).addSnapshotListener((snapshot, error) -> {
            if (error != null) { toast("تعذّر تحميل الرسائل."); return; }
            if (snapshot == null || roomMessages == null) return;
            roomMessages.removeAllViews();
            if (snapshot.isEmpty()) {
                LinearLayout empty = column(); empty.setGravity(Gravity.CENTER); pad(empty, 20, 60, 20, 20);
                empty.addView(iconBox(R.drawable.ic_chat, accentText(accent), tint(accent, .16f), 28, 30), lp(64, 64));
                TextView t = text(currentRoomId.equals("global") ? "لا توجد رسائل بعد" : "ابدأ الحديث مع فئتك", 16, ink, true); t.setGravity(Gravity.CENTER); top(empty, t, 14);
                TextView s = text("كن أول من يكتب هنا.", 13, muted, false); s.setGravity(Gravity.CENTER); top(empty, s, 4);
                roomMessages.addView(empty, lp(-1, -2)); return;
            }
            String myUid = auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid();
            int maxBubble = (int) (getResources().getDisplayMetrics().widthPixels * 0.78f);
            int mineBg = dark ? blend(accent, surface, .32f) : blend(accent, Color.WHITE, .20f);
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", AR);
            for (int i = snapshot.size() - 1; i >= 0; i--) {
                DocumentSnapshot doc = snapshot.getDocuments().get(i);
                String author = doc.getString("authorName"), body = doc.getString("text"), authorId = doc.getString("authorId");
                boolean mine = myUid.equals(authorId);
                BubbleLayout bubble = new BubbleLayout(maxBubble); bubble.setOrientation(LinearLayout.VERTICAL);
                float r = dp(18), s = dp(5);
                GradientDrawable bd = new GradientDrawable(); bd.setColor(mine ? mineBg : surface); if (!mine) bd.setStroke(dp(1), line);
                bd.setCornerRadii(mine ? new float[]{r, r, r, r, s, s, r, r} : new float[]{r, r, r, r, r, r, s, s});
                bubble.setBackground(bd); pad(bubble, 13, 9, 13, 8);
                if (!mine) bubble.addView(text(author == null ? "عضو" : author, 12, accentText(accent), true));
                Blob media = doc.getBlob("mediaData"); String mediaType = doc.getString("mediaType");
                if (media != null && mediaType != null) {
                    LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(maxBubble - dp(26), dp(200)); pp.topMargin = dp(mine ? 0 : 6);
                    bubble.addView(mediaPreview(media.toBytes(), mediaType), pp);
                }
                if (body != null && !body.isEmpty()) { TextView m = text(body, 15, ink, false); m.setLineSpacing(dp(2), 1.1f); top(bubble, m, mine || (media != null) ? (media != null ? 6 : 0) : 3); }
                Timestamp ts = doc.getTimestamp("createdAt");
                TextView time = text(ts == null ? "…" : timeFormat.format(ts.toDate()), 10, muted, false);
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2); tp.topMargin = dp(3); tp.gravity = Gravity.END; bubble.addView(time, tp);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2); bp.topMargin = dp(6); bp.gravity = mine ? Gravity.START : Gravity.END;
                roomMessages.addView(bubble, bp);
            }
            messagesScroll.post(() -> messagesScroll.fullScroll(View.FOCUS_DOWN));
        });
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
    private View mediaPreview(byte[] bytes, String type) {
        if ("image/gif".equals(type)) return new AnimatedGifView(bytes);
        ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap != null) image.setImageBitmap(bitmap); else image.setContentDescription("صورة مرفقة");
        image.setBackground(shape(tint(accent, .10f), 12)); image.setClipToOutline(true); return image;
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

    // ---- Announcements ----
    private void newsPage() {
        pageHeader("الإعلانات", "آخر أخبار فرع السلام الكشفي.");
        if (isAdmin) {
            TextView b = button("إعلان جديد", accent, onColor(accent)); top(content, b, 18); b.setOnClickListener(v -> showAnnouncementDialog());
        }
        LinearLayout feed = column(); top(content, feed, 12); listenAnnouncements(feed);
    }
    private int audienceColor(String audience) { for (int i = 0; i < classIds.length; i++) if (classIds[i].equals(audience)) return classColors[i]; return brand; }
    private String audienceName(String audience) {
        if ("all".equals(audience)) return "الجميع";
        for (int i = 0; i < classIds.length; i++) if (classIds[i].equals(audience)) return classArabic[i];
        return "إعلان قديم";
    }
    private void listenAnnouncements(LinearLayout feed) {
        Query query = db.collection("announcements");
        if (isAdmin) query = query.orderBy("createdAt", Query.Direction.DESCENDING).limit(50); else query = query.whereIn("audience", Arrays.asList("all", scoutClassId));
        announcementListener = query.addSnapshotListener((snapshot, error) -> {
            if (error != null) { toast("تعذّر تحميل الإعلانات."); return; }
            if (snapshot == null) return;
            feed.removeAllViews(); int shown = 0;
            ArrayList<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
            Collections.sort(docs, (a, b) -> { Timestamp ta = a.getTimestamp("createdAt"), tb = b.getTimestamp("createdAt"); if (ta == null) return tb == null ? 0 : 1; if (tb == null) return -1; return tb.compareTo(ta); });
            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy · h:mm a", AR);
            for (DocumentSnapshot doc : docs) {
                String audience = doc.getString("audience");
                if (!isAdmin && !("all".equals(audience) || scoutClassId.equals(audience))) continue;
                int color = audienceColor(audience);
                LinearLayout c = card(20); pad(c, 18, 16, 18, 18);
                LinearLayout.LayoutParams cp = lp(-1, -2); cp.topMargin = dp(12); feed.addView(c, cp);
                TextView badge = text(audienceName(audience), 12, accentText(color), true); badge.setBackground(shape(tint(color, .16f), 20)); pad(badge, 11, 5, 11, 5);
                c.addView(badge, new LinearLayout.LayoutParams(-2, -2));
                top(c, text(doc.getString("title") == null ? "إعلان" : doc.getString("title"), 19, ink, true), 12);
                TextView body = text(doc.getString("body") == null ? "" : doc.getString("body"), 15, muted, false); body.setLineSpacing(dp(3), 1.1f); top(c, body, 8);
                String authorName = doc.getString("createdByName"); Timestamp created = doc.getTimestamp("createdAt");
                String meta = (authorName != null && !authorName.isEmpty() ? authorName : "") + (authorName != null && !authorName.isEmpty() && created != null ? " · " : "") + (created != null ? dateFormat.format(created.toDate()) : "");
                if (!meta.isEmpty()) top(c, text(meta, 12, muted, false), 14);
                shown++;
            }
            if (shown == 0) emptyStateIn(feed, R.drawable.ic_news, "لا توجد إعلانات بعد", isAdmin ? "ستظهر إعلانات فرع السلام هنا." : "ستظهر هنا إعلانات الفرع والإعلانات الموجهة لفئتك.");
        });
    }
    private void emptyStateIn(LinearLayout parent, int iconRes, String title, String subtitle) {
        LinearLayout c = card(22); c.setGravity(Gravity.CENTER); pad(c, 24, 34, 24, 34);
        c.addView(iconBox(iconRes, accentText(accent), tint(accent, .16f), 28, 30), lp(64, 64));
        TextView h = text(title, 18, ink, true); h.setGravity(Gravity.CENTER); top(c, h, 16);
        TextView s = text(subtitle, 14, muted, false); s.setGravity(Gravity.CENTER); top(c, s, 6);
        LinearLayout.LayoutParams p = lp(-1, -2); p.topMargin = dp(12); parent.addView(c, p);
    }
    private ArrayAdapter<String> styledAdapter(String[] items) {
        return new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override public View getView(int p, View c, ViewGroup parent) { TextView t = (TextView) super.getView(p, c, parent); t.setTextColor(ink); t.setTextSize(15); return t; }
            @Override public View getDropDownView(int p, View c, ViewGroup parent) { TextView t = (TextView) super.getDropDownView(p, c, parent); t.setTextColor(ink); t.setTextSize(15); t.setBackgroundColor(surface); pad(t, 16, 14, 16, 14); return t; }
        };
    }
    private int dialogTheme() { return dark ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert; }
    private void showAnnouncementDialog() { showAnnouncementDialog("", "", 0); }
    private void showAnnouncementDialog(String initialTitle, String initialBody, int initialAudience) {
        LinearLayout form = column(); form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); pad(form, 18, 8, 18, 4);
        EditText title = field("عنوان الإعلان"); title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)}); title.setText(initialTitle);
        form.addView(box(title, null));
        EditText body = new EditText(this); body.setHint("اكتب الإعلان…"); body.setHintTextColor(muted); body.setTextColor(ink); body.setTextSize(16);
        body.setBackground(bordered(surface, 14, line)); body.setPadding(dp(16), dp(14), dp(16), dp(14)); body.setMinLines(4); body.setGravity(Gravity.TOP | Gravity.START);
        body.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        body.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4000)}); body.setText(initialBody); top(form, body, 10);
        String[] options = new String[1 + classIds.length], audiences = new String[1 + classIds.length];
        options[0] = "الجميع"; audiences[0] = "all";
        for (int i = 0; i < classIds.length; i++) { options[i + 1] = classArabic[i]; audiences[i + 1] = classId(i); }
        Spinner audience = new Spinner(this); audience.setAdapter(styledAdapter(options));
        audience.setSelection(Math.max(0, Math.min(initialAudience, audiences.length - 1))); audience.setBackground(bordered(surface, 14, line)); audience.setPopupBackgroundDrawable(shape(surface, 12));
        LinearLayout.LayoutParams ap = lp(-1, 52); ap.topMargin = dp(10); form.addView(audience, ap);
        AlertDialog dialog = new AlertDialog.Builder(this, dialogTheme()).setTitle("إعلان جديد").setView(form).setNegativeButton("إلغاء", null).setPositiveButton("معاينة", null).create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String t = title.getText().toString().trim(), b = body.getText().toString().trim();
            if (t.isEmpty() || b.isEmpty()) { toast("أضف العنوان والرسالة أولًا."); return; }
            int selected = audience.getSelectedItemPosition(); dialog.dismiss(); showAnnouncementPreview(t, b, audiences[selected], selected);
        }));
        dialog.show();
    }
    private void showAnnouncementPreview(String title, String body, String audience, int selectedAudience) {
        LinearLayout preview = column(); preview.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); pad(preview, 22, 8, 22, 8);
        preview.addView(text(organizationArabic + " · " + branchArabic, 12, green, true));
        top(preview, text("الجمهور: " + audienceName(audience), 13, accentText(audienceColor(audience)), true), 12);
        top(preview, text(title, 20, ink, true), 12);
        TextView message = text(body, 15, muted, false); message.setLineSpacing(dp(3), 1.1f); top(preview, message, 6);
        top(preview, text("بواسطة " + user, 12, muted, false), 14);
        new AlertDialog.Builder(this, dialogTheme()).setTitle("معاينة الإعلان").setView(preview)
            .setNegativeButton("رجوع للتعديل", (d, w) -> showAnnouncementDialog(title, body, selectedAudience))
            .setPositiveButton("نشر", (d, w) -> postAnnouncement(title, body, audience)).show();
    }
    private void postAnnouncement(String title, String body, String audience) {
        FirebaseUser account = auth.getCurrentUser(); if (account == null || !isAdmin) { toast("النشر متاح للمشرفين فقط."); return; }
        Map<String, Object> a = new HashMap<>();
        a.put("title", title); a.put("body", body); a.put("createdBy", account.getUid()); a.put("createdByName", user); a.put("createdAt", FieldValue.serverTimestamp()); a.put("audience", audience);
        db.collection("announcements").add(a).addOnSuccessListener(ref -> toast("تم نشر الإعلان.")).addOnFailureListener(error -> toast("تعذّر نشر الإعلان."));
    }

    // ---- Profile ----
    private void profilePage() {
        int idx = classIndex(scoutClassId);
        pageHeader("ملفي الشخصي", organizationArabic + " · " + branchArabic);
        LinearLayout c = card(22); pad(c, 18, 18, 18, 8); top(content, c, 20);
        LinearLayout head = row(); head.addView(logoView(idx, 62));
        LinearLayout info = column(); info.addView(text(user, 19, ink, true));
        top(info, text(classArabic[idx], 14, accentText(classColors[idx]), true), 3);
        head.addView(info, weighted(14)); c.addView(head);
        infoRow(c, "الفئة العمرية", classAges[idx]);
        infoRow(c, "البريد الإلكتروني", emailAddress == null ? "" : emailAddress);
        infoRow(c, "الجنس", genderLabel(gender));
        infoRow(c, "الرتبة", rankLabel(rank));
        TextView out = text("تسجيل الخروج", 16, Color.rgb(dark ? 240 : 190, dark ? 120 : 40, dark ? 120 : 45), true);
        out.setGravity(Gravity.CENTER); out.setBackground(ripple(bordered(surface, 16, line))); pad(out, 16, 16, 16, 16); top(content, out, 16);
        out.setOnClickListener(v -> { auth.signOut(); legacyClassSetup = false; page = "Home"; isAdmin = false; accent = defaultAccent(); showAuth(false); });
    }
    private void infoRow(LinearLayout parent, String label, String value) {
        View d = new View(this); d.setBackgroundColor(line); LinearLayout.LayoutParams dp = lp(-1, 1); dp.topMargin = dp(14); parent.addView(d, dp);
        top(parent, text(label, 12, muted, true), 10);
        TextView v = text(value, 15, ink, false); v.setPadding(0, 0, 0, dp(10)); top(parent, v, 3);
    }

    // ---- Settings (English) ----
    private void settingsPage() {
        pageHeader("Settings", "Choose how the app looks on your device.");
        sectionLabel("Appearance");
        String mode = prefs.getString("theme", "system");
        themeOption("system", mode, "System default", "Match your phone's light or dark setting");
        themeOption("light", mode, "Light", "Bright and clean");
        themeOption("dark", mode, "Dark", "Easier on the eyes at night");
        sectionLabel("About");
        LinearLayout about = card(20); pad(about, 18, 16, 18, 16); top(content, about, 12);
        about.addView(text(organizationArabic + " – " + branchArabic, 16, ink, true));
        top(about, text("Version 1.0", 13, muted, false), 6);
        TextView out = text("Sign out", 16, Color.rgb(dark ? 240 : 190, dark ? 120 : 40, dark ? 120 : 45), true);
        out.setGravity(Gravity.CENTER); out.setBackground(ripple(bordered(surface, 16, line))); pad(out, 16, 16, 16, 16); top(content, out, 24);
        out.setOnClickListener(v -> { auth.signOut(); legacyClassSetup = false; page = "Home"; isAdmin = false; accent = defaultAccent(); showAuth(false); });
    }
    private void themeOption(String value, String current, String title, String subtitle) {
        boolean sel = value.equals(current);
        LinearLayout c = row(); pad(c, 16, 14, 16, 14); top(content, c, 10);
        c.setBackground(ripple(sel ? bordered(tint(accent, .12f), 18, accent, 2) : bordered(surface, 18, line)));
        LinearLayout words = column(); words.addView(text(title, 16, ink, true)); top(words, text(subtitle, 13, muted, false), 3);
        c.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        FrameLayout mark = iconBox(R.drawable.ic_check, onColor(accent), accent, 14, 12); mark.setVisibility(sel ? View.VISIBLE : View.INVISIBLE);
        c.addView(mark, lp(24, 24));
        c.setOnClickListener(v -> { prefs.edit().putString("theme", value).apply(); applyTheme(); if (auth.getCurrentUser() == null || user.isEmpty()) accent = defaultAccent(); showApp(); });
    }
}
