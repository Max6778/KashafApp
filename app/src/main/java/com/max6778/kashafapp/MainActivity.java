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
    private final int ink=Color.rgb(43,35,39), muted=Color.rgb(132,116,120), paper=Color.rgb(255,248,245), white=Color.WHITE, line=Color.rgb(239,228,224), peach=Color.rgb(255,236,226);
    private final String[] clans={"Red Clan","Green Clan","Blue Clan"};
    private final String[] clanNotes={"Bold energy","Grow together","Make your mark"};
    private final int[] clanColors={Color.rgb(226,76,88),Color.rgb(42,151,105),Color.rgb(62,112,216)};
    private SharedPreferences prefs;
    private int accent=Color.rgb(226,76,88), selectedClan=0;
    private String clan="Red Clan", user="", page="Home";
    private LinearLayout root, content;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("kashaf",MODE_PRIVATE);
        getWindow().setStatusBarColor(paper); getWindow().setNavigationBarColor(white);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(prefs.getBoolean("loggedIn",false)) showApp(); else showAuth(false);
    }

    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable bordered(int color,int radius,int stroke){GradientDrawable d=shape(color,radius);d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void pad(View v,int l,int t,int r,int b){v.setPadding(dp(l),dp(t),dp(r),dp(b));}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w>0?dp(w):w,h>0?dp(h):h);}
    private void top(LinearLayout parent,View child,int margin){LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(margin);parent.addView(child,p);}
    private TextView kicker(String value){TextView t=text(value.toUpperCase(),11,accent,true);t.setLetterSpacing(.12f);return t;}

    private void showAuth(boolean signup){
        accent=signup?clanColors[selectedClan]:prefs.getInt("accent",clanColors[0]);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(paper);
        LinearLayout pageView=column();pad(pageView,24,22,24,26);scroll.addView(pageView);setContentView(scroll);
        LinearLayout brand=row();TextView monogram=text("K",18,white,true);monogram.setGravity(Gravity.CENTER);monogram.setBackground(shape(accent,14));brand.addView(monogram,lp(42,42));
        TextView brandName=text("Kashaf",18,ink,true);LinearLayout.LayoutParams bp=lp(-2,-2);bp.leftMargin=12;brand.addView(brandName,bp);pageView.addView(brand);
        TextView eyebrow=kicker("A place for your people");top(pageView,eyebrow,35);
        TextView title=text(signup?"Find your\npeople.":"Good to see\nyou again.",36,ink,true);title.setLineSpacing(dp(1),1);top(pageView,title,10);
        TextView subtitle=text(signup?"Create your account and pick a clan to make this space yours.":"Sign in to pick up where you left off.",15,muted,false);subtitle.setLineSpacing(dp(4),1);top(pageView,subtitle,10);
        EditText name=field("Username","Your username");top(pageView,name,28);
        EditText pass=field("Password","Your password");pass.setInputType(129);top(pageView,pass,12);
        if(signup){
            TextView choose=text("Choose your clan",16,ink,true);top(pageView,choose,24);
            TextView detail=text("Your clan sets the accent color throughout the app.",13,muted,false);top(pageView,detail,4);
            LinearLayout options=column();top(pageView,options,12);LinearLayout[] clanRows=new LinearLayout[clans.length];TextView[] submitRef=new TextView[1];
            for(int i=0;i<clans.length;i++){
                final int index=i;LinearLayout option=row();clanRows[i]=option;option.setBackground(bordered(white,17,i==selectedClan?clanColors[i]:line));pad(option,14,12,14,12);
                View dot=new View(this);dot.setBackground(shape(clanColors[i],20));option.addView(dot,lp(14,14));
                LinearLayout labels=column();TextView titleText=text(clans[i],15,ink,true);labels.addView(titleText);TextView note=text(clanNotes[i],12,muted,false);top(labels,note,2);
                LinearLayout.LayoutParams op=lp(0,-2);op.weight=1;op.leftMargin=12;option.addView(labels,op);
                TextView colorText=text(new String[]{"RED","GREEN","BLUE"}[i],10,clanColors[i],true);colorText.setGravity(Gravity.CENTER);colorText.setBackground(shape(tint(clanColors[i],.12f),12));pad(colorText,9,6,9,6);option.addView(colorText);
                options.addView(option,lp(-1,-2));LinearLayout.LayoutParams sep=lp(-1,0);sep.topMargin=dp(8);options.addView(new View(this),sep);
                option.setOnClickListener(v->{selectedClan=index;accent=clanColors[index];for(int j=0;j<clanRows.length;j++)clanRows[j].setBackground(bordered(white,17,j==selectedClan?clanColors[j]:line));if(submitRef[0]!=null)submitRef[0].setBackground(shape(accent,16));});
            }
        }
        TextView submit=text(signup?"Create account":"Sign in",15,white,true);submit.setGravity(Gravity.CENTER);submit.setBackground(shape(accent,16));top(pageView,submit,24);pad(submit,18,17,18,17);if(signup)submitRef[0]=submit;
        submit.setOnClickListener(v->{String n=name.getText().toString().trim(),p=pass.getText().toString();if(n.isEmpty()||p.isEmpty()){toast("Enter your username and password.");return;}
            if(signup){int i=selectedClan;prefs.edit().putString("user",n).putString("password",p).putString("clan",clans[i]).putInt("accent",clanColors[i]).putBoolean("loggedIn",true).apply();}
            else{if(!n.equals(prefs.getString("user",""))||!p.equals(prefs.getString("password",""))){toast("That username or password doesn’t match.");return;}prefs.edit().putBoolean("loggedIn",true).apply();}
            user=n;clan=prefs.getString("clan",clans[0]);accent=prefs.getInt("accent",clanColors[0]);showApp();});
        TextView toggle=text(signup?"Already have an account?  Sign in":"New to Kashaf?  Create an account",14,accent,true);toggle.setGravity(Gravity.CENTER);top(pageView,toggle,12);pad(toggle,8,12,8,12);toggle.setOnClickListener(v->showAuth(!signup));
        if(!signup){TextView demo=text("Your clan color and account stay saved on this device.",12,muted,false);demo.setGravity(Gravity.CENTER);top(pageView,demo,12);}
    }
    private EditText field(String label,String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(15);e.setTextColor(ink);e.setHintTextColor(muted);e.setHint(hint);e.setPadding(dp(16),0,dp(16),0);e.setBackground(bordered(white,15,line));e.setContentDescription(label);return e;}
    private int tint(int color,float factor){return Color.rgb((int)(Color.red(color)*factor+255*(1-factor)),(int)(Color.green(color)*factor+255*(1-factor)),(int)(Color.blue(color)*factor+255*(1-factor)));}

    private void showApp(){
        user=prefs.getString("user",user);clan=prefs.getString("clan",clans[0]);accent=prefs.getInt("accent",clanColors[0]);
        root=column();root.setBackgroundColor(paper);setContentView(root);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=column();pad(content,22,24,22,26);scroll.addView(content);renderPage();
        LinearLayout nav=row();nav.setGravity(Gravity.CENTER);nav.setBackgroundColor(white);nav.setElevation(dp(12));String[] tabs={"Home","Announcements","Test","Settings"};String[] icons={"⌂","▤","✓","⚙"};
        for(int i=0;i<tabs.length;i++){final String tab=tabs[i];boolean active=page.equals(tab);LinearLayout item=column();item.setGravity(Gravity.CENTER);if(active)item.setBackground(shape(tint(accent,.10f),16));TextView icon=text(icons[i],20,active?accent:muted,true);icon.setGravity(Gravity.CENTER);item.addView(icon);TextView caption=text(tabs[i],10,active?accent:muted,active);caption.setGravity(Gravity.CENTER);top(item,caption,2);nav.addView(item,new LinearLayout.LayoutParams(0,dp(58),1));item.setOnClickListener(v->{page=tab;showApp();});}
        pad(nav,8,5,8,5);root.addView(nav,lp(-1,68));
    }
    private void renderPage(){content.removeAllViews();
        if(page.equals("Home")){
            LinearLayout top=row();TextView brand=kicker("Kashaf  /  Home");LinearLayout.LayoutParams grow=lp(0,-2);grow.weight=1;top.addView(brand,grow);TextView avatar=text(initial(user),15,accent,true);avatar.setGravity(Gravity.CENTER);avatar.setBackground(shape(tint(accent,.12f),22));top.addView(avatar,lp(42,42));content.addView(top);
            TextView hello=text("Hello, "+user+" ✨",29,ink,true);top(content,hello,25);TextView sub=text("Your community is right here.",15,muted,false);top(content,sub,5);
            clanHero();
            sectionLabel("Your space","EXPLORE");
            quickLink("▤","Announcements","Catch up with your community","Announcements");quickLink("✓","Tests","See what’s coming up","Test");
        }else if(page.equals("Announcements")){pageHeader("Announcements","The latest from your community.");emptyState("▤","All caught up","New announcements will show up here.");}
        else if(page.equals("Test")){pageHeader("Your tests","A little challenge goes a long way.");emptyState("✓","Nothing to take yet","When a test is ready, it’ll appear here.");}
        else{pageHeader("Settings","Make yourself at home.");profileCard();TextView signout=text("Sign out",15,accent,true);signout.setGravity(Gravity.CENTER);signout.setBackground(bordered(white,16,line));top(content,signout,14);pad(signout,16,16,16,16);signout.setOnClickListener(v->{prefs.edit().putBoolean("loggedIn",false).apply();page="Home";showAuth(false);});}
    }
    private String initial(String value){return value==null||value.isEmpty()?"K":value.substring(0,1).toUpperCase();}
    private void pageHeader(String title,String subtitle){content.addView(kicker("Kashaf  /  "+page));TextView h=text(title,30,ink,true);top(content,h,18);TextView s=text(subtitle,15,muted,false);top(content,s,5);}
    private void clanHero(){
        LinearLayout card=column();card.setBackground(shape(ink,24));pad(card,20,20,20,20);top(content,card,24);
        LinearLayout head=row();TextView badge=text("✦  "+clan.toUpperCase(),11,white,true);badge.setLetterSpacing(.06f);badge.setBackground(shape(0x33ffffff,30));pad(badge,11,8,11,8);head.addView(badge);LinearLayout.LayoutParams spacer=lp(0,1);spacer.weight=1;head.addView(new View(this),spacer);TextView dot=text("●",14,accent,true);head.addView(dot);card.addView(head);
        TextView title=text("This is your\ncorner of the world.",24,white,true);title.setLineSpacing(dp(2),1);top(card,title,22);
        TextView desc=text("Find your people. Share what matters.",13,Color.rgb(220,207,207),false);top(card,desc,7);
        LinearLayout colors=row();TextView colorLabel=text("YOUR CLAN COLOR",10,Color.rgb(220,207,207),true);colorLabel.setLetterSpacing(.1f);colors.addView(colorLabel);LinearLayout.LayoutParams push=lp(0,1);push.weight=1;colors.addView(new View(this),push);TextView rgb=text(rgbLabel(accent),11,white,true);rgb.setBackground(shape(0x33ffffff,20));pad(rgb,10,6,10,6);colors.addView(rgb);top(card,colors,20);
    }
    private String rgbLabel(int c){return "RGB  "+Color.red(c)+" · "+Color.green(c)+" · "+Color.blue(c);}
    private void sectionLabel(String title,String eyebrow){LinearLayout r=row();TextView t=text(title,20,ink,true);LinearLayout.LayoutParams a=lp(0,-2);a.weight=1;r.addView(t,a);TextView e=text(eyebrow,10,muted,true);e.setLetterSpacing(.1f);r.addView(e);top(content,r,26);}
    private void quickLink(String icon,String title,String subtitle,String target){
        LinearLayout card=row();card.setBackground(bordered(white,18,line));pad(card,14,14,14,14);top(content,card,10);
        TextView symbol=text(icon,18,accent,true);symbol.setGravity(Gravity.CENTER);symbol.setBackground(shape(tint(accent,.11f),13));card.addView(symbol,lp(42,42));
        LinearLayout words=column();TextView h=text(title,15,ink,true);words.addView(h);TextView s=text(subtitle,12,muted,false);top(words,s,3);LinearLayout.LayoutParams p=lp(0,-2);p.weight=1;p.leftMargin=12;card.addView(words,p);TextView arrow=text("›",25,muted,false);card.addView(arrow);card.setOnClickListener(v->{page=target;showApp();});
    }
    private void emptyState(String icon,String title,String subtitle){
        LinearLayout card=column();card.setGravity(Gravity.CENTER);card.setBackground(bordered(white,22,line));pad(card,24,30,24,30);top(content,card,24);
        TextView mark=text(icon,24,accent,true);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(tint(accent,.11f),24));card.addView(mark,lp(58,58));TextView h=text(title,19,ink,true);h.setGravity(Gravity.CENTER);top(card,h,18);TextView s=text(subtitle,14,muted,false);s.setGravity(Gravity.CENTER);s.setLineSpacing(dp(3),1);top(card,s,7);
    }
    private void profileCard(){LinearLayout c=row();c.setBackground(bordered(white,20,line));pad(c,16,16,16,16);top(content,c,24);TextView av=text(initial(user),18,accent,true);av.setGravity(Gravity.CENTER);av.setBackground(shape(tint(accent,.12f),30));c.addView(av,lp(52,52));LinearLayout info=column();TextView name=text(user,16,ink,true);info.addView(name);TextView clanName=text(clan+"  ·  "+rgbLabel(accent),12,muted,false);top(info,clanName,5);LinearLayout.LayoutParams p=lp(0,-2);p.weight=1;p.leftMargin=13;c.addView(info,p);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}
}
