package com.morocco.appfactory;

import android.content.*;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.morocco.appfactory.data.PasswordHasher;
import com.morocco.appfactory.ui.*;
import java.util.*;
import java.util.concurrent.*;

public class LoginActivity extends AppCompatActivity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private FactoryButton login;
    private boolean busy;
    private boolean entering;
    private final ActivityResultLauncher<Intent> googleResult=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result->{
        if(result.getResultCode()!=RESULT_OK) { Ui.toast(this,"Google sign-in cancelled or unavailable");return; }
        try {
            GoogleSignInAccount account=GoogleSignIn.getSignedInAccountFromIntent(result.getData()).getResult(ApiException.class);
            if(account==null || account.getEmail()==null) throw new IllegalStateException("Google did not return an email");
            enter(false,account.getEmail());
        } catch(Exception e) { Ui.message(this,"Google sign-in", "Sign-in failed: "+Ui.error(e)+"\n\nRegister com.morocco.appfactory and this build’s signing SHA-1 in Google Cloud Console, enable Google Sign-In, and use a device with Google Play Services. Guest and local accounts work offline."); }
    });
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);setContentView(R.layout.activity_login);
        login=findViewById(R.id.login);login.setIcon(R.drawable.ic_person);
        findViewById(R.id.guest).setOnClickListener(v->enter(true,"Guest creator"));
        FactoryButton google=findViewById(R.id.google);google.setBackgroundResource(R.drawable.bg_google);google.setTextColor(0xFF202030);google.setIcon(R.drawable.ic_google);
        android.graphics.drawable.Drawable icon=google.getCompoundDrawablesRelative()[0];if(icon!=null)icon.setTint(0xFF4285F4);
        google.setOnClickListener(v->{
            if(busy)return;
            GoogleSignInOptions options=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
            googleResult.launch(GoogleSignIn.getClient(this,options).getSignInIntent());
        });
        login.setOnClickListener(v->authenticate());
        Ui.fade(findViewById(R.id.email),0);Ui.fade(findViewById(R.id.password),1);
    }
    private void authenticate() {
        if(busy)return;
        EditText emailView=findViewById(R.id.email),passView=findViewById(R.id.password);
        String email=emailView.getText().toString().trim().toLowerCase(Locale.ROOT);
        char[] password=passView.getText().toString().toCharArray();
        if(email.length()>254 || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailView.setError("Enter a valid email");Arrays.fill(password,'\0');return; }
        if(password.length<8 || password.length>256) { passView.setError("Use 8–256 characters");Arrays.fill(password,'\0');return; }
        busy=true;login.setEnabled(false);login.setText("Checking local account…");
        worker.execute(()->{
            String error=null;boolean created=false;
            try {
                SharedPreferences accounts=getSharedPreferences("local_accounts",0);
                String stored=accounts.getString(email,null);
                if(stored==null) { if(!accounts.edit().putString(email,PasswordHasher.hash(password)).commit()) throw new IllegalStateException("Could not save account");created=true; }
                else if(!PasswordHasher.verify(password,stored)) error="Incorrect local password";
            } catch(Exception e) {error=Ui.error(e);}finally {Arrays.fill(password,'\0');}
            String failure=error;boolean isNew=created;
            runOnUiThread(()->{
                if(isDestroyed())return;
                busy=false;login.setEnabled(true);login.setText("Login / Create Local Account");
                if(failure!=null)Ui.toast(this,failure);else {Ui.toast(this,isNew?"Local account created on this device":"Welcome back");enter(false,email);}
            });
        });
    }
    private void enter(boolean guest,String name) {
        if(isFinishing()||entering)return;
        entering=true;
        getSharedPreferences("factory",0).edit().putBoolean("isGuest",guest).putBoolean("signed_in",true).putString("profile",name).apply();
        startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
    @Override protected void onDestroy() {worker.shutdown();super.onDestroy();}
}
