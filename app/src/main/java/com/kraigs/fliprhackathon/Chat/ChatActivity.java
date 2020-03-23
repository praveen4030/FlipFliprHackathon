package com.kraigs.fliprhackathon.Chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.kraigs.fliprhackathon.Extra.GetTimeAgo;
import com.kraigs.fliprhackathon.MainActivity;
import com.kraigs.fliprhackathon.R;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String messageRecieverID, messageSenderId;
    private TextView userName, lastSeen, typingTv;
    private CircleImageView userImage;

    private Toolbar chatToolbar;
    private ImageButton sendMessageButton, imagesBt;
    private EditText messageInputText;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef, imageRootRef, notiRef, friendsRef, channelRef, chatRef;
    CollectionReference userCol;
    Query chatQuery;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessagesAdapter messagesAdapter;
    private RecyclerView userMessagesList;

    private String saveCurrentTime, saveCurrentDate;
    private String checker = "", myUrl = "";
    private Uri fileUri;
    private StorageTask uploadTask;
    private ProgressDialog loadingBar;
    RelativeLayout customchatToolbar;
    public static int FLAG_SEEN = 0;
    LinearLayout backLl;
    String image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser().getUid();
        messageRecieverID = getIntent().getStringExtra("visit_user_id");
        rootRef = FirebaseDatabase.getInstance().getReference();
        imageRootRef = FirebaseDatabase.getInstance().getReference();
        notiRef = FirebaseDatabase.getInstance().getReference().child("ChatNotify");
        friendsRef = FirebaseDatabase.getInstance().getReference().child("Friends");
        channelRef = rootRef.child("ChatChannel");
        chatRef = rootRef.child("Message").child(messageSenderId).child(messageRecieverID);
        chatQuery = rootRef.child("Message").child(messageSenderId).child(messageRecieverID).orderByChild("time");
        userCol = FirebaseFirestore.getInstance().collection("User");

        updateUserStatus();

        userCol.document(messageRecieverID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable DocumentSnapshot dataSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (dataSnapshot.exists()) {
                    if (dataSnapshot.contains("image")) {
                        image = dataSnapshot.get("image").toString();
                        Picasso.get().load(image).placeholder(R.drawable.user_profile_image).into(userImage);
                    }

                    if (dataSnapshot.contains("name")) {
                        String name = dataSnapshot.get("name").toString();
                        userName.setText(name);
                    }
                }
            }
        });

        rootRef.child("OfflineUsers").child(messageRecieverID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (dataSnapshot.hasChild("online")) {
                        String onlineStatus = dataSnapshot.child("online").getValue().toString();
                        if (onlineStatus.equals("true")) {
                            lastSeen.setText("Online");
                        } else {
                            if (dataSnapshot.hasChild("timestamp")) {
                                long timestamp = (long) dataSnapshot.child("timestamp").getValue();
                                GetTimeAgo gta = new GetTimeAgo();
                                String time = gta.getTimeAgo(timestamp);
                                lastSeen.setText(time);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        InitializeFields();

        chatQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (dataSnapshot.exists()) {
                    Messages messages = dataSnapshot.getValue(Messages.class);
                    String key = dataSnapshot.getKey();
                    String seen = messages.getSeen();

                    if (FLAG_SEEN == 1) {
                        if (seen != null) {
                            if (seen.equals("false")) {
                                chatRef.child(key).child("seen").setValue("true");
                            }
                        } else {
                            chatRef.child(key).child("seen").setValue("true");
                        }
                    }


                    messagesList.add(messages);
                    messagesAdapter.notifyDataSetChanged();
                    userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());


                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        customchatToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(ChatActivity.this, MentorProfileActivity.class);
//                intent.putExtra("mentor_id", messageRecieverID);
//                startActivity(intent);
            }
        });

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        imagesBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDialog();
            }
        });

        messageInputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s)) {
                    channelRef.child(messageSenderId).child(messageRecieverID).child("typing").setValue("true");
                } else {
                    channelRef.child(messageSenderId).child(messageRecieverID).child("typing").setValue("false");
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        channelRef.child(messageRecieverID).child(messageSenderId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (dataSnapshot.hasChild("typing")) {
                        String typing = dataSnapshot.child("typing").getValue().toString();
                        if (typing.equals("true")) {
                            typingTv.setText("Typing...");
                            lastSeen.setVisibility(View.INVISIBLE);
                            typingTv.setVisibility(View.VISIBLE);

                        } else {
                            typingTv.setText("");
                            typingTv.setVisibility(View.INVISIBLE);
                            lastSeen.setVisibility(View.VISIBLE);
                        }
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(ChatActivity.this);
        View sheetView = getLayoutInflater().inflate(R.layout.zlayout_dialog, null);
        dialog.setContentView(sheetView);

        RelativeLayout sendImageBt = sheetView.findViewById(R.id.sendImage);
        RelativeLayout sendDocumenBt = sheetView.findViewById(R.id.sendDocument);

        sendDocumenBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checker = "pdf";
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("application/pdf");
                startActivityForResult(intent.createChooser(intent, "Select Pdf File"), 438);
                dialog.dismiss();
            }
        });

        sendImageBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checker = "image";
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(ChatActivity.this);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void sendMessage() {

        String messageText = messageInputText.getText().toString();

        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(ChatActivity.this, "Send something...", Toast.LENGTH_SHORT).show();
        } else {

            String messageSenderRef = "Message/" + messageSenderId + "/" + messageRecieverID;
            String messageRecieverRef = "Message/" + messageRecieverID + "/" + messageSenderId;

            DatabaseReference userMessageKeyRef = rootRef.child("Message")
                    .child(messageSenderId).child(messageRecieverID).push();
            String messagePushId = userMessageKeyRef.getKey();

            HashMap<String, Object> messageTextBody = new HashMap<>();
            messageTextBody.put("message", messageText);
            messageTextBody.put("type", "text");
            messageTextBody.put("from", messageSenderId);
            messageTextBody.put("to", messageRecieverID);
            messageTextBody.put("messageID", messagePushId);
            messageTextBody.put("time", ServerValue.TIMESTAMP);
            messageTextBody.put("seen", "false");
            messageTextBody.put("key", userMessageKeyRef.getKey());

            Map messageBodydetails = new HashMap();
            messageBodydetails.put(messageSenderRef + "/" + messagePushId, messageTextBody);
            messageBodydetails.put(messageRecieverRef + "/" + messagePushId, messageTextBody);

            rootRef.updateChildren(messageBodydetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("type", "message");
                        map.put("from", messageSenderId);

                        notiRef.child(messageRecieverID).push().setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    channelRef.child(messageSenderId).child(messageRecieverID).child("timestamp").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                channelRef.child(messageRecieverID).child(messageSenderId).child("timestamp").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {

                                                        } else {
                                                            Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                            } else {
                                                Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    });
                                } else {
                                    Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            messageInputText.setText("");
        }
    }

    private void InitializeFields() {

        chatToolbar = (Toolbar) findViewById(R.id.chat_toolbar);
        setSupportActionBar(chatToolbar);

        getSupportActionBar().setTitle("");

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null);
        actionBar.setCustomView(actionBarView);

        backLl = findViewById(R.id.backLl);
        backLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatActivity.this, MainActivity.class);
                intent.putExtra("type", "chat");
                startActivity(intent);
                finish();
            }
        });

        loadingBar = new ProgressDialog(this);
        userImage = (CircleImageView) findViewById(R.id.custom_profile_IMAGE);
        lastSeen = findViewById(R.id.onlineStatus);
        typingTv = findViewById(R.id.typing);
        userName = (TextView) findViewById(R.id.custom_profile_name);

        sendMessageButton = (ImageButton) findViewById(R.id.send_message_btn);
        messageInputText = (EditText) findViewById(R.id.input_message);
        imagesBt = (ImageButton) findViewById(R.id.imagesBt);

        customchatToolbar = (RelativeLayout) findViewById(R.id.customChatRl);

        messagesAdapter = new MessagesAdapter(messagesList, messageRecieverID);
        userMessagesList = (RecyclerView) findViewById(R.id.private_message_list_of_users);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messagesAdapter);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calendar.getTime());

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

                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Image Files");

                final String messageSenderRef = "Message/" + messageSenderId + "/" + messageRecieverID;
                final String messageRecieverRef = "Message/" + messageRecieverID + "/" + messageSenderId;

                DatabaseReference userMessageKeyRef = rootRef.child("Message")
                        .child(messageSenderId).child(messageRecieverID).push();
                final String messagePushId = userMessageKeyRef.getKey();

                final StorageReference filePath = storageRef.child(messagePushId + "." + "jpg");

                uploadTask = filePath.putFile(uri);

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

                                    HashMap<String, Object> messageTextBody = new HashMap<>();
                                    messageTextBody.put("message", uri.toString());
                                    messageTextBody.put("name", uri.getLastPathSegment());
                                    messageTextBody.put("type", checker);
                                    messageTextBody.put("from", messageSenderId);
                                    messageTextBody.put("to", messageRecieverID);
                                    messageTextBody.put("messageID", messagePushId);
                                    messageTextBody.put("time", ServerValue.TIMESTAMP);
                                    messageTextBody.put("seen", "false");
                                    messageTextBody.put("key", userMessageKeyRef.getKey());

                                    HashMap<String, Object> messageBodyDetails = new HashMap<>();
                                    messageBodyDetails.put(messageSenderRef + "/" + messagePushId, messageTextBody);
                                    messageBodyDetails.put(messageRecieverRef + "/" + messagePushId, messageTextBody);

                                    imageRootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                        @Override
                                        public void onComplete(@NonNull Task task) {
                                            if (task.isSuccessful()) {
                                                HashMap<String, Object> map = new HashMap<>();
                                                map.put("type", "image");
                                                map.put("from", messageSenderId);

                                                notiRef.child(messageRecieverID).push().setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            channelRef.child(messageSenderId).child(messageRecieverID).child("timestamp").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        channelRef.child(messageRecieverID).child(messageSenderId).child("timestamp").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if (task.isSuccessful()) {
                                                                                    loadingBar.dismiss();
                                                                                    Toast.makeText(ChatActivity.this, "Message Sent Successfully...", Toast.LENGTH_SHORT).show();
                                                                                } else {
                                                                                    Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                                                                }
                                                                            }
                                                                        });
                                                                    } else {
                                                                        Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                                                    }

                                                                }
                                                            });

                                                        } else {
                                                            loadingBar.dismiss();
                                                            Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                            } else {
                                                loadingBar.dismiss();
                                                Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();

                                            }

                                            messageInputText.setText("");
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

            fileUri = data.getData();

            if (!checker.equals("image")) {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Doc Files");
                final String messageSenderRef = "Message/" + messageSenderId + "/" + messageRecieverID;
                final String messageRecieverRef = "Message/" + messageRecieverID + "/" + messageSenderId;

                DatabaseReference userMessageKeyRef = rootRef.child("Message")
                        .child(messageSenderId).child(messageRecieverID).push();
                final String messagePushId = userMessageKeyRef.getKey();

                final StorageReference filePath = storageRef.child(messagePushId + "." + checker);
                final StorageReference thumbPath = storageRef.child(messagePushId + ".thumb");
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
                                                            HashMap<String, Object> messageTextBody = new HashMap<>();

                                                            messageTextBody.put("message", uri.toString());
                                                            messageTextBody.put("name", fileUri.getLastPathSegment());
                                                            messageTextBody.put("type", checker);
                                                            messageTextBody.put("from", messageSenderId);
                                                            messageTextBody.put("to", messageRecieverID);
                                                            messageTextBody.put("messageID", messagePushId);
                                                            messageTextBody.put("time", ServerValue.TIMESTAMP);
                                                            messageTextBody.put("seen", "false");
                                                            messageTextBody.put("key", userMessageKeyRef.getKey());
                                                            messageTextBody.put("thumb", thumbUri.toString());

                                                            HashMap<String, Object> messageBodydetails = new HashMap<>();
                                                            messageBodydetails.put(messageSenderRef + "/" + messagePushId, messageTextBody);
                                                            messageBodydetails.put(messageRecieverRef + "/" + messagePushId, messageTextBody);
                                                            rootRef.updateChildren(messageBodydetails).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        HashMap<String, Object> map = new HashMap<>();
                                                                        map.put("type", "document");
                                                                        map.put("from", messageSenderId);

                                                                        notiRef.child(messageRecieverID).push().setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if (task.isSuccessful()) {
                                                                                    channelRef.child(messageSenderId).child(messageRecieverID).child("timestamp").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                        @Override
                                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                                            if (task.isSuccessful()) {
                                                                                                channelRef.child(messageRecieverID).child(messageSenderId).child("timestamp").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                    @Override
                                                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                                                        if (task.isSuccessful()) {
                                                                                                            loadingBar.dismiss();
                                                                                                            Toast.makeText(ChatActivity.this, "Message Sent Successfully...", Toast.LENGTH_SHORT).show();
                                                                                                        } else {
                                                                                                            Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                                                                                        }
                                                                                                    }
                                                                                                });
                                                                                            } else {
                                                                                                Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                                                                            }

                                                                                        }
                                                                                    });
                                                                                } else {
                                                                                    loadingBar.dismiss();
                                                                                    Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                                                                }
                                                                            }
                                                                        });
                                                                    } else {
                                                                        loadingBar.dismiss();
                                                                        Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(ChatActivity.this, "Can't send file. Please try again", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingBar.dismiss();
                        Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double p = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        MessagesAdapter.progress = p;
                        loadingBar.setMessage((int) p + "% Uploading...");
                    }
                });
            } else {

                loadingBar.dismiss();
                Toast.makeText(this, "Nothing selected, Error.", Toast.LENGTH_SHORT).show();

            }
        }
    }

    @Override
    public void onBackPressed() {
        channelRef.child(messageSenderId).child(messageRecieverID).child("typing").setValue("false");
        super.onBackPressed();
        finish();
        FLAG_SEEN = 0;
    }

    @Override
    protected void onStop() {
        channelRef.child(messageSenderId).child(messageRecieverID).child("typing").setValue("false");
        super.onStop();
        FLAG_SEEN = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        FLAG_SEEN = 1;
    }

    @Override
    protected void onPause() {
        channelRef.child(messageSenderId).child(messageRecieverID).child("typing").setValue("false");
        super.onPause();
        FLAG_SEEN = 0;
    }

    @Override
    protected void onStart() {
        super.onStart();
        FLAG_SEEN = 1;
    }

    private void updateUserStatus() {


        HashMap<String, Object> map = new HashMap<>();
        map.put("online", "true");

        rootRef.child("OfflineUsers").child(messageSenderId).updateChildren(map);

        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("timestamp", ServerValue.TIMESTAMP);
        onlineStateMap.put("online", "false");

        rootRef.child("OfflineUsers").child(messageSenderId).onDisconnect().setValue(onlineStateMap);

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
}
