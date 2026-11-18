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
import com.google.firebase.firestore.*;
import java.util.HashMap;
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
    private final int ink=Color.rgb(35,35,48), muted=Color.rgb(112,112,130), paper=Color.rgb(247,247,251), white=Color.WHITE, line=Color.rgb(232,232,241), peach=Color.rgb(255,236,226);
    private final String[] clans={"Red Clan","Green Clan","Blue Clan"};
    private final String[] clanNotes={"Bold energy","Grow together","Make your mark"};
    private final int[] clanColors={Color.rgb(226,76,88),Color.rgb(42,151,105),Color.rgb(62,112,216)};
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private int accent=Color.rgb(226,76,88), selectedClan=0;
    private String clan="Red Clan", user="", gender="prefer_not_to_say", rank="member", page="Home";
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
        db.collection("users").document(account.getUid()).set(profile).addOnSuccessListener(unused->{user=name;gender=genderValue;clan=clans[clanPosition];rank="member";accent=clanColors[clanPosition];page="Home";loadAdminStatus();}).addOnFailureListener(error->{toast("Account exists, but the profile could not be saved. Check Firestore rules and try again.");showAuth(true);});
    }
    private void loadProfile(){
        FirebaseUser account=auth.getCurrentUser();if(account==null){showAuth(false);return;}
        db.collection("users").document(account.getUid()).get().addOnSuccessListener(snapshot->{if(!snapshot.exists()){toast("Finish setting up your profile.");showAuth(true);return;}user=snapshot.getString("name");gender=snapshot.getString("gender");rank=snapshot.getString("rank");clan=clanName(snapshot.getString("clan"));accent=clanColors[clanIndex(snapshot.getString("clan"))];page="Home";loadAdminStatus();}).addOnFailureListener(error->{toast("Could not load your profile. Check your connection and Firestore rules.");showAuth(false);});
    }
    private String clanName(String id){return clans[clanIndex(id)];}
    private String currentClanId(){return clanId(clanIndex(clan.startsWith("Green")?"green":clan.startsWith("Blue")?"blue":"red"));}
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
        }else if(page.equals("News")){pageHeader("Announcements","Updates from your community.");if(isAdmin)adminAnnouncementButton();LinearLayout feed=column();top(content,feed,16);listenAnnouncements(feed);}
        else if(page.equals("Chat")){pageHeader("Your conversations","Choose a room to join.");roomCard("✦","Global lounge","Everyone across Kashaf","ALL CLANS",ink,"global");roomCard("●",clan,"Your clan’s private space",clan.toUpperCase(),accent,currentClanId());}
        else if(page.equals("Room")){roomPage();}
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
    private void roomCard(String icon,String title,String subtitle,String tag,int color,String roomId){
        LinearLayout card=row();card.setBackground(bordered(white,20,line));pad(card,16,16,16,16);top(content,card,12);
        TextView mark=text(icon,20,color,true);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(tint(color,.10f),15));card.addView(mark,lp(48,48));
        LinearLayout words=column();TextView name=text(title,16,ink,true);words.addView(name);TextView detail=text(subtitle,12,muted,false);top(words,detail,4);LinearLayout.LayoutParams p=lp(0,-2);p.weight=1;p.leftMargin=13;card.addView(words,p);
        TextView badge=text(tag,9,color,true);badge.setLetterSpacing(.05f);badge.setBackground(shape(tint(color,.10f),20));pad(badge,9,7,9,7);card.addView(badge);
        card.setOnClickListener(v->{currentRoomId=roomId;currentRoomTitle=title;page="Room";showApp();});
    }
    private void roomPage(){
        TextView back=text("‹  All rooms",14,accent,true);top(content,back,4);back.setOnClickListener(v->{page="Chat";showApp();});
        pageHeader(currentRoomTitle,"Messages are visible to "+(currentRoomId.equals("global")?"all clans":"members of this clan")+".");
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
            if(snapshot.isEmpty()){TextView empty=text("No messages yet. Start the conversation.",14,muted,false);empty.setGravity(Gravity.CENTER);pad(empty,12,28,12,28);roomMessages.addView(empty);return;}
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
        db.collection("rooms").document(currentRoomId).collection("messages").add(message).addOnSuccessListener(ref->{if(messageInput!=null)messageInput.setText("");}).addOnFailureListener(error->toast("Message could not be sent. Check your clan membership and Firestore rules."));
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
        TextView button=text("＋  Post announcement",14,white,true);button.setGravity(Gravity.CENTER);button.setBackground(shape(accent,16));pad(button,14,14,14,14);top(content,button,18);button.setOnClickListener(v->showAnnouncementDialog());
    }
    private void showAnnouncementDialog(){
        LinearLayout form=column();pad(form,4,4,4,4);EditText title=field("Title","Announcement title");form.addView(title);
        EditText body=field("Message","Write your update...");body.setSingleLine(false);body.setMinLines(4);body.setGravity(Gravity.TOP);LinearLayout.LayoutParams bodyParams=lp(-1,-2);bodyParams.topMargin=dp(10);form.addView(body,bodyParams);
        Spinner audience=new Spinner(this);String[] options={"All clans","Red Clan","Green Clan","Blue Clan"};ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,options);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);audience.setAdapter(adapter);audience.setBackground(bordered(white,12,line));LinearLayout.LayoutParams audienceParams=lp(-1,48);audienceParams.topMargin=dp(10);form.addView(audience,audienceParams);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("New announcement").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Post",null).create();
        dialog.setOnShowListener(unused->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String t=title.getText().toString().trim(),b=body.getText().toString().trim();if(t.isEmpty()||b.isEmpty()){toast("Add a title and message first.");return;}String[] audiences={"all","red","green","blue"};dialog.dismiss();postAnnouncement(t,b,audiences[audience.getSelectedItemPosition()]);}));dialog.show();
    }
    private void postAnnouncement(String title,String body,String audience){
        FirebaseUser account=auth.getCurrentUser();if(account==null||!isAdmin){toast("Only admins can post announcements.");return;}
        Map<String,Object> announcement=new HashMap<>();announcement.put("title",title);announcement.put("body",body);announcement.put("createdBy",account.getUid());announcement.put("createdAt",FieldValue.serverTimestamp());announcement.put("audience",audience);
        db.collection("announcements").add(announcement).addOnFailureListener(error->toast("Announcement was not published. Check admin access and Firestore rules."));
    }
    private void listenAnnouncements(LinearLayout feed){
        announcementListener=db.collection("announcements").orderBy("createdAt",Query.Direction.DESCENDING).limit(50).addSnapshotListener((snapshot,error)->{
            if(error!=null){toast("Could not load announcements. Check your Firestore rules.");return;}if(snapshot==null)return;feed.removeAllViews();int shown=0;
            for(DocumentSnapshot doc:snapshot.getDocuments()){
                String audience=doc.getString("audience");if(!"all".equals(audience)&&!currentClanId().equals(audience))continue;
                LinearLayout card=column();card.setBackground(bordered(white,20,line));pad(card,18,18,18,18);LinearLayout.LayoutParams cardParams=lp(-1,-2);cardParams.topMargin=dp(10);feed.addView(card,cardParams);
                TextView badge=text("all".equals(audience)?"ALL CLANS":clanName(audience)+" ONLY",10,accent,true);badge.setLetterSpacing(.08f);card.addView(badge);
                TextView title=text(doc.getString("title")==null?"Announcement":doc.getString("title"),18,ink,true);top(card,title,10);
                TextView body=text(doc.getString("body")==null?"":doc.getString("body"),14,muted,false);body.setLineSpacing(dp(3),1);top(card,body,6);
                Timestamp created=doc.getTimestamp("createdAt");if(created!=null){TextView date=text(new SimpleDateFormat("MMM d, yyyy · h:mm a",Locale.getDefault()).format(created.toDate()),10,muted,false);top(card,date,12);}shown++;
            }
            if(shown==0)emptyStateIn(feed,"▤","No announcements yet","New community updates will appear here.");
        });
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
    private void emptyStateIn(LinearLayout parent,String icon,String title,String subtitle){
        LinearLayout card=column();card.setGravity(Gravity.CENTER);card.setBackground(bordered(white,22,line));pad(card,24,30,24,30);
        TextView mark=text(icon,24,accent,true);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(tint(accent,.11f),24));card.addView(mark,lp(58,58));
        TextView heading=text(title,19,ink,true);heading.setGravity(Gravity.CENTER);top(card,heading,18);TextView detail=text(subtitle,14,muted,false);detail.setGravity(Gravity.CENTER);detail.setLineSpacing(dp(3),1);top(card,detail,7);
        LinearLayout.LayoutParams params=lp(-1,-2);params.topMargin=dp(10);parent.addView(card,params);
    }
    private void profileCard(){LinearLayout c=row();c.setBackground(bordered(white,20,line));pad(c,16,16,16,16);top(content,c,24);TextView av=text(initial(user),18,accent,true);av.setGravity(Gravity.CENTER);av.setBackground(shape(tint(accent,.12f),30));c.addView(av,lp(52,52));LinearLayout info=column();TextView name=text(user,16,ink,true);info.addView(name);TextView clanName=text(clan+"  ·  "+rank+"  ·  "+gender,12,muted,false);top(info,clanName,5);LinearLayout.LayoutParams p=lp(0,-2);p.weight=1;p.leftMargin=13;c.addView(info,p);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}
}
