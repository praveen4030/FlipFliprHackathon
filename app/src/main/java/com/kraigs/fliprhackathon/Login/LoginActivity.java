package com.kraigs.fliprhackathon.Login;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.kraigs.fliprhackathon.MainActivity;
import com.kraigs.fliprhackathon.R;
import com.kraigs.fliprhackathon.User.UsersDetailActivity;
import com.mukesh.OnOtpCompletionListener;
import com.mukesh.OtpView;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, OnOtpCompletionListener {

    private ImageButton sendCodeButton;
    private TextInputEditText inputPhoneNo ;
    TextInputLayout phoneInputLayout;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    private GoogleSignInClient mGoogleSignInClient;
    CardView googleCv;
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private static final String TAG = "LoginActivity";
    ProgressDialog loadingBar;
    private Button validateButton;
    private OtpView otpView;
    CollectionReference userCol;

    @BindView(R.id.otpLl)
    LinearLayout otpLl;

    @BindView(R.id.loginRl)
    RelativeLayout loginRl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null){
            SendUserToMainActivity();
        }

        initializeFields();
        setListeners();

        userCol = FirebaseFirestore.getInstance().collection("User");

        googleSignIn();

        mobileSignIn();
    }

    private void mobileSignIn() {
        sendCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = inputPhoneNo.getText().toString();
                if (TextUtils.isEmpty(phoneNumber)){
                    Toast.makeText(LoginActivity.this, "Please enter Phone number to continue.", Toast.LENGTH_SHORT).show();
                }

                else{

                    loadingBar.setTitle("Phone Verification");
                    loadingBar.setMessage("Please wait we are authenticating your phone.");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();
                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            "+91" + phoneNumber,
                            60, TimeUnit.SECONDS,
                            LoginActivity.this,
                            mCallbacks);
                }
            }
        });

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                loadingBar.dismiss();
                Toast.makeText(LoginActivity.this, "Invalid Phone Number ,Please retry", Toast.LENGTH_SHORT).show();

                loginRl.setVisibility(View.VISIBLE);
                otpLl.setVisibility(View.GONE);
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                mVerificationId = verificationId;
                mResendToken = token;
                loadingBar.dismiss();

                Toast.makeText(LoginActivity.this, "Code Sent,Please check and verify.", Toast.LENGTH_SHORT).show();

                loginRl.setVisibility(View.GONE);
                otpLl.setVisibility(View.VISIBLE);

            }
        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential){
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            String currentUserId = mAuth.getCurrentUser().getUid();
                            String deviceToken = FirebaseInstanceId.getInstance().getToken();
                            String phoneNumber = mAuth.getCurrentUser().getPhoneNumber();

                            userCol.document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                                    if (snapshot.exists()){
                                        HashMap<String,Object> userMap = new HashMap<>();
                                        userMap.put("device_token",deviceToken);
                                        userMap.put("userID",phoneNumber);
                                        userMap.put("key",currentUserId);

                                        userCol.document(currentUserId).update(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()){
                                                    loadingBar.dismiss();
                                                    Toast.makeText(LoginActivity.this, "You have logged in Successfully", Toast.LENGTH_SHORT).show();
                                                    SendUserToMainActivity();
                                                }
                                            }
                                        });

                                    } else{
                                        HashMap<String,Object> userMap = new HashMap<>();
                                        userMap.put("device_token",deviceToken);
                                        userMap.put("userID",phoneNumber);
                                        userMap.put("key",currentUserId);

                                        userCol.document(currentUserId).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()){
                                                    loadingBar.dismiss();
                                                    Toast.makeText(LoginActivity.this, "You have logged in Successfully", Toast.LENGTH_SHORT).show();
                                                    SendUserToDetailActivity();
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }

                        else{
                            String message = task.getException().toString();
                            Toast.makeText(LoginActivity.this, "Error : " + message, Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    private void initializeFields() {
        loadingBar = new ProgressDialog(this);
        googleCv = findViewById(R.id.googleCv);

        sendCodeButton = (ImageButton) findViewById(R.id.bt_send_code);
        inputPhoneNo = findViewById(R.id.phone_number_input);
        phoneInputLayout = findViewById(R.id.phoneLayout);
        loadingBar = new ProgressDialog(this);

        otpView = findViewById(R.id.otp_view);
        validateButton = findViewById(R.id.validate_button);
    }

    private void googleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();

        googleCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                signIn();

                Toast.makeText(LoginActivity.this, "Google SIgn In", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        Toast.makeText(this, "Signing In...", Toast.LENGTH_SHORT).show();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }

//        mCallbackManager.onActivityResult(requestCode, resultCode, data);

    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        loadingBar.setTitle("Sign in");
        loadingBar.setMessage("Please Wait,while we signing you!");
        loadingBar.setCanceledOnTouchOutside(true);
        loadingBar.show();

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            boolean isNew = task.getResult().getAdditionalUserInfo().isNewUser();
                            if (isNew) {

                                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(LoginActivity.this.getApplicationContext());

                                String personName = account.getDisplayName();
                                String image = account.getPhotoUrl().toString();
                                String email = account.getEmail();

                                String deviceToken = FirebaseInstanceId.getInstance().getToken();
                                String currentUserId = mAuth.getCurrentUser().getUid();

                                HashMap<String, Object> userMap = new HashMap<>();
                                userMap.put("device_token", deviceToken);
                                userMap.put("userID", email);
                                userMap.put("name", personName);
                                userMap.put("image",image);
                                userMap.put("key",currentUserId);


                                userCol.document(currentUserId).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            loadingBar.dismiss();
                                            LoginActivity.this.SendUserToDetailActivity();
                                            Toast.makeText(LoginActivity.this, "Logged in Successfully", Toast.LENGTH_SHORT).show();

                                        } else{
                                            loadingBar.dismiss();
                                            Toast.makeText(LoginActivity.this, "Error: " + task.getException().toString(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                            } else {
                                String Uid = mAuth.getCurrentUser().getUid();
                                String token = FirebaseInstanceId.getInstance().getToken();
                                HashMap<String,Object> map = new HashMap<>();
                                map.put("device_token",token);

                                userCol.document(Uid).update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            loadingBar.dismiss();
                                            SendUserToMainActivity();
                                        } else{
                                            loadingBar.dismiss();
                                            Toast.makeText(LoginActivity.this, "Error: " + task.getException().toString(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        } else {
                            loadingBar.dismiss();
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void SendUserToDetailActivity() {
        Intent mainIntent = new Intent(LoginActivity.this, UsersDetailActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(mainIntent);
        finish();
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(LoginActivity.this, UsersDetailActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainIntent);
        finish();
    }

    private void setListeners() {
        validateButton.setOnClickListener(this);
        otpView.setOtpCompletionListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.validate_button) {
            loginRl.setVisibility(View.GONE);

            String verificationCode = otpView.getText().toString();
            if (TextUtils.isEmpty(verificationCode)){
                Toast.makeText(LoginActivity.this, "Please enter Verification Code.", Toast.LENGTH_SHORT).show();
            }
            else{
                loadingBar.setTitle("Verification Code");
                loadingBar.setMessage("Please wait we are verifying your verification Code.");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId,verificationCode);
                signInWithPhoneAuthCredential(credential);
            }

        }
    }

    @Override
    public void onOtpCompleted(String otp) {
        loginRl.setVisibility(View.GONE);

        String verificationCode = otpView.getText().toString();
        if (TextUtils.isEmpty(verificationCode)){
            Toast.makeText(LoginActivity.this, "Please enter Verification Code.", Toast.LENGTH_SHORT).show();
        }

        else{
            loadingBar.setTitle("Verification Code");
            loadingBar.setMessage("Please wait we are verifying your verification Code.");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId,verificationCode);
            signInWithPhoneAuthCredential(credential);
        }


    }

}
