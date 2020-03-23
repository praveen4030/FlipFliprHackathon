package com.kraigs.fliprhackathon.Boards;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kraigs.fliprhackathon.Chat.User;
import com.kraigs.fliprhackathon.Extra.GetTimeAgo;
import com.kraigs.fliprhackathon.Model.Attachments;
import com.kraigs.fliprhackathon.Model.Checklist;
import com.kraigs.fliprhackathon.R;
import com.kraigs.fliprhackathon.User.UsersActivity;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class CardsActivity extends AppCompatActivity {

    RelativeLayout checklistRl, attachmentRl, dateRl, membersRl,attachmentFullRL,checkListFullRl;
    ProgressDialog loadingBar;
    String card, board, list, currentUserId;
    DocumentReference cardRef;
    Bitmap bitmap;
    RecyclerView attachmentRv,checklistRv;
    TextView descriptionTv,dueTimeTv;
    ImageView dueTimeIcon,addChecklist;
    CheckBox dueTimeCheckbox;
    CollectionReference userCol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cards);

        card = getIntent().getStringExtra("card");
        board = getIntent().getStringExtra("board");
        list = getIntent().getStringExtra("list");

        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(card);

//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        toolbar.setTitle(card);

        initializeFields();
        loadingBar = new ProgressDialog(this);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userCol = FirebaseFirestore.getInstance().collection("User");
        cardRef = FirebaseFirestore.getInstance().collection("User").document(currentUserId).collection("Boards").document(board).collection("Lists").document(list).collection("Cards").document(card);

        attachmentFullRL.setVisibility(View.GONE);
        checkListFullRl.setVisibility(View.GONE);
        dueTimeTv.setVisibility(View.GONE);


        click();

        cardRef.collection("Attachments").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {

                if (snapshots.isEmpty()){
                    attachmentFullRL.setVisibility(View.GONE);
                } else{
                    attachmentFullRL.setVisibility(View.VISIBLE);
                }

            }
        });

        cardRef.collection("Checklist").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {

                if (snapshots.isEmpty()){
                    checkListFullRl.setVisibility(View.GONE);
                } else{
                    checkListFullRl.setVisibility(View.VISIBLE);
                }
            }
        });



        fetchData();
        setUpCheckRv();
        setUpAttachmentRv();

    }

    private void click() {

        membersRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inviteMembers();
            }
        });

        attachmentRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                attachmentDialog();
            }
        });

        findViewById(R.id.descCv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addDescription();
            }
        });

        dateRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dueDatDialog();
            }
        });


        addChecklist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCheckItem();
            }
        });

        checklistRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCheckItem();
            }
        });
    }

    private void inviteMembers() {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.zlayout_members);

        RecyclerView inviteRv = dialog.findViewById(R.id.friendsRv);
        Button addFriendsBt = dialog.findViewById(R.id.addFriends);
        inviteRv.setLayoutManager(new LinearLayoutManager(this));
        
        Query query = userCol.document(currentUserId).collection("Friends");

        FirestoreRecyclerOptions<User> options= new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query,User.class)
                .build();
        
        FirestoreRecyclerAdapter<User,FriendsAdapter> adapter = new FirestoreRecyclerAdapter<User, FriendsAdapter>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FriendsAdapter holder, int position, @NonNull User model) {
                holder.nameTv.setText(model.getName());
                Picasso.get().load(model.getImage()).placeholder(R.drawable.user_profile_image).into(holder.userProfileImage);


                cardRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()){

                            ArrayList<String> listMembers;

                            if (task.getResult().contains("members")){
                                listMembers = (ArrayList<String>) task.getResult().get("members");
                            } else{
                                listMembers = new ArrayList<>();
                            }

                            if (listMembers.contains(model.getKey())){
                                holder.inviteBt.setText("Added");
                                holder.inviteBt.setEnabled(false);
                            } else{
                                holder.inviteBt.setText("Invite");
                                holder.inviteBt.setEnabled(true);
                            }


                        }
                    }
                });

                holder.inviteBt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        loadingBar.setTitle("Invite");
                        loadingBar.setMessage("Please wait!");
                        loadingBar.setCanceledOnTouchOutside(false);
                        loadingBar.show();

                        cardRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()){
                                    ArrayList<String> listMembers;

                                    if (task.getResult().contains("members")){
                                        listMembers = (ArrayList<String>) task.getResult().get("members");
                                    } else{
                                        listMembers = new ArrayList<>();
                                    }

                                    listMembers.add(model.getKey());


                                    HashMap<String,Object> map = new HashMap<>();
                                    map.put("members",listMembers);

                                    cardRef.update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){

                                                HashMap<String,Object> map2 = new HashMap<>();
                                                map2.put("card",card);
                                                map2.put("list",list);
                                                map2.put("board",board);
                                                map2.put("owner",currentUserId);
                                                map2.put("timestamp",FieldValue.serverTimestamp());

                                                userCol.document(model.getKey()).collection("Invited cards").document().set(map2).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()){

                                                            loadingBar.dismiss();
                                                            Toast.makeText(CardsActivity.this, "Invited successfully!", Toast.LENGTH_SHORT).show();

                                                        }
                                                    }
                                                });

                                            }
                                        }
                                    });
                                }
                            }
                        });
                    }
                });
            }

            @NonNull
            @Override
            public FriendsAdapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.zunit_friends,parent,false);
                return new FriendsAdapter(v);
            }
        };

        inviteRv.setAdapter(adapter);
        adapter.startListening();

        addFriendsBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CardsActivity.this, UsersActivity.class);
                startActivity(intent);
            }
        });

        dialog.show();

    }

    private void addCheckItem() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.zlayout_list);

        Button saveBt = dialog.findViewById(R.id.saveBt);
        TextInputEditText cardEt = dialog.findViewById(R.id.listEt);
        TextView rawTv = dialog.findViewById(R.id.addRawTv);
        rawTv.setText("Add task");
        cardEt.setHint("Add task");

        saveBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String task  = cardEt.getText().toString();
                if (!TextUtils.isEmpty(card)) {

                    HashMap<String, Object> map1 = new HashMap<>();
                    map1.put("task", task);
                    map1.put("timestamp",FieldValue.serverTimestamp());

                    cardRef.collection("Checklist").document().set(map1).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                dialog.dismiss();
                            }
                        }
                    });

                } else {
                    Toast.makeText(CardsActivity.this, "Select all fields and continue", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();

    }

    private void setUpCheckRv() {

        Query query = cardRef.collection("Checklist").orderBy("timestamp");

        FirestoreRecyclerOptions<Checklist> options = new FirestoreRecyclerOptions.Builder<Checklist>()
                .setQuery(query,Checklist.class)
                .build();

        FirestoreRecyclerAdapter<Checklist,CheckHolder> adapter = new FirestoreRecyclerAdapter<Checklist, CheckHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CheckHolder holder, int position, @NonNull Checklist model) {

                if (model.getTask()!=null){
                    GetTimeAgo gta = new GetTimeAgo();
                    String time = gta.getTimeAgo(model.getTimestamp().getSeconds());
                    holder.timeTv.setText(time);
                }

                if (model.isDone()){
                    holder.check.setChecked(true);
                }

                holder.taskTv.setText(model.getTask());

                holder.check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked){
                            HashMap<String,Object> map = new HashMap<>();
                            map.put("done",true);

                            cardRef.collection("Checklist").document(getSnapshots().getSnapshot(position).getId()).update(map);
                        } else{
                            HashMap<String,Object> map = new HashMap<>();
                            map.put("done",false);

                            cardRef.collection("Checklist").document(getSnapshots().getSnapshot(position).getId()).update(map);
                        }
                    }
                });
            }

            @NonNull
            @Override
            public CheckHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.zunit_checklist,parent,false);
                return  new CheckHolder(v);
            }
        };

        checklistRv.setAdapter(adapter);
        adapter.startListening();

    }

    private void fetchData() {
        cardRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {

                if (snapshot.exists()){

                    if (snapshot.contains("description")){
                        String description = snapshot.get("description").toString();
                        descriptionTv.setText(description);
                    }

                    if (snapshot.contains("dueDate")){

                        dueTimeTv.setVisibility(View.VISIBLE);
                        dueTimeCheckbox.setVisibility(View.VISIBLE);

                        String dueDate = snapshot.get("dueDate").toString();
                        String dueTime = snapshot.get("dueTime").toString();

                        dueTimeTv.setText("Due before: " + dueTime + " " + dueDate);
                        dueTimeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E74C3C")));

                    } else{

                        dueTimeTv.setVisibility(View.GONE);
                        dueTimeCheckbox.setVisibility(View.GONE);

                    }
                }
            }
        });
    }

    private void addDescription() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.zlayout_list);

        Button saveBt = dialog.findViewById(R.id.saveBt);
        TextInputEditText cardEt = dialog.findViewById(R.id.listEt);
        TextView rawTv = dialog.findViewById(R.id.addRawTv);
        rawTv.setText("Add description");
        cardEt.setHint("Card description");

        saveBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String description  = cardEt.getText().toString();
                if (!TextUtils.isEmpty(card)) {

                    HashMap<String, Object> map1 = new HashMap<>();
                    map1.put("description", description);

                    cardRef.update(map1);
                    dialog.dismiss();

                } else {
                    Toast.makeText(CardsActivity.this, "Select all fields and continue", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void setUpAttachmentRv() {
        Query query = cardRef.collection("Attachments").orderBy("timestamp");

        FirestoreRecyclerOptions<Attachments> options = new FirestoreRecyclerOptions.Builder<Attachments>()
                .setQuery(query,Attachments.class)
                .build();

        FirestoreRecyclerAdapter<Attachments,AttachmentHolder> adapter = new FirestoreRecyclerAdapter<Attachments, AttachmentHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull AttachmentHolder holder, int position, @NonNull Attachments model) {

                holder.fileNameTv.setText(model.getFileName());

                GetTimeAgo gta = new GetTimeAgo();
                String time = gta.getTimeAgo(model.getTimestamp().getSeconds());
                holder.sizeTv.setText(getFileSize(model.getSize()) + ", " +  time);


            }

            @NonNull
            @Override
            public AttachmentHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.zunit_attachment,parent,false);
                return  new AttachmentHolder(v);
            }
        };

        attachmentRv.setAdapter(adapter);
        adapter.startListening();

    }

    private void dueDatDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.zlayout_due_date);

        RelativeLayout dateRl,timeRl;
        dateRl = dialog.findViewById(R.id.dateRl);
        timeRl = dialog.findViewById(R.id.timeRl);
        Button doneBt,cancelBt;
        doneBt = dialog.findViewById(R.id.doneBt);
        cancelBt = dialog.findViewById(R.id.cancelBt);
        TextView dateTv,timeTv;
        dateTv = dialog.findViewById(R.id.dateTv);
        timeTv = dialog.findViewById(R.id.timeTv);


        dateRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.zunit_date_picker, null, false);

                // the time picker on the alert dialog, getActivity() is how to get the value
                final DatePicker myDatePicker = (DatePicker) view.findViewById(R.id.myDatePicker);

                // the alert dialog
                new AlertDialog.Builder(CardsActivity.this).setView(view)
                        .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                            @RequiresApi(api = Build.VERSION_CODES.N)
                            public void onClick(DialogInterface dialog, int id) {

                                int month = myDatePicker.getMonth() + 1;
                                int day = myDatePicker.getDayOfMonth();
                                int year = myDatePicker.getYear();

                                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");
                                String from= String.format("%02d", day) + "/" + String.format("%02d", month) + "/" + year;
                                dateTv.setText(from);

                            }

                        }).show();
            }
        });

        timeRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Calendar c = Calendar.getInstance();
                int mHour,mMinute;
                mHour = c.get(Calendar.HOUR_OF_DAY);
                mMinute = c.get(Calendar.MINUTE);


                // Launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(CardsActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {

                                timeTv.setText(hourOfDay + ":" + minute);

                            }
                        }, mHour, mMinute, false);
                timePickerDialog.show();

            }
        });

        doneBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String time = timeTv.getText().toString();
                String date = dateTv.getText().toString();

                if (!time.equals("Select time") && !date.equals("Select date")){

                    HashMap<String,Object> map = new HashMap<>();
                    map.put("dueTime",time);
                    map.put("dueDate",date);

                    cardRef.update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(CardsActivity.this, "Due date added succesfully!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else{
                    Toast.makeText(CardsActivity.this, "Please select all fields", Toast.LENGTH_SHORT).show();
                }

            }
        });

        cancelBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
            }
        });

        dialog.show();

    }

    private void attachmentDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.zlayout_attachment);

        RelativeLayout fileRL, photoRl, linkRL;
        fileRL = dialog.findViewById(R.id.fileRl);
        photoRl = dialog.findViewById(R.id.photoRl);
        linkRL = dialog.findViewById(R.id.linkRl);

        fileRL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("application/pdf");
                startActivityForResult(intent.createChooser(intent, "Select Pdf File"), 438);

            }
        });

        photoRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(CardsActivity.this);

            }
        });

        linkRL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(CardsActivity.this, "Attach link", Toast.LENGTH_SHORT).show();

            }
        });

        dialog.show();
    }

    private void initializeFields() {

        checklistRl = findViewById(R.id.checklistRl);
        attachmentRl = findViewById(R.id.attachmentRl);
        dateRl = findViewById(R.id.dateRl);
        membersRl = findViewById(R.id.membersRl);
        attachmentFullRL = findViewById(R.id.attachmentFullRl);
        attachmentRv = findViewById(R.id.attachmentsRv);
        attachmentRv.setLayoutManager(new LinearLayoutManager(this));
        descriptionTv = findViewById(R.id.descriptionTv);
        dueTimeCheckbox = findViewById(R.id.dueCheckbox);
        dueTimeTv = findViewById(R.id.dueTimeTv);
        dueTimeIcon = findViewById(R.id.icon2);
        checkListFullRl = findViewById(R.id.checklistFullRl);
        checklistRv = findViewById(R.id.checkRv);
        addChecklist = findViewById(R.id.addCheck);
        checklistRl = findViewById(R.id.checklistRl);
        checklistRv.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                loadingBar.setTitle("Sending");
                loadingBar.setMessage("Please wait,we are sending your image.");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                Uri uri = result.getUri();
                File imageFile = new File(uri.getPath());

                Cursor returnCursor =
                        getContentResolver().query(uri, null, null, null, null);
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                returnCursor.moveToFirst();

                String fileName = returnCursor.getString(nameIndex);
                long size = returnCursor.getLong(sizeIndex);

                bitmap = new Compressor(this).setMaxHeight(100).setMaxWidth(100).setQuality(60).compressToBitmap(imageFile);

                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Attachments").child(board).child(list).child(card).child("Images");
                DocumentReference docRef = cardRef.collection("Attachments").document();
                final StorageReference filePath = storageRef.child(docRef + "." + "jpg");

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
                byte[] bytes = baos.toByteArray();

                UploadTask uploadTask = filePath.putBytes(bytes);

                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {

                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {

                                    HashMap<String, Object> map = new HashMap<>();
                                    map.put("url", uri.toString());
                                    map.put("size",size);
                                    map.put("fileName",fileName);
                                    map.put("timestamp",FieldValue.serverTimestamp());

                                    docRef.set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                loadingBar.dismiss();
                                            }
                                        }
                                    });
                                }
                            });
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
            }
        }

        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            loadingBar.setTitle("Sending ic_file");
            loadingBar.setMessage("Please wait,we are sending your document.");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            Uri fileUri = data.getData();
            File file = new File(fileUri.getPath());
