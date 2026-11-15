package com.max6778.kashafapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
    private final int ink = Color.rgb(48, 39, 42), muted = Color.rgb(130, 113, 117), paper = Color.rgb(255, 249, 246), peach = Color.rgb(255, 238, 229);
    private SharedPreferences prefs;
    private int accent = Color.rgb(229, 79, 89);
    private String clan = "Red Clan", user = "";
    private LinearLayout root, body;
    private String page = "Home";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(paper); getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        prefs = getSharedPreferences("kashaf", MODE_PRIVATE);
        if (prefs.getBoolean("loggedIn", false)) showApp(); else showAuth(false);
    }

    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView label(String text, int size, int color, boolean bold) { TextView v = new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(color); if (bold) v.setTypeface(null, Typeface.BOLD); return v; }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private void pad(View v, int l, int t, int r, int b) { v.setPadding(dp(l),dp(t),dp(r),dp(b)); }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w > 0 ? dp(w) : w, h > 0 ? dp(h) : h); }

    private void showAuth(boolean signup) {
        root = column(); root.setBackgroundColor(paper); pad(root, 26, 28, 26, 24); setContentView(root);
        TextView brand = label("KASHAf", 16, accent, true); root.addView(brand);
        TextView title = label(signup ? "Join your clan." : "Welcome back.", 32, ink, true); LinearLayout.LayoutParams tp=lp(-1,-2); tp.topMargin=40; root.addView(title,tp);
        TextView sub = label(signup ? "Create an account and choose your colors." : "Sign in to continue to KashafApp.", 15, muted, false); tp=lp(-1,-2); tp.topMargin=8; root.addView(sub,tp);
        EditText name = field("Username"); tp=lp(-1,56); tp.topMargin=28; root.addView(name,tp);
        EditText pass = field("Password"); pass.setInputType(129); tp=lp(-1,56); tp.topMargin=12; root.addView(pass,tp);
        Spinner clans = null;
        if (signup) {
            clans = new Spinner(this); String[] choices={"Red Clan · RGB red", "Green Clan · RGB green", "Blue Clan · RGB blue"};
            clans.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, choices)); tp=lp(-1,56); tp.topMargin=12; root.addView(clans,tp);
        }
        final Spinner clanSpinner=clans;
        TextView submit=label(signup ? "Create account" : "Sign in",16,Color.WHITE,true); submit.setGravity(Gravity.CENTER); submit.setBackground(shape(accent,18)); tp=lp(-1,56); tp.topMargin=22; root.addView(submit,tp);
        submit.setOnClickListener(v -> {
            String n=name.getText().toString().trim(), p=pass.getText().toString();
            if(n.isEmpty() || p.isEmpty()) { toast("Enter your username and password."); return; }
            if(signup) {
                int i=clanSpinner.getSelectedItemPosition(); String[] names={"Red Clan","Green Clan","Blue Clan"}; int[] colors={Color.rgb(229,79,89),Color.rgb(42,154,103),Color.rgb(55,112,218)};
                prefs.edit().putString("user",n).putString("password",p).putString("clan",names[i]).putInt("accent",colors[i]).putBoolean("loggedIn",true).apply();
            } else {
                if(!n.equals(prefs.getString("user","")) || !p.equals(prefs.getString("password",""))) { toast("That username or password doesn’t match."); return; }
                prefs.edit().putBoolean("loggedIn",true).apply();
            }
            user=n; clan=prefs.getString("clan","Red Clan"); accent=prefs.getInt("accent",Color.rgb(229,79,89)); showApp();
        });
        TextView toggle=label(signup ? "Already have an account?  Sign in" : "New here?  Create an account",14,accent,true); toggle.setGravity(Gravity.CENTER); tp=lp(-1,54); tp.topMargin=10; root.addView(toggle,tp); toggle.setOnClickListener(v->showAuth(!signup));
        TextView note=label("Choose your clan when you create your account. Your clan sets your app accent color.",13,muted,false); note.setGravity(Gravity.CENTER); tp=lp(-1,-2); tp.topMargin=22; root.addView(note,tp);
    }
    private EditText field(String hint) { EditText e=new EditText(this); e.setSingleLine(true); e.setTextSize(15); e.setHint(hint); e.setPadding(16,0,16,0); e.setBackground(shape(Color.WHITE,16)); return e; }

    private void showApp() {
        user=prefs.getString("user",user); clan=prefs.getString("clan","Red Clan"); accent=prefs.getInt("accent",Color.rgb(229,79,89));
        root=column(); root.setBackgroundColor(paper); setContentView(root);
        body=column(); pad(body,24,28,24,18); root.addView(body,new LinearLayout.LayoutParams(-1,0,1)); renderPage();
        LinearLayout nav=new LinearLayout(this); nav.setBackgroundColor(Color.WHITE); nav.setElevation(dp(10)); nav.setGravity(Gravity.CENTER); String[] tabs={"Home","Announcements","Test","Settings"};
        for(String tab:tabs) { TextView b=label(tab,11,page.equals(tab)?accent:muted,page.equals(tab)); b.setGravity(Gravity.CENTER); b.setPadding(3,14,3,14); nav.addView(b,new LinearLayout.LayoutParams(0,60,1)); b.setOnClickListener(v->{page=tab;showApp();}); }
        root.addView(nav,lp(-1,60));
    }
    private void renderPage() {
        body.removeAllViews();
        if(page.equals("Home")) {
            body.addView(label("YOUR SPACE",12,accent,true)); TextView title=label("Hello, "+user+".",30,ink,true); LinearLayout.LayoutParams p=lp(-1,-2); p.topMargin=10; body.addView(title,p);
            TextView card=label("✦   "+clan+"\n\nYour clan, your color.\nThis is your home base for announcements and tests.",17,ink,true); card.setBackground(shape(peach,22)); pad(card,20,22,20,22); p=lp(-1,-2); p.topMargin=25; body.addView(card,p);
            TextView hint=label("Pick a section below to get started.",14,muted,false); p=lp(-1,-2);p.topMargin=20;body.addView(hint,p);
        } else if(page.equals("Announcements")) {
            heading("Announcements","Updates from your community."); emptyCard("You’re all caught up.","New announcements will show up here.");
        } else if(page.equals("Test")) {
            heading("Test","A place for your clan’s tests."); emptyCard("No tests yet.","When a test is available, you’ll find it here.");
        } else {
            heading("Settings","Your account and clan."); emptyCard(user,"Signed in · "+clan); TextView logout=label("Sign out",15,accent,true); logout.setGravity(Gravity.CENTER); logout.setBackground(shape(Color.WHITE,16)); LinearLayout.LayoutParams p=lp(-1,54);p.topMargin=14;body.addView(logout,p); logout.setOnClickListener(v->{prefs.edit().putBoolean("loggedIn",false).apply();page="Home";showAuth(false);});
        }
    }
    private void heading(String title,String subtitle) { body.addView(label("KASHAf  /  "+page.toUpperCase(),12,accent,true)); TextView t=label(title,30,ink,true); LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=10;body.addView(t,p);TextView s=label(subtitle,14,muted,false);p=lp(-1,-2);p.topMargin=6;body.addView(s,p); }
    private void emptyCard(String title,String sub) { TextView c=label(title+"\n\n"+sub,17,ink,true); c.setBackground(shape(Color.WHITE,20));pad(c,20,24,20,24);LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=24;body.addView(c,p); }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_SHORT).show(); }
}
