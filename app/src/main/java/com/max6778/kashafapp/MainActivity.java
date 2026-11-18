package com.max6778.kashafapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.net.Uri;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.text.InputFilter;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

public class MainActivity extends Activity {
    private static final int PICK_CHAT_IMAGE = 7104;
    private static final int MAX_MEDIA_BYTES = 700 * 1024;
    private static final int MAX_SOURCE_BYTES = 12 * 1024 * 1024;
    private final int ink=Color.rgb(28,45,66), muted=Color.rgb(112,112,130), paper=Color.rgb(247,249,247), white=Color.WHITE, line=Color.rgb(228,234,229);
    private final int navy=Color.rgb(20,38,61), forest=Color.rgb(39,111,76);
    private final String branchArabic="فرع السلام", organizationArabic="الكشفية الحسنية المغربية";
    private final String[] classIds={"cubs","wolf_cubs_brownies","scouts_guides","senior_scouts_guides","rovers_rangers"};
    private final String[] classArabic={"البراعم","الأشبال والزهرات","الكشافة والمرشدات","الكشاف المتقدم والرائدات","الجوالة والدليلات"};
    private final String[] classEnglish={"Cubs","Wolf Cubs & Brownies","Scouts & Guides","Senior Scouts & Senior Guides","Rovers & Rangers"};
    private final String[] classAges={"5–7 years","7–12 years","12–15 years","15–18 years","18–23 years"};
    private final String[] classTaglines={"اكتشاف ومتعة","معًا نتعلّم وننمو","خدمة ومهارة","قيادة ومبادرة","مسؤولية وخدمة المجتمع"};
    private final String[] classEnglishTaglines={"Discover and play","Learn and grow together","Service and skills","Leadership and initiative","Responsibility and community service"};
    private final int[] classColors={Color.rgb(211,166,55),Color.rgb(45,137,86),Color.rgb(52,112,190),Color.rgb(128,83,166),Color.rgb(202,61,68)};
    private final int[] classLogos={R.drawable.class_cubs,R.drawable.class_wolf_cubs,R.drawable.class_scouts_guides,R.drawable.class_senior,R.drawable.class_rovers};
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private int accent=Color.rgb(20,38,61), selectedClass=-1;
    private String scoutClassId="", user="", emailAddress="", gender="prefer_not_to_say", rank="member", page="Home";
    private boolean legacyClassSetup=false;
    private String currentRoomId="global", currentRoomTitle="Global lounge";
    private boolean isAdmin=false;
    private ListenerRegistration chatListener, announcementListener;
    private LinearLayout root, content, roomMessages;
    private EditText messageInput;

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
        accent=signup&&selectedClass>=0?classColors[selectedClass]:navy;
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(paper);
        LinearLayout pageView=column();pad(pageView,24,22,24,26);scroll.addView(pageView);setContentView(scroll);
        LinearLayout brand=column();TextView brandName=text(organizationArabic,19,navy,true);brand.addView(brandName);TextView branch=text(branchArabic+"  ·  Kashfia Alhassania – Far3 Salam",12,forest,true);top(brand,branch,4);pageView.addView(brand);
        TextView eyebrow=kicker("كشافة فرع السلام  /  Far3 Salam Scouts");top(pageView,eyebrow,28);
        TextView title=text(signup?"انضم إلى فرع السلام\nJoin our scout community":"مرحبًا بعودتك\nWelcome back",30,ink,true);title.setLineSpacing(dp(2),1);top(pageView,title,10);
        TextView subtitle=text(signup?"أنشئ حسابك واختر فئتك الكشفية.\nCreate your account and choose your scout class.":"سجّل الدخول إلى مجتمعك الكشفي.\nSign in to your scout community.",14,muted,false);subtitle.setLineSpacing(dp(4),1);top(pageView,subtitle,10);
        EditText name=null;Spinner genderChoice=null;
        FirebaseUser signedIn=auth.getCurrentUser();
        if(signup){name=field("الاسم | Name","اسمك | Your name");if(legacyClassSetup&&user!=null)name.setText(user);top(pageView,name,22);}
        EditText email=field("البريد الإلكتروني | Email","you@example.com");email.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);top(pageView,email,signup?10:24);
        if(signup&&signedIn!=null&&signedIn.getEmail()!=null){email.setText(signedIn.getEmail());email.setEnabled(false);}
        EditText pass=null;if(signedIn==null){pass=field("كلمة المرور | Password","كلمة المرور | Password");pass.setInputType(129);top(pageView,pass,10);}
        TextView[] submitRef=new TextView[1];
        if(signup){
            TextView genderLabel=text("الجنس | Gender",15,ink,true);top(pageView,genderLabel,16);
            genderChoice=new Spinner(this);String[] genderOptions={"اختر الجنس | Choose gender","أنثى | Female","ذكر | Male","غير ثنائي | Non-binary","أفضل عدم الإفصاح | Prefer not to say"};
            ArrayAdapter<String> genderAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,genderOptions);genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);genderChoice.setAdapter(genderAdapter);genderChoice.setBackground(bordered(white,15,line));pad(genderChoice,12,4,12,4);
            int genderPos=genderPosition(gender);if((legacyClassSetup||signedIn!=null)&&genderPos>0)genderChoice.setSelection(genderPos);top(pageView,genderChoice,7);
            TextView choose=text("الفئة الكشفية | Choose your scout class",18,navy,true);top(pageView,choose,20);
            TextView detail=text("اختر فئتك لتحديد لونك ودردشتك. | Your class sets your color and private chat.",12,muted,false);top(pageView,detail,4);
            LinearLayout options=column();top(pageView,options,11);LinearLayout[] classRows=new LinearLayout[classIds.length];
            for(int i=0;i<classIds.length;i++){
                final int index=i;LinearLayout option=row();classRows[i]=option;option.setBackground(bordered(white,17,i==selectedClass?classColors[i]:line));pad(option,12,10,12,10);
                ImageView logo=new ImageView(this);logo.setImageResource(classLogos[i]);logo.setScaleType(ImageView.ScaleType.CENTER_CROP);option.addView(logo,lp(48,48));
                LinearLayout labels=column();TextView titleText=text(classArabic[i],15,ink,true);labels.addView(titleText);TextView english=text(classEnglish[i]+" · "+classAges[i],11,muted,false);top(labels,english,2);TextView note=text(classTaglines[i]+"  ·  "+classEnglishTaglines[i],10,classColors[i],true);top(labels,note,2);
                LinearLayout.LayoutParams op=lp(0,-2);op.weight=1;op.leftMargin=11;option.addView(labels,op);
                View dot=new View(this);dot.setBackground(shape(classColors[i],16));LinearLayout.LayoutParams dotParams=lp(12,12);dotParams.leftMargin=7;option.addView(dot,dotParams);
                options.addView(option,lp(-1,-2));LinearLayout.LayoutParams sep=lp(-1,0);sep.topMargin=dp(7);options.addView(new View(this),sep);
                option.setOnClickListener(v->{selectedClass=index;accent=classColors[index];for(int j=0;j<classRows.length;j++)classRows[j].setBackground(bordered(white,17,j==selectedClass?classColors[j]:line));if(submitRef[0]!=null)submitRef[0].setBackground(shape(accent,16));});
            }
            TextView memberRank=text("الرتبة عند التسجيل: عضو | Starting rank: Member",12,muted,true);top(pageView,memberRank,12);
        }
        TextView submit=text(signup?(signedIn!=null?"إكمال الحساب | Finish setup":"إنشاء الحساب | Create account"):"تسجيل الدخول | Sign in",14,white,true);submit.setGravity(Gravity.CENTER);submit.setBackground(shape(accent,16));top(pageView,submit,20);pad(submit,18,16,18,16);if(signup)submitRef[0]=submit;
        final EditText signupName=name;final Spinner signupGender=genderChoice;final EditText passwordField=pass;
        submit.setOnClickListener(v->{String e=email.getText().toString().trim(),p=passwordField==null?"":passwordField.getText().toString();if(e.isEmpty()||(passwordField!=null&&p.isEmpty())||(signup&&(signupName==null||signupName.getText().toString().trim().isEmpty()))){toast("أكمل الحقول المطلوبة | Complete the required fields.");return;}if(passwordField!=null&&p.length()<6){toast("Use a password with at least 6 characters.");return;}
            if(signup){if(signupGender.getSelectedItemPosition()==0){toast("اختر الجنس | Choose a gender option.");return;}if(selectedClass<0){toast("اختر فئتك الكشفية أولًا | Choose a scout class first.");return;}String n=signupName.getText().toString().trim(),g=genderValue(signupGender.getSelectedItemPosition());createAccount(n,e,p,g,selectedClass);}
            else auth.signInWithEmailAndPassword(e,p).addOnCompleteListener(this,task->{if(task.isSuccessful())loadProfile();else toast("Sign in failed. Check your email and password.");});});
        if(signedIn==null){TextView toggle=text(signup?"لديك حساب؟ تسجيل الدخول | Already registered? Sign in":"مستخدم جديد؟ إنشاء حساب | New member? Create account",13,accent,true);toggle.setGravity(Gravity.CENTER);top(pageView,toggle,12);pad(toggle,8,12,8,12);toggle.setOnClickListener(v->{selectedClass=-1;showAuth(!signup);});}
        if(!signup){TextView hint=text("فرع السلام · الكشفية الحسنية المغربية",12,forest,true);hint.setGravity(Gravity.CENTER);top(pageView,hint,14);}
    }
    private String genderValue(int position){String[] values={"","female","male","non-binary","prefer_not_to_say"};return values[position];}
    private int genderPosition(String value){if("female".equals(value))return 1;if("male".equals(value))return 2;if("non-binary".equals(value))return 3;if("prefer_not_to_say".equals(value))return 4;return 0;}
    private String classId(int index){return classIds[index];}
    private int classIndex(String id){for(int i=0;i<classIds.length;i++)if(classIds[i].equals(id))return i;return 0;}
    private String genderLabel(String value){if("female".equals(value))return "أنثى | Female";if("male".equals(value))return "ذكر | Male";if("non-binary".equals(value))return "غير ثنائي | Non-binary";return "أفضل عدم الإفصاح | Prefer not to say";}
    private void createAccount(String name,String email,String password,String genderValue,int classPosition){
        FirebaseUser current=auth.getCurrentUser();
        if(current!=null&&email.equalsIgnoreCase(current.getEmail())){saveProfile(current,name,email,genderValue,classPosition);return;}
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this,task->{if(!task.isSuccessful()){toast("Could not create account. Check the email and try another password.");return;}saveProfile(auth.getCurrentUser(),name,email,genderValue,classPosition);});
    }
    private void saveProfile(FirebaseUser account,String name,String email,String genderValue,int classPosition){
        if(account==null){toast("Please sign in again.");return;}
        String verifiedEmail=account.getEmail()==null?email:account.getEmail();Map<String,Object> profile=new HashMap<>();profile.put("name",name);profile.put("email",verifiedEmail);profile.put("gender",genderValue);profile.put("scoutClass",classId(classPosition));
        DocumentReference ref=db.collection("users").document(account.getUid());
        if(legacyClassSetup){profile.put("clan",FieldValue.delete());ref.update(profile).addOnSuccessListener(unused->{legacyClassSetup=false;finishProfile(name,verifiedEmail,genderValue,classPosition,rank);}).addOnFailureListener(error->{toast("Could not save your class. Publish the updated Firestore rules and try again.");showAuth(true);});}
        else{profile.put("rank","member");profile.put("createdAt",FieldValue.serverTimestamp());ref.set(profile).addOnSuccessListener(unused->finishProfile(name,verifiedEmail,genderValue,classPosition,"member")).addOnFailureListener(error->{toast("Account exists, but the profile could not be saved. Check Firestore rules and try again.");showAuth(true);});}
    }
    private void finishProfile(String name,String email,String genderValue,int classPosition,String savedRank){user=name;emailAddress=email;gender=genderValue;scoutClassId=classId(classPosition);rank=savedRank;accent=classColors[classPosition];page="Home";loadAdminStatus();}
    private void loadProfile(){
        FirebaseUser account=auth.getCurrentUser();if(account==null){showAuth(false);return;}
        db.collection("users").document(account.getUid()).get().addOnSuccessListener(snapshot->{if(!snapshot.exists()){legacyClassSetup=false;user="";gender="prefer_not_to_say";selectedClass=-1;toast("Finish setting up your profile.");showAuth(true);return;}
            user=snapshot.getString("name")==null?"Scout":snapshot.getString("name");emailAddress=snapshot.getString("email")==null?account.getEmail():snapshot.getString("email");gender=snapshot.getString("gender")==null?"prefer_not_to_say":snapshot.getString("gender");rank=snapshot.getString("rank")==null?"member":snapshot.getString("rank");scoutClassId=snapshot.getString("scoutClass");
            if(scoutClassId==null||!java.util.Arrays.asList(classIds).contains(scoutClassId)){legacyClassSetup=true;selectedClass=-1;toast("Choose your scout class to finish setting up your profile.");showAuth(true);return;}
            legacyClassSetup=false;accent=classColors[classIndex(scoutClassId)];page="Home";loadAdminStatus();}).addOnFailureListener(error->{toast("Could not load your profile. Check your connection and Firestore rules.");showAuth(false);});
    }
    private void loadAdminStatus(){
        FirebaseUser account=auth.getCurrentUser();if(account==null){isAdmin=false;showApp();return;}
        db.collection("admins").document(account.getUid()).get().addOnSuccessListener(doc->{isAdmin=doc.exists();showApp();}).addOnFailureListener(error->{isAdmin=false;showApp();});
    }
    private void clearListeners(){if(chatListener!=null){chatListener.remove();chatListener=null;}if(announcementListener!=null){announcementListener.remove();announcementListener=null;}}
    @Override protected void onDestroy(){clearListeners();super.onDestroy();}

    private EditText field(String label,String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(15);e.setTextColor(ink);e.setHintTextColor(muted);e.setHint(hint);e.setPadding(dp(16),0,dp(16),0);e.setBackground(bordered(white,15,line));e.setContentDescription(label);return e;}
    private int tint(int color,float factor){return Color.rgb((int)(Color.red(color)*factor+255*(1-factor)),(int)(Color.green(color)*factor+255*(1-factor)),(int)(Color.blue(color)*factor+255*(1-factor)));}

    private void showApp(){
        clearListeners();
        root=column();root.setBackgroundColor(paper);setContentView(root);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=column();pad(content,22,24,22,26);scroll.addView(content);renderPage();
        LinearLayout nav=row();nav.setGravity(Gravity.CENTER);nav.setBackgroundColor(white);nav.setElevation(dp(12));String[] tabs={"Home","Chat","News","Profile"};String[] icons={"⌂","◉","▤","●"};String[] labels={"الرئيسية\nHome","الدردشة\nChat","الإعلانات\nNews","ملفي\nProfile"};
        for(int i=0;i<tabs.length;i++){final String tab=tabs[i];boolean active=page.equals(tab);LinearLayout item=column();item.setGravity(Gravity.CENTER);if(active)item.setBackground(shape(tint(accent,.10f),16));TextView icon=text(icons[i],19,active?accent:muted,true);icon.setGravity(Gravity.CENTER);item.addView(icon);TextView caption=text(labels[i],9,active?accent:muted,active);caption.setGravity(Gravity.CENTER);caption.setLineSpacing(0,0.9f);top(item,caption,1);nav.addView(item,new LinearLayout.LayoutParams(0,dp(60),1));item.setOnClickListener(v->{page=tab;showApp();});}
        pad(nav,8,5,8,5);root.addView(nav,lp(-1,68));
    }
    private void renderPage(){content.removeAllViews();
        if(page.equals("Home")){
            LinearLayout top=row();LinearLayout brand=column();TextView name=text(organizationArabic,15,navy,true);brand.addView(name);TextView branch=text(branchArabic+" · Far3 Salam",11,forest,true);top(brand,branch,2);LinearLayout.LayoutParams grow=lp(0,-2);grow.weight=1;top.addView(brand,grow);TextView avatar=text(initial(user),15,accent,true);avatar.setGravity(Gravity.CENTER);avatar.setBackground(shape(tint(accent,.12f),22));top.addView(avatar,lp(42,42));content.addView(top);
            TextView hello=text("مرحبًا، "+user+" ✨",25,ink,true);top(content,hello,22);TextView sub=text("أهلًا بك في مجتمع فرع السلام الكشفي | Welcome to Far3 Salam Scouts.",13,muted,false);top(content,sub,5);
            classHero();sectionLabel("مساحتك الكشفية | Your scout space","FAR3 SALAM");
            quickLink("◉","دردشة الفئة | Class chat","انضم إلى دردشة فئتك | Talk with your class","Chat");quickLink("▤","إعلانات الفئة | Class announcements","أخبار فرع السلام وفئتك | Branch and class news","News");
        }else if(page.equals("News")){pageHeader("الإعلانات | Announcements","آخر أخبار فرع السلام الكشفي | Updates from Far3 Salam.");if(isAdmin)adminAnnouncementButton();LinearLayout feed=column();top(content,feed,16);listenAnnouncements(feed);}
        else if(page.equals("Chat")){
            pageHeader("الدردشة | Chats","اختر مساحة للحديث | Choose a room.");roomCard("✦","الصالة العامة","Global lounge","الجميع | ALL",navy,"global",-1);
            if(isAdmin){for(int i=0;i<classIds.length;i++)roomCard("●",classArabic[i]+" السلام",classEnglish[i]+" · "+classAges[i],"ADMIN",classColors[i],classId(i),i);}
            else{int idx=classIndex(scoutClassId);roomCard("●",classArabic[idx]+" السلام",classEnglish[idx]+" · "+classAges[idx],"فئتي | CLASS",classColors[idx],classId(idx),idx);}
        }else if(page.equals("Room")){roomPage();}
        else{pageHeader("ملفي الشخصي | Profile","فرع السلام · الكشفية الحسنية المغربية");profileCard();TextView signout=text("تسجيل الخروج | Sign out",14,accent,true);signout.setGravity(Gravity.CENTER);signout.setBackground(bordered(white,16,line));top(content,signout,14);pad(signout,16,16,16,16);signout.setOnClickListener(v->{auth.signOut();legacyClassSetup=false;page="Home";showAuth(false);});}
    }
    private String initial(String value){return value==null||value.isEmpty()?"ك":value.substring(0,1).toUpperCase();}
    private void pageHeader(String title,String subtitle){TextView branch=text(organizationArabic+" · "+branchArabic,12,forest,true);content.addView(branch);TextView eyebrow=kicker("FAR3 SALAM  /  "+page);top(content,eyebrow,7);TextView h=text(title,24,ink,true);top(content,h,14);TextView s=text(subtitle,13,muted,false);top(content,s,5);}
    private void classHero(){
        int idx=classIndex(scoutClassId);LinearLayout card=column();card.setBackground(shape(navy,24));pad(card,18,18,18,18);top(content,card,22);
        LinearLayout head=row();ImageView logo=new ImageView(this);logo.setImageResource(classLogos[idx]);logo.setScaleType(ImageView.ScaleType.CENTER_CROP);card.addView(logo,lp(64,64));LinearLayout details=column();LinearLayout.LayoutParams d=lp(0,-2);d.weight=1;d.leftMargin=14;head.addView(details,d);TextView badge=text("فئتك الكشفية | YOUR CLASS",10,classColors[idx],true);details.addView(badge);TextView title=text(classArabic[idx],20,white,true);top(details,title,5);TextView english=text(classEnglish[idx]+" · "+classAges[idx],11,Color.rgb(225,231,239),false);top(details,english,3);card.addView(head);
        TextView desc=text(classTaglines[idx]+" · "+classEnglishTaglines[idx],13,white,true);top(card,desc,14);TextView branch=text("الكشفية الحسنية المغربية · فرع السلام",11,Color.rgb(225,231,239),false);top(card,branch,5);
    }
    private void roomCard(String icon,String title,String subtitle,String tag,int color,String roomId,int classIndex){
        LinearLayout card=row();card.setBackground(bordered(white,20,line));pad(card,16,16,16,16);top(content,card,12);
        if(classIndex>=0){ImageView mark=new ImageView(this);mark.setImageResource(classLogos[classIndex]);mark.setScaleType(ImageView.ScaleType.CENTER_CROP);card.addView(mark,lp(48,48));}else{TextView mark=text(icon,20,color,true);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(tint(color,.10f),15));card.addView(mark,lp(48,48));}
        LinearLayout words=column();TextView name=text(title,16,ink,true);words.addView(name);TextView detail=text(subtitle,12,muted,false);top(words,detail,4);LinearLayout.LayoutParams p=lp(0,-2);p.weight=1;p.leftMargin=13;card.addView(words,p);
        TextView badge=text(tag,9,color,true);badge.setLetterSpacing(.05f);badge.setBackground(shape(tint(color,.10f),20));pad(badge,9,7,9,7);card.addView(badge);
        card.setOnClickListener(v->{currentRoomId=roomId;currentRoomTitle=title;page="Room";showApp();});
    }
    private void roomPage(){
        TextView back=text("‹  All rooms",14,accent,true);top(content,back,4);back.setOnClickListener(v->{page="Chat";showApp();});
        pageHeader(currentRoomTitle,currentRoomId.equals("global")?"الصالة العامة متاحة لجميع الأعضاء | Open to all members.":"دردشة خاصة بأعضاء هذه الفئة | Private to this class.");
        ScrollView messagesScroll=new ScrollView(this);messagesScroll.setFillViewport(false);LinearLayout.LayoutParams scrollParams=lp(-1,320);scrollParams.topMargin=dp(16);content.addView(messagesScroll,scrollParams);
        roomMessages=column();messagesScroll.addView(roomMessages);
        TextView limits=text("Messages: 1,000 characters max  ·  Images/GIFs: 700 KB max",11,muted,false);top(content,limits,9);
        LinearLayout composer=row();top(content,composer,12);messageInput=field("Message","Write a message...");messageInput.setSingleLine(true);messageInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1000)});LinearLayout.LayoutParams inputParams=lp(0,52);inputParams.weight=1;composer.addView(messageInput,inputParams);
        TextView attach=text("＋",22,accent,true);attach.setGravity(Gravity.CENTER);attach.setBackground(bordered(white,14,line));LinearLayout.LayoutParams attachParams=lp(48,48);attachParams.leftMargin=6;composer.addView(attach,attachParams);attach.setOnClickListener(v->pickChatImage());
        TextView send=text("Send",14,white,true);send.setGravity(Gravity.CENTER);send.setBackground(shape(accent,14));pad(send,16,14,16,14);LinearLayout.LayoutParams sendParams=lp(-2,48);sendParams.leftMargin=8;composer.addView(send,sendParams);send.setOnClickListener(v->sendMessage());
        listenMessages(messagesScroll);
    }
    private void listenMessages(ScrollView messagesScroll){
        chatListener=db.collection("rooms").document(currentRoomId).collection("messages").orderBy("createdAt",Query.Direction.DESCENDING).limit(60).addSnapshotListener((snapshot,error)->{
            if(error!=null){toast("Could not load messages. Check your Firestore rules.");return;}
            if(snapshot==null||roomMessages==null)return;roomMessages.removeAllViews();
            if(snapshot.isEmpty()){String message=currentRoomId.equals("global")?"لا توجد رسائل بعد | Say hello!":"انضم إلى دردشة فئتك | Start chatting with your class.";TextView empty=text(message,14,muted,false);empty.setGravity(Gravity.CENTER);pad(empty,12,28,12,28);roomMessages.addView(empty);return;}
            String myUid=auth.getCurrentUser()==null?"":auth.getCurrentUser().getUid();
            for(int i=snapshot.size()-1;i>=0;i--){
                DocumentSnapshot doc=snapshot.getDocuments().get(i);String author=doc.getString("authorName"),body=doc.getString("text"),authorId=doc.getString("authorId");
                LinearLayout bubble=column();boolean mine=myUid.equals(authorId);bubble.setBackground(bordered(mine?ink:white,16,line));pad(bubble,13,10,13,10);
                TextView sender=text(mine?"You":(author==null?"Member":author),11,mine?white:accent,true);bubble.addView(sender);
                Blob media=doc.getBlob("mediaData");String mediaType=doc.getString("mediaType");
                if(media!=null&&mediaType!=null){View preview=mediaPreview(media.toBytes(),mediaType);LinearLayout.LayoutParams previewParams=lp(-1,220);previewParams.topMargin=dp(7);bubble.addView(preview,previewParams);}
                if(body!=null&&!body.isEmpty()){TextView message=text(body,14,mine?white:ink,false);message.setLineSpacing(dp(3),1);top(bubble,message,4);}
                LinearLayout.LayoutParams bubbleParams=lp(-1,-2);bubbleParams.topMargin=dp(8);roomMessages.addView(bubble,bubbleParams);
            }
            messagesScroll.post(()->messagesScroll.fullScroll(View.FOCUS_DOWN));
        });
    }
    private void sendMessage(){
        FirebaseUser account=auth.getCurrentUser();String body=messageInput==null?"":messageInput.getText().toString().trim();
        if(account==null||body.isEmpty())return;if(body.length()>1000){toast("Messages must be 1,000 characters or less.");return;}
        Map<String,Object> message=new HashMap<>();message.put("authorId",account.getUid());message.put("authorName",user);message.put("text",body);message.put("createdAt",FieldValue.serverTimestamp());
        db.collection("rooms").document(currentRoomId).collection("messages").add(message).addOnSuccessListener(ref->{if(messageInput!=null)messageInput.setText("");}).addOnFailureListener(error->toast("Message could not be sent. Check your class access and Firestore rules."));
    }
    private void pickChatImage(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/*");try{startActivityForResult(intent,PICK_CHAT_IMAGE);}catch(Exception error){toast("Could not open your image picker.");}}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);if(requestCode!=PICK_CHAT_IMAGE||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();String mime=getContentResolver().getType(uri);if(mime==null||!mime.startsWith("image/")){toast("Choose an image or GIF.");return;}
        String caption=messageInput==null?"":messageInput.getText().toString().trim();FirebaseUser account=auth.getCurrentUser();if(account==null)return;toast("Preparing image…");
        new Thread(()->{try{
            byte[] bytes;String storedType;try(InputStream input=getContentResolver().openInputStream(uri)){if(input==null)throw new IllegalArgumentException("Cannot read image");bytes=readLimited(input,MAX_SOURCE_BYTES);}
            if("image/gif".equalsIgnoreCase(mime)){storedType="image/gif";if(bytes.length>MAX_MEDIA_BYTES)throw new IllegalArgumentException("TOO_LARGE");}else{bytes=compressImage(bytes);storedType="image/jpeg";}
            if(bytes.length>MAX_MEDIA_BYTES)throw new IllegalArgumentException("TOO_LARGE");
            Map<String,Object> message=new HashMap<>();message.put("authorId",account.getUid());message.put("authorName",user);message.put("text",caption);message.put("mediaData",Blob.fromBytes(bytes));message.put("mediaType",storedType);message.put("createdAt",FieldValue.serverTimestamp());
            db.collection("rooms").document(currentRoomId).collection("messages").add(message).addOnSuccessListener(ref->{if(messageInput!=null)messageInput.setText("");}).addOnFailureListener(error->toast("Image could not be sent. Check Firestore rules and your connection."));
        }catch(IllegalArgumentException error){runOnUiThread(()->toast("Image is too large. Pick a smaller image or a GIF under 700 KB."));}catch(Exception error){runOnUiThread(()->toast("Could not read or compress that image."));}}).start();
    }
    private byte[] readLimited(InputStream input,int limit)throws Exception{ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count,total=0;while((count=input.read(buffer))!=-1){total+=count;if(total>limit)throw new IllegalArgumentException("SOURCE_TOO_LARGE");output.write(buffer,0,count);}return output.toByteArray();}
    private byte[] compressImage(byte[] source)throws Exception{
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(source,0,source.length,bounds);if(bounds.outWidth<=0||bounds.outHeight<=0)throw new IllegalArgumentException("INVALID_IMAGE");
        int sample=1;while(Math.max(bounds.outWidth/sample,bounds.outHeight/sample)>1600)sample*=2;BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;Bitmap bitmap=BitmapFactory.decodeByteArray(source,0,source.length,options);if(bitmap==null)throw new IllegalArgumentException("INVALID_IMAGE");
        try{ByteArrayOutputStream output=new ByteArrayOutputStream();int quality=82;do{output.reset();bitmap.compress(Bitmap.CompressFormat.JPEG,quality,output);quality-=10;}while(output.size()>MAX_MEDIA_BYTES&&quality>=42);if(output.size()>MAX_MEDIA_BYTES)throw new IllegalArgumentException("TOO_LARGE");return output.toByteArray();}finally{bitmap.recycle();}
    }
    private View mediaPreview(byte[] bytes,String type){if("image/gif".equals(type))return new AnimatedGifView(bytes);ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bitmap!=null)image.setImageBitmap(bitmap);else image.setContentDescription("Attached image");image.setBackground(shape(tint(accent,.10f),12));return image;}
    private class AnimatedGifView extends View{
        private final Movie movie;private final long started=android.os.SystemClock.uptimeMillis();AnimatedGifView(byte[] bytes){super(MainActivity.this);movie=Movie.decodeStream(new ByteArrayInputStream(bytes));setBackground(shape(tint(accent,.10f),12));setContentDescription("Animated GIF");}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(movie==null)return;int duration=movie.duration();if(duration<=0)duration=1000;movie.setTime((int)((android.os.SystemClock.uptimeMillis()-started)%duration));float scale=Math.min((float)getWidth()/movie.width(),(float)getHeight()/movie.height());float left=(getWidth()-movie.width()*scale)/2f,top=(getHeight()-movie.height()*scale)/2f;canvas.save();canvas.translate(left,top);canvas.scale(scale,scale);movie.draw(canvas,0,0);canvas.restore();postInvalidateDelayed(40);}
    }
    private void adminAnnouncementButton(){
        TextView button=text("＋  إعلان جديد | New announcement",14,white,true);button.setGravity(Gravity.CENTER);button.setBackground(shape(accent,16));pad(button,14,14,14,14);top(content,button,18);button.setOnClickListener(v->showAnnouncementDialog());
    }
    private void showAnnouncementDialog(){showAnnouncementDialog("","",0);}
    private void showAnnouncementDialog(String initialTitle,String initialBody,int initialAudience){
        LinearLayout form=column();pad(form,4,4,4,4);EditText title=field("العنوان | Title","عنوان الإعلان | Announcement title");title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});title.setText(initialTitle);form.addView(title);
        EditText body=field("الرسالة | Message","اكتب الإعلان | Write your update...");body.setSingleLine(false);body.setMinLines(4);body.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4000)});body.setText(initialBody);body.setGravity(Gravity.TOP);LinearLayout.LayoutParams bodyParams=lp(-1,-2);bodyParams.topMargin=dp(10);form.addView(body,bodyParams);
        String[] options=new String[1+classIds.length];String[] audiences=new String[1+classIds.length];options[0]="الجميع | All members";audiences[0]="all";for(int i=0;i<classIds.length;i++){options[i+1]=classArabic[i]+" السلام | "+classEnglish[i];audiences[i+1]=classId(i);}
        Spinner audience=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,options);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);audience.setAdapter(adapter);audience.setSelection(Math.max(0,Math.min(initialAudience,audiences.length-1)));audience.setBackground(bordered(white,12,line));LinearLayout.LayoutParams audienceParams=lp(-1,52);audienceParams.topMargin=dp(10);form.addView(audience,audienceParams);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("إعلان فرع السلام | New announcement").setView(form).setNegativeButton("إلغاء | Cancel",null).setPositiveButton("معاينة | Preview",null).create();
        dialog.setOnShowListener(unused->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String t=title.getText().toString().trim(),b=body.getText().toString().trim();if(t.isEmpty()||b.isEmpty()){toast("أضف العنوان والرسالة | Add a title and message first.");return;}int selected=audience.getSelectedItemPosition();dialog.dismiss();showAnnouncementPreview(t,b,audiences[selected],selected);}));dialog.show();
    }
    private String audienceName(String audience){if("all".equals(audience))return "الجميع | All members";for(int i=0;i<classIds.length;i++)if(classIds[i].equals(audience))return classArabic[i]+" السلام | "+classEnglish[i];return "إعلان قديم | Legacy audience";}
    private void showAnnouncementPreview(String title,String body,String audience,int selectedAudience){
        LinearLayout preview=column();pad(preview,18,8,18,8);TextView branch=text("الكشفية الحسنية المغربية · فرع السلام",11,forest,true);preview.addView(branch);TextView target=text("الجمهور | Audience: "+audienceName(audience),12,accent,true);top(preview,target,12);TextView heading=text(title,19,ink,true);top(preview,heading,12);TextView message=text(body,15,muted,false);message.setLineSpacing(dp(3),1);top(preview,message,6);TextView author=text("بواسطة | By "+user,11,muted,false);top(preview,author,14);
        new AlertDialog.Builder(this).setTitle("معاينة الإعلان | Preview").setView(preview).setNegativeButton("رجوع للتعديل | Edit",(d,w)->showAnnouncementDialog(title,body,selectedAudience)).setPositiveButton("نشر | Publish",(d,w)->postAnnouncement(title,body,audience)).show();
    }
    private void postAnnouncement(String title,String body,String audience){
        FirebaseUser account=auth.getCurrentUser();if(account==null||!isAdmin){toast("Only admins can post announcements.");return;}
        Map<String,Object> announcement=new HashMap<>();announcement.put("title",title);announcement.put("body",body);announcement.put("createdBy",account.getUid());announcement.put("createdByName",user);announcement.put("createdAt",FieldValue.serverTimestamp());announcement.put("audience",audience);
        db.collection("announcements").add(announcement).addOnFailureListener(error->toast("Announcement was not published. Check admin access and Firestore rules."));
    }
    private void listenAnnouncements(LinearLayout feed){
        Query query=db.collection("announcements");if(isAdmin)query=query.orderBy("createdAt",Query.Direction.DESCENDING).limit(50);else query=query.whereIn("audience",java.util.Arrays.asList("all",scoutClassId));
        announcementListener=query.addSnapshotListener((snapshot,error)->{
            if(error!=null){toast("Could not load announcements. Check your Firestore rules.");return;}if(snapshot==null)return;feed.removeAllViews();int shown=0;
            ArrayList<DocumentSnapshot> docs=new ArrayList<>(snapshot.getDocuments());Collections.sort(docs,(a,b)->{Timestamp ta=a.getTimestamp("createdAt"),tb=b.getTimestamp("createdAt");if(ta==null)return tb==null?0:1;if(tb==null)return -1;return tb.compareTo(ta);});
            for(DocumentSnapshot doc:docs){
                String audience=doc.getString("audience");if(!isAdmin&&!("all".equals(audience)||scoutClassId.equals(audience)))continue;
                LinearLayout card=column();card.setBackground(bordered(white,20,line));pad(card,18,18,18,18);LinearLayout.LayoutParams cardParams=lp(-1,-2);cardParams.topMargin=dp(10);feed.addView(card,cardParams);
                TextView badge=text(audienceName(audience),11,accent,true);badge.setLetterSpacing(.02f);card.addView(badge);
                TextView title=text(doc.getString("title")==null?"إعلان | Announcement":doc.getString("title"),18,ink,true);top(card,title,10);
                TextView body=text(doc.getString("body")==null?"":doc.getString("body"),14,muted,false);body.setLineSpacing(dp(3),1);top(card,body,6);
                String authorName=doc.getString("createdByName");if(authorName!=null&&!authorName.isEmpty())top(card,text("بواسطة | By "+authorName,11,forest,true),10);
                Timestamp created=doc.getTimestamp("createdAt");if(created!=null){TextView date=text(new SimpleDateFormat("MMM d, yyyy · h:mm a",Locale.getDefault()).format(created.toDate()),10,muted,false);top(card,date,8);}shown++;
            }
            if(shown==0){if(isAdmin)emptyStateIn(feed,"▤","لا توجد إعلانات بعد | No announcements yet","ستظهر إعلانات فرع السلام هنا | Branch announcements will appear here.");else emptyStateIn(feed,"▤","لا توجد إعلانات لفئتك بعد","ستظهر هنا إعلانات فرع السلام والإعلانات الموجهة لفئتك.");}
        });
    }
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
    private void emptyStateIn(LinearLayout parent,String icon,String title,String subtitle){
        LinearLayout card=column();card.setGravity(Gravity.CENTER);card.setBackground(bordered(white,22,line));pad(card,24,30,24,30);
        TextView mark=text(icon,24,accent,true);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(tint(accent,.11f),24));card.addView(mark,lp(58,58));
        TextView heading=text(title,19,ink,true);heading.setGravity(Gravity.CENTER);top(card,heading,18);TextView detail=text(subtitle,14,muted,false);detail.setGravity(Gravity.CENTER);detail.setLineSpacing(dp(3),1);top(card,detail,7);
        LinearLayout.LayoutParams params=lp(-1,-2);params.topMargin=dp(10);parent.addView(card,params);
    }
    private void profileCard(){int idx=classIndex(scoutClassId);LinearLayout c=row();c.setBackground(bordered(white,20,line));pad(c,16,16,16,16);top(content,c,20);ImageView av=new ImageView(this);av.setImageResource(classLogos[idx]);av.setScaleType(ImageView.ScaleType.CENTER_CROP);c.addView(av,lp(58,58));LinearLayout info=column();TextView name=text(user,16,ink,true);info.addView(name);TextView cls=text(classArabic[idx]+" · "+classEnglish[idx],12,classColors[idx],true);top(info,cls,4);TextView meta=text("الرتبة | Rank: "+rank+" · "+genderLabel(gender)+"\n"+(emailAddress==null?"":emailAddress),11,muted,false);top(info,meta,4);LinearLayout.LayoutParams p=lp(0,-2);p.weight=1;p.leftMargin=13;c.addView(info,p);TextView branch=text("الكشفية الحسنية المغربية · فرع السلام",12,forest,true);top(content,branch,12);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}
}