//            String fileName = file.getName();
            Cursor returnCursor =
                    getContentResolver().query(fileUri, null, null, null, null);
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            returnCursor.moveToFirst();

            String fileName = returnCursor.getString(nameIndex);
            long size = returnCursor.getLong(sizeIndex);


            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Attachments").child(board).child(list).child(card).child("Files");
            DocumentReference docRef = cardRef.collection("Attachments").document();

            final StorageReference filePath = storageRef.child(docRef.getId());
            final StorageReference thumbPath = storageRef.child(docRef + ".thumb");
            filePath.putFile(fileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                try {
                                    Bitmap bitmap = openPdf(fileUri);
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                                    byte[] data = baos.toByteArray();

                                    UploadTask uploadTask = thumbPath.putBytes(data);
                                    uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                thumbPath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                    @Override
                                                    public void onSuccess(Uri thumbUri) {

                                                        HashMap<String, Object> map = new HashMap<>();
                                                        map.put("fileUrl", uri.toString());
                                                        map.put("url",thumbUri.toString());
                                                        map.put("fileName",fileName);
                                                        map.put("size",size);
                                                        map.put("timestamp", FieldValue.serverTimestamp());

                                                        docRef.set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    loadingBar.dismiss();
                                                                    Toast.makeText(CardsActivity.this, "Added successfully!", Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });


                                                    }
                                                });
                                            }
                                        }
                                    });

                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                    Toast.makeText(CardsActivity.this, "Can't send file. Please try again", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    loadingBar.dismiss();
                    Toast.makeText(CardsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double p = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    loadingBar.setMessage((int) p + "% Uploading...");
                }
            });
        }

    }

    Bitmap openPdf(Uri pdfuri) throws FileNotFoundException {
        ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(pdfuri, "r");
        int pageNum = 0;
        PdfiumCore pdfiumCore = new PdfiumCore(this);
        Bitmap bitmap = null;
        try {

            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);

            pdfiumCore.openPage(pdfDocument, pageNum);

            int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNum);
            int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNum);

            // ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
            // RGB_565 - little worse quality, twice less memory usage
            bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.RGB_565);
            pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNum, 0, 0,
                    width, height);

            //if you need to render annotations and form fields, you can use
            //the same method above adding 'true' as last param

            pdfiumCore.closeDocument(pdfDocument); // important!
            return bitmap;


        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return bitmap;
    }

    private class AttachmentHolder extends RecyclerView.ViewHolder {

        TextView fileNameTv,sizeTv;

        public AttachmentHolder(@NonNull View itemView) {
            super(itemView);

            fileNameTv = itemView.findViewById(R.id.attachmentTv);
            sizeTv = itemView.findViewById(R.id.sizeTv);
        }
    }

    public String getFileSize(long file) {
        DecimalFormat format = new DecimalFormat("#.##");
        long MiB = 1024 * 1024;
        final long KiB = 1024;


        final double length = Double.parseDouble(String.valueOf(file));

        if (length > MiB) {
            return format.format(length / MiB) + " MiB";
        }
        if (length > KiB) {
            return format.format(length / KiB) + " KiB";
        }
        return format.format(length) + " B";
    }

    private class CheckHolder extends RecyclerView.ViewHolder {
        TextView taskTv,timeTv;
        CheckBox check;

        public CheckHolder(@NonNull View itemView) {
            super(itemView);

            taskTv = itemView.findViewById(R.id.tasktv);
            check = itemView.findViewById(R.id.checkbox);
            timeTv = itemView.findViewById(R.id.timeTv);

        }
    }

    private class FriendsAdapter extends RecyclerView.ViewHolder {
        TextView nameTv;
        CircleImageView userProfileImage;
        Button inviteBt;

        public FriendsAdapter(@NonNull View itemView) {
            super(itemView);

            inviteBt = itemView.findViewById(R.id.inviteBt);
            userProfileImage = itemView.findViewById(R.id.users_profile_image);
            nameTv = itemView.findViewById(R.id.nameTv);
        }
    }
}
