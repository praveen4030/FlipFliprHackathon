package com.kraigs.fliprhackathon.User;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.kraigs.fliprhackathon.Chat.User;
import com.kraigs.fliprhackathon.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import javax.annotation.Nullable;

import de.hdodenhof.circleimageview.CircleImageView;

public class RequestsActivity extends AppCompatActivity {

    private RecyclerView requestListRv;
    private FirebaseAuth mAuth;
    private String currentUserId;
    CollectionReference userCol,reqCol;
    DatabaseReference notiRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        getSupportActionBar().setTitle("Requests");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        notiRef = FirebaseDatabase.getInstance().getReference().child("Notifications");

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        userCol = FirebaseFirestore.getInstance().collection("User");
        reqCol = userCol;

        requestListRv = (RecyclerView) findViewById(R.id.requestRv);
        requestListRv.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(reqCol.document(currentUserId).collection("Requests"), User.class)
                .build();

        FirestoreRecyclerAdapter<User,RequestViewHolder> adapter = new FirestoreRecyclerAdapter<User, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final RequestViewHolder holder, int position, @NonNull User model) {

                final String list_user_id = getSnapshots().getSnapshot(position).getId();

                holder.acceptButton.setVisibility(View.VISIBLE);
                holder.cancelButton.setVisibility(View.VISIBLE);

                String type = getSnapshots().getSnapshot(position).get("request_type").toString();

                holder.acceptButton.setEnabled(true);
                holder.cancelButton.setEnabled(true);

                if (type.equals("recieved")){

                    userCol.document(list_user_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot snapshot1) {
                            String requestImage = null;
                            if (snapshot1.contains("image")){
                                requestImage = snapshot1.get("image").toString();
                                Picasso.get().load(requestImage).into(holder.profileImage);
                            }

                            final String requestName = snapshot1.get("name").toString();

                            String finalRequestImage = requestImage;
                            userCol.document(currentUserId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot snapshot2) {

                                    String userimage = null;
                                    if (snapshot2.contains("image")){
                                        userimage = snapshot2.get("image").toString();
                                        Picasso.get().load(userimage).into(holder.profileImage);
                                    }

                                    final String username = snapshot2.get("name").toString();

                                    holder.userName.setText(requestName);

                                    String finalUserimage = userimage;
                                    holder.acceptButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(RequestsActivity.this);
                                            alertDialog.setTitle("Cancel").setMessage("Do you really want to accept a connect request?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {

                                                    HashMap<String,String> map = new HashMap<>();
                                                    map.put("name",requestName);
                                                    map.put("key",list_user_id);
                                                    if(finalRequestImage !=null && !TextUtils.isEmpty(finalRequestImage)){
                                                        map.put("image",finalRequestImage);
                                                    }


                                                    userCol.document(currentUserId).collection("Friends").document(list_user_id)
                                                            .set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()){

                                                                HashMap<String,String> usermap = new HashMap<>();
                                                                usermap.put("name",username);
                                                                usermap.put("key",currentUserId);
                                                                if(finalUserimage !=null && !TextUtils.isEmpty(finalUserimage)){
                                                                    usermap.put("image",finalUserimage);
                                                                }

                                                                userCol.document(list_user_id).collection("Friends").document(currentUserId)
                                                                        .set(usermap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if(task.isSuccessful()){
                                                                            reqCol.document(currentUserId).collection("Requests").document(list_user_id).delete()
                                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                        @Override
                                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                                            if (task.isSuccessful()){
                                                                                                reqCol.document(list_user_id).collection("Requests").document(currentUserId).delete()
                                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                            @Override
                                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                                if (task.isSuccessful()){
                                                                                                                    HashMap<String, String> chatNotificationMap = new HashMap<>();
                                                                                                                    chatNotificationMap.put("from", currentUserId);
                                                                                                                    chatNotificationMap.put("type", "accept");
                                                                                                                    notiRef.child(list_user_id).push().setValue(chatNotificationMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                        @Override
                                                                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                                                                            if (task.isSuccessful()) {
                                                                                                                                Toast.makeText(RequestsActivity.this, "New Mentor Saved ", Toast.LENGTH_SHORT).show();

                                                                                                                            } else {
                                                                                                                                Toast.makeText(RequestsActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                                                                                                            }
                                                                                                                        }
                                                                                                                    });
                                                                                                                }
                                                                                                            }
                                                                                                        });
                                                                                            }
                                                                                        }
                                                                                    });
                                                                        } else{
                                                                            Toast.makeText(RequestsActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                                                                        }

                                                                    }
                                                                });
                                                            }
                                                        }
                                                    });
                                                }
                                            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {

                                                }
                                            }).show();

                                            holder.cancelButton.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(RequestsActivity.this);
                                                    alertDialog.setTitle("Cancel").setMessage("Do you really want to cancel a connect request?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            reqCol.document(currentUserId).collection("Requests").document(list_user_id).delete()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()){
                                                                                reqCol.document(list_user_id).collection("Requests").document(currentUserId).delete()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                if (task.isSuccessful()){
                                                                                                    Toast.makeText(RequestsActivity.this, "Request Deleted Successfully", Toast.LENGTH_SHORT).show();
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {

                                                        }
                                                    }).show();

                                                }
                                            });
                                        }
                                    });


                                }
                            });


                        }
                    });
                }

                else if (type.equals("sent")){

                    holder.acceptButton.setEnabled(false);
                    holder.acceptButton.setVisibility(View.GONE);
                    holder.cancelButton.setEnabled(true);

                    userCol.document(list_user_id).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot dataSnapshot, @Nullable FirebaseFirestoreException e) {
                            if (dataSnapshot.exists()){
                                if (dataSnapshot.contains("image")){
                                    final String requestImage = dataSnapshot.get("image").toString();

                                    Picasso.get().load(requestImage).into(holder.profileImage);
                                }

                                final String requestName = dataSnapshot.get("name").toString();

                                holder.userName.setText(requestName);

                                holder.cancelButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(RequestsActivity.this);
                                        alertDialog.setTitle("Cancel request").setMessage("Do you really want to cancel a connect request?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                reqCol.document(currentUserId).collection("Requests").document(list_user_id).delete()                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()){
                                                            reqCol.document(list_user_id).collection("Requests").document(currentUserId).delete()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()){
                                                                                notifyItemRemoved(position);
                                                                                Toast.makeText(RequestsActivity.this, "You have cancelled the request to connect!", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                            }
                                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        }).show();

                                    }
                                });
                            }

                        }
                    });
                }

            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.zunit_request,viewGroup,false);
                return  new RequestViewHolder(view);
            }
        };

        requestListRv.setAdapter(adapter);
        adapter.startListening();

    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder{
        TextView userName ;
        CircleImageView profileImage;
        ImageView acceptButton,cancelButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.user_profile_name);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            acceptButton = itemView.findViewById(R.id.request_accept_btn);
            cancelButton = itemView.findViewById(R.id.request_cancel_btn);

        }
    }
}

