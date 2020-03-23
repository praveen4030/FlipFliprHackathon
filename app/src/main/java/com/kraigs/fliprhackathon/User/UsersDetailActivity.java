package com.kraigs.fliprhackathon.User;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kraigs.fliprhackathon.MainActivity;
import com.kraigs.fliprhackathon.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class UsersDetailActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    String currentUserID;
    String gender;
    ProgressDialog loadingBar;
    private static Uri uri = null;

    CollectionReference userCol;
    @BindView(R.id.userPic)
    CircleImageView userPic;
    @BindView(R.id.userNameEt)
    EditText userNameEt;
    @BindView(R.id.cityEt)
    EditText cityEt;
    @BindView(R.id.dobEt)
    EditText dobEt;
    @BindView(R.id.boySrc)
    ImageView boySrc;
    @BindView(R.id.boyRl)
    RelativeLayout boyRl;
    @BindView(R.id.girlSrc)
    ImageView girlSrc;
    @BindView(R.id.girlRl)
    RelativeLayout girlRl;
    @BindView(R.id.calendarView)
    ImageView calendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_data);

        ButterKnife.bind(this);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        loadingBar = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        userCol = FirebaseFirestore.getInstance().collection("User");

        calendarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog = new Dialog(UsersDetailActivity.this);
                dialog.setCancelable(true);
                dialog.setContentView(R.layout.zunit_date_picker);

                DatePicker myDatePicker = dialog.findViewById(R.id.myDatePicker);

                TextView doneTv = dialog.findViewById(R.id.doneTv);
                doneTv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int month = myDatePicker.getMonth() + 1;
                        int day = myDatePicker.getDayOfMonth();
                        int year = myDatePicker.getYear();

                        String dob = String.format("%02d", day) + "/" + String.format("%02d", month) + "/" + year;
                        dobEt.setText(dob);

                        dialog.dismiss();

                    }
                });

                dialog.show();

            }
        });

        boyRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                gender = "Male";
                girlSrc.setColorFilter(getResources().getColor(R.color.grey));
                boySrc.setColorFilter(getResources().getColor(R.color.red));

            }
        });


        girlRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                gender = "Female";
                girlSrc.setColorFilter(getResources().getColor(R.color.red));
                boySrc.setColorFilter(getResources().getColor(R.color.grey));
            }
        });

        userPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, 32);
            }
        });


        userCol.document(currentUserID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot dataSnapshot, @Nullable FirebaseFirestoreException e) {
                if (dataSnapshot.exists()) {

                    if (dataSnapshot.contains("name")) {
                        String name = dataSnapshot.get("name").toString();
                        userNameEt.setText(name);
                    }
                    if (dataSnapshot.contains("image")) {
                        String image = dataSnapshot.get("image").toString();
                        Picasso.get().load(image).into(userPic);
                    }
                    if (dataSnapshot.contains("city")) {
                        String city = dataSnapshot.get("city").toString();
                        cityEt.setText(city);
                    }

                    if (dataSnapshot.contains("dob")) {
                        String dob = dataSnapshot.get("dob").toString();
                        dobEt.setText(dob);
                    }

                    if (dataSnapshot.contains("gender")) {
                        String genderSt = dataSnapshot.get("gender").toString();
                        if (genderSt.equals("Male")) {
                            girlSrc.setColorFilter(getResources().getColor(R.color.grey));
                            boySrc.setColorFilter(getResources().getColor(R.color.red));
                            gender = genderSt;
                        } else if (genderSt.equals("Female")) {

                            gender = genderSt;

                            girlSrc.setColorFilter(getResources().getColor(R.color.red));
                            boySrc.setColorFilter(getResources().getColor(R.color.grey));

                        }
                    }


                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 32 && resultCode == RESULT_OK && data != null) {

            uri = data.getData();
            Picasso.get().load(uri.toString()).into(userPic);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_save, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.action_save) {
            saveDetails();
        }

        return true;

    }

    private void saveDetails() {

        String name = userNameEt.getText().toString();
        String city = cityEt.getText().toString();
        String dob = dobEt.getText().toString();

        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(city) && !TextUtils.isEmpty(gender) && !TextUtils.isEmpty(dob)) {
            loadingBar.setMessage("Please wait");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            if (uri != null) {

                final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("ProfilePic").child(currentUserID + ".jpg");
                filePath.putFile(uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {

                                    HashMap<String, Object> map = new HashMap<>();
                                    map.put("name", name);
                                    map.put("city", city);
                                    map.put("gender", gender);
                                    map.put("image", uri.toString());
                                    map.put("dob", dob);


                                    userCol.document(currentUserID).update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                loadingBar.dismiss();

                                                Intent intent = new Intent(UsersDetailActivity.this, MainActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                startActivity(intent);
                                                finish();

                                                Toast.makeText(UsersDetailActivity.this, "Your Details updated successfully!", Toast.LENGTH_SHORT).show();
                                            } else {
                                                String message = task.getException().toString();
                                                Toast.makeText(UsersDetailActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                        }
                                    });
                                }
                            });
                        } else {

                            String message = task.getException().toString();
                            Toast.makeText(UsersDetailActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();

                        }
                    }
                });

            } else {

                HashMap<String, Object> map = new HashMap<>();
                map.put("name", name);
                map.put("city", city);
                map.put("gender", gender);
                map.put("dob", dob);

                userCol.document(currentUserID).update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            loadingBar.dismiss();
                            Intent intent = new Intent(UsersDetailActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                            Toast.makeText(UsersDetailActivity.this, "Your Details saved successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            String message = task.getException().toString();
                            Toast.makeText(UsersDetailActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                });
            }
        } else {
            Toast.makeText(UsersDetailActivity.this, "Please fill all details!", Toast.LENGTH_SHORT).show();
        }

    }
}
