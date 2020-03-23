package com.kraigs.fliprhackathon.Chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.kraigs.fliprhackathon.R;
import com.kraigs.fliprhackathon.User.FriendsActivity;
import com.kraigs.fliprhackathon.User.UsersActivity;
import com.squareup.picasso.Picasso;

import javax.annotation.Nullable;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsListActivity extends AppCompatActivity {

    private RecyclerView chatsList;
    private DatabaseReference chatRef,friendsRef,channelRef;
    private FirebaseAuth mAuth;
    private String currentUserId;
    FloatingActionButton chatFb;
    LinearLayoutManager linearLayoutManager;
    CollectionReference userCol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats_list);

        mAuth = FirebaseAuth.getInstance();

        currentUserId = mAuth.getCurrentUser().getUid();
        chatRef = FirebaseDatabase.getInstance().getReference().child("Message").child(currentUserId);
        channelRef = FirebaseDatabase.getInstance().getReference().child("ChatChannel").child(currentUserId);
        chatsList = (RecyclerView) findViewById(R.id.chats_list);
        linearLayoutManager = new LinearLayoutManager(ChatsListActivity.this);
        chatsList.setLayoutManager(linearLayoutManager);
        chatFb = findViewById(R.id.chatFb);
        userCol = FirebaseFirestore.getInstance().collection("User");

        channelRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    findViewById(R.id.noChat).setVisibility(View.GONE);
                    chatsList.setVisibility(View.VISIBLE);

                } else{
                    findViewById(R.id.noChat).setVisibility(View.VISIBLE);
                    chatsList.setVisibility(View.GONE);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        friendsRef = FirebaseDatabase.getInstance().getReference().child("Friends").child(currentUserId);

        chatFb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatsListActivity.this, FriendsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        Query channelQuery = FirebaseDatabase.getInstance().getReference().child("ChatChannel").child(currentUserId).orderByChild("timestamp");
        FirebaseRecyclerOptions<User> options = new FirebaseRecyclerOptions.Builder<User>()
                .setQuery(channelQuery,User.class)
                .build();

        FirebaseRecyclerAdapter<User,ChatsViewHolder> adapter = new FirebaseRecyclerAdapter<User, ChatsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatsViewHolder holder, int position, @NonNull User model) {
                final String userIDs = getRef(position).getKey();
                final String profileImage[] = {"default_image"};
                holder.onlineStatus.setVisibility(View.GONE);

                Query lastQuery = chatRef.child(userIDs).orderByKey().limitToLast(1);
                lastQuery.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        String message = dataSnapshot.child("message").getValue().toString();
//                        holder.userStatus.setText(message);

                        for (DataSnapshot child: dataSnapshot.getChildren()) {
                            String message = child.child("message").getValue().toString();
                            String seen = child.child("seen").getValue().toString();
                            String type = child.child("type").getValue().toString();
                            if (type.equals("image")){
                                holder.userStatus.setText("New Image");
                            } else  if (type.equals("pdf")){
                                holder.userStatus.setText("New File");
                            } else{
                                holder.userStatus.setText(message);
                            }

                            if (seen.equals("false")){
                                holder.userStatus.setTypeface(null, Typeface.BOLD);
                                holder.userStatus.setTextColor(Color.BLACK);
                            }

                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //Handle possible errors.
                    }
                });

                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ChatsListActivity.this);
                        alertDialog.setTitle("Delete").setMessage("Do you want to delete this chat?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                notifyItemRemoved(position);

                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();

                        return true;
                    }
                });

                userCol.document(userIDs).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot dataSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (dataSnapshot.exists()){
                            if (dataSnapshot.contains("image")){
                                profileImage[0] = dataSnapshot.get("image").toString();
                                Picasso.get().load(profileImage[0])
                                        .placeholder(R.drawable.user_profile_image)
                                        .into(holder.profileImage);
                            }

                            if(dataSnapshot.contains("online")){
                                String onlineStatus = dataSnapshot.get("online").toString();
                                if (onlineStatus.equals("true")){
                                    holder.onlineStatus.setVisibility(View.VISIBLE);
                                } else{
                                    holder.onlineStatus.setVisibility(View.GONE);
                                }
                            }

                            if(dataSnapshot.contains("name")){
                                final String profileName = dataSnapshot.get("name").toString();
                                holder.userName.setText(profileName);
                            }


                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    Intent intent = new Intent(ChatsListActivity.this, ChatActivity.class);
                                    intent.putExtra("visit_user_id",userIDs);
                                    startActivity(intent);

                                }
                            });
                        }

                    }
                });
            }

            @NonNull
            @Override
            public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.zunit_chat,viewGroup,false);
                return new ChatsViewHolder(view);
            }
        };

        chatsList.setAdapter(adapter);
        linearLayoutManager.smoothScrollToPosition(chatsList, null, 0);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                chatsList.smoothScrollToPosition(positionStart + 1);
                linearLayoutManager.setReverseLayout(true);
                linearLayoutManager.setStackFromEnd(true);
            }
        });

        adapter.startListening();
    }

    public class ChatsViewHolder extends RecyclerView.ViewHolder
    {
        TextView userName , userStatus;
        CircleImageView profileImage;
        ImageView onlineStatus;

        public ChatsViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userNameTv);
            userStatus = itemView.findViewById(R.id.messageTv);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            onlineStatus = itemView.findViewById(R.id.onlineStatus);
        }
    }
}
