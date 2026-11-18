package com.max6778.kashafapp;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private final int ink=Color.rgb(35,35,48), muted=Color.rgb(112,112,130), paper=Color.rgb(247,247,251), white=Color.WHITE, line=Color.rgb(232,232,241), peach=Color.rgb(255,236,226);
    private final String[] clans={"Red Clan","Green Clan","Blue Clan"};
    private final String[] clanNotes={"Bold energy","Grow together","Make your mark"};
    private final int[] clanColors={Color.rgb(226,76,88),Color.rgb(42,151,105),Color.rgb(62,112,216)};
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private int accent=Color.rgb(226,76,88), selectedClan=0;
    private String clan="Red Clan", user="", gender="prefer_not_to_say", rank="member", page="Home";
    private LinearLayout root, content;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        auth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();
        getWindow().setStatusBarColor(paper); getWindow().setNavigationBarColor(white);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(auth.getCurrentUser()!=null) loadProfile(); else showAuth(false);
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
        accent=signup?clanColors[selectedClan]:clanColors[0];
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(paper);
        LinearLayout pageView=column();pad(pageView,24,22,24,26);scroll.addView(pageView);setContentView(scroll);
        LinearLayout brand=row();TextView monogram=text("K",18,white,true);monogram.setGravity(Gravity.CENTER);monogram.setBackground(shape(accent,14));brand.addView(monogram,lp(42,42));
        TextView brandName=text("Kashaf",18,ink,true);LinearLayout.LayoutParams bp=lp(-2,-2);bp.leftMargin=12;brand.addView(brandName,bp);pageView.addView(brand);
        TextView eyebrow=kicker("A place for your people");top(pageView,eyebrow,35);
        TextView title=text(signup?"Find your\npeople.":"Good to see\nyou again.",36,ink,true);title.setLineSpacing(dp(1),1);top(pageView,title,10);
        TextView subtitle=text(signup?"Create your account and pick a clan to make this space yours.":"Sign in to pick up where you left off.",15,muted,false);subtitle.setLineSpacing(dp(4),1);top(pageView,subtitle,10);
        EditText name=null;Spinner genderChoice=null;
        if(signup){name=field("Name","Your name");top(pageView,name,24);}
        EditText email=field("Email","you@example.com");email.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);top(pageView,email,signup?12:28);
        if(signup && auth.getCurrentUser()!=null && auth.getCurrentUser().getEmail()!=null) email.setText(auth.getCurrentUser().getEmail());
        EditText pass=field("Password","Your password");pass.setInputType(129);top(pageView,pass,12);
        TextView[] submitRef=new TextView[1];
        if(signup){
            TextView genderLabel=text("Gender",15,ink,true);top(pageView,genderLabel,18);
            genderChoice=new Spinner(this);String[] genderOptions={"Choose gender","Female","Male","Non-binary","Prefer not to say"};
            ArrayAdapter<String> genderAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,genderOptions);genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);genderChoice.setAdapter(genderAdapter);genderChoice.setBackground(bordered(white,15,line));pad(genderChoice,12,4,12,4);top(pageView,genderChoice,7);
            TextView choose=text("Choose your clan",16,ink,true);top(pageView,choose,20);
            TextView detail=text("Your clan sets the accent color throughout the app.",13,muted,false);top(pageView,detail,4);
            LinearLayout options=column();top(pageView,options,12);LinearLayout[] clanRows=new LinearLayout[clans.length];
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
        final EditText signupName=name;final Spinner signupGender=genderChoice;
        submit.setOnClickListener(v->{String e=email.getText().toString().trim(),p=pass.getText().toString();if(e.isEmpty()||p.isEmpty()||(signup&&(signupName==null||signupName.getText().toString().trim().isEmpty()))){toast("Enter your name, email, and password.");return;}if(p.length()<6){toast("Use a password with at least 6 characters.");return;}
            if(signup){if(signupGender.getSelectedItemPosition()==0){toast("Choose a gender option, or select ‘Prefer not to say’.");return;}String n=signupName.getText().toString().trim();String g=genderValue(signupGender.getSelectedItemPosition());createAccount(n,e,p,g,selectedClan);}
            else auth.signInWithEmailAndPassword(e,p).addOnCompleteListener(this,task->{if(task.isSuccessful())loadProfile();else toast("Sign in failed. Check your email and password.");});});
        TextView toggle=text(signup?"Already have an account?  Sign in":"New to Kashaf?  Create an account",14,accent,true);toggle.setGravity(Gravity.CENTER);top(pageView,toggle,12);pad(toggle,8,12,8,12);toggle.setOnClickListener(v->showAuth(!signup));
        if(!signup){TextView demo=text("Your password is handled securely by Firebase.",12,muted,false);demo.setGravity(Gravity.CENTER);top(pageView,demo,12);}
    }
    private String genderValue(int position){String[] values={"","female","male","non-binary","prefer_not_to_say"};return values[position];}
    private String clanId(int index){return new String[]{"red","green","blue"}[index];}
    private int clanIndex(String id){if("green".equals(id))return 1;if("blue".equals(id))return 2;return 0;}
    private void createAccount(String name,String email,String password,String genderValue,int clanPosition){
        FirebaseUser current=auth.getCurrentUser();
        if(current!=null && email.equalsIgnoreCase(current.getEmail())){saveProfile(current,name,genderValue,clanPosition);return;}
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this,task->{if(!task.isSuccessful()){toast("Could not create account. Check the email and try another password.");return;}saveProfile(auth.getCurrentUser(),name,genderValue,clanPosition);});
    }
    private void saveProfile(FirebaseUser account,String name,String genderValue,int clanPosition){
        if(account==null){toast("Please sign in again.");return;}
        Map<String,Object> profile=new HashMap<>();profile.put("name",name);profile.put("gender",genderValue);profile.put("clan",clanId(clanPosition));profile.put("rank","member");profile.put("createdAt",FieldValue.serverTimestamp());
        db.collection("users").document(account.getUid()).set(profile).addOnSuccessListener(unused->{user=name;gender=genderValue;clan=clans[clanPosition];rank="member";accent=clanColors[clanPosition];page="Home";showApp();}).addOnFailureListener(error->{toast("Account exists, but the profile could not be saved. Check Firestore rules and try again.");showAuth(true);});
    }
    private void loadProfile(){
        FirebaseUser account=auth.getCurrentUser();if(account==null){showAuth(false);return;}
        db.collection("users").document(account.getUid()).get().addOnSuccessListener(snapshot->{if(!snapshot.exists()){toast("Finish setting up your profile.");showAuth(true);return;}user=snapshot.getString("name");gender=snapshot.getString("gender");rank=snapshot.getString("rank");clan=clanName(snapshot.getString("clan"));accent=clanColors[clanIndex(snapshot.getString("clan"))];page="Home";showApp();}).addOnFailureListener(error->{toast("Could not load your profile. Check your connection and Firestore rules.");showAuth(false);});
    }
    private String clanName(String id){return clans[clanIndex(id)];}
    private EditText field(String label,String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(15);e.setTextColor(ink);e.setHintTextColor(muted);e.setHint(hint);e.setPadding(dp(16),0,dp(16),0);e.setBackground(bordered(white,15,line));e.setContentDescription(label);return e;}
    private int tint(int color,float factor){return Color.rgb((int)(Color.red(color)*factor+255*(1-factor)),(int)(Color.green(color)*factor+255*(1-factor)),(int)(Color.blue(color)*factor+255*(1-factor)));}

    private void showApp(){
        root=column();root.setBackgroundColor(paper);setContentView(root);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=column();pad(content,22,24,22,26);scroll.addView(content);renderPage();
        LinearLayout nav=row();nav.setGravity(Gravity.CENTER);nav.setBackgroundColor(white);nav.setElevation(dp(12));String[] tabs={"Home","Chat","News","Profile"};String[] icons={"⌂","◉","▤","●"};
        for(int i=0;i<tabs.length;i++){final String tab=tabs[i];boolean active=page.equals(tab);LinearLayout item=column();item.setGravity(Gravity.CENTER);if(active)item.setBackground(shape(tint(accent,.10f),16));TextView icon=text(icons[i],20,active?accent:muted,true);icon.setGravity(Gravity.CENTER);item.addView(icon);TextView caption=text(tabs[i],10,active?accent:muted,active);caption.setGravity(Gravity.CENTER);top(item,caption,2);nav.addView(item,new LinearLayout.LayoutParams(0,dp(58),1));item.setOnClickListener(v->{page=tab;showApp();});}
        pad(nav,8,5,8,5);root.addView(nav,lp(-1,68));
    }
    private void renderPage(){content.removeAllViews();
        if(page.equals("Home")){
            LinearLayout top=row();TextView brand=kicker("Kashaf  /  Home");LinearLayout.LayoutParams grow=lp(0,-2);grow.weight=1;top.addView(brand,grow);TextView avatar=text(initial(user),15,accent,true);avatar.setGravity(Gravity.CENTER);avatar.setBackground(shape(tint(accent,.12f),22));top.addView(avatar,lp(42,42));content.addView(top);
            TextView hello=text("Hello, "+user+" ✨",29,ink,true);top(content,hello,25);TextView sub=text("Your community is right here.",15,muted,false);top(content,sub,5);
            clanHero();
            sectionLabel("Your space","EXPLORE");
            quickLink("◉","Clan chat","Catch up with your people","Chat");quickLink("▤","Announcements","Updates from your community","News");
        }else if(page.equals("News")){pageHeader("Announcements","Updates from the whole community.");announcementPreview();}
        else if(page.equals("Chat")){pageHeader("Your conversations","Pick a room and join in.");roomCard("✦","Global lounge","Everyone across Kashaf","ALL CLANS",ink);roomCard("●",clan,"Your clan’s private space",clan.toUpperCase(),accent);}
        else{pageHeader("Your profile","Your Kashaf space, your way.");profileCard();TextView signout=text("Sign out",15,accent,true);signout.setGravity(Gravity.CENTER);signout.setBackground(bordered(white,16,line));top(content,signout,14);pad(signout,16,16,16,16);signout.setOnClickListener(v->{auth.signOut();page="Home";showAuth(false);});}
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
    private void roomCard(String icon,String title,String subtitle,String tag,int color){
        LinearLayout card=row();card.setBackground(bordered(white,20,line));pad(card,16,16,16,16);top(content,card,12);
        TextView mark=text(icon,20,color,true);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(tint(color,.10f),15));card.addView(mark,lp(48,48));
        LinearLayout words=column();TextView name=text(title,16,ink,true);words.addView(name);TextView detail=text(subtitle,12,muted,false);top(words,detail,4);LinearLayout.LayoutParams p=lp(0,-2);p.weight=1;p.leftMargin=13;card.addView(words,p);
        TextView badge=text(tag,9,color,true);badge.setLetterSpacing(.05f);badge.setBackground(shape(tint(color,.10f),20));pad(badge,9,7,9,7);card.addView(badge);
    }
    private void announcementPreview(){
        LinearLayout card=column();card.setBackground(bordered(white,22,line));pad(card,20,20,20,20);top(content,card,22);
        TextView badge=text("COMMUNITY UPDATE",10,accent,true);badge.setLetterSpacing(.1f);card.addView(badge);
        TextView title=text("A new space for every clan",19,ink,true);top(card,title,12);
        TextView body=text("Announcements will appear here when an admin posts an update. Check back soon.",14,muted,false);body.setLineSpacing(dp(4),1);top(card,body,7);
        TextView foot=text("KASHAF  ·  ALL CLANS",10,muted,true);foot.setLetterSpacing(.08f);top(card,foot,18);
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
    private void profileCard(){LinearLayout c=row();c.setBackground(bordered(white,20,line));pad(c,16,16,16,16);top(content,c,24);TextView av=text(initial(user),18,accent,true);av.setGravity(Gravity.CENTER);av.setBackground(shape(tint(accent,.12f),30));c.addView(av,lp(52,52));LinearLayout info=column();TextView name=text(user,16,ink,true);info.addView(name);TextView clanName=text(clan+"  ·  "+rank+"  ·  "+gender,12,muted,false);top(info,clanName,5);LinearLayout.LayoutParams p=lp(0,-2);p.weight=1;p.leftMargin=13;c.addView(info,p);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}
}
