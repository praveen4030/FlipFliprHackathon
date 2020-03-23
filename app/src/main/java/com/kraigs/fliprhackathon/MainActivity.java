package com.kraigs.fliprhackathon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.kraigs.fliprhackathon.Boards.BoardDetailActivity;
import com.kraigs.fliprhackathon.Boards.InvitedCardsActivity;
import com.kraigs.fliprhackathon.Chat.ChatsListActivity;
import com.kraigs.fliprhackathon.Extra.GetTimeAgo;
import com.kraigs.fliprhackathon.Login.LoginActivity;
import com.kraigs.fliprhackathon.Model.Board;
import com.kraigs.fliprhackathon.Model.InvitedCards;
import com.kraigs.fliprhackathon.User.FriendsActivity;
import com.kraigs.fliprhackathon.User.RequestsActivity;
import com.kraigs.fliprhackathon.User.UsersDetailActivity;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private CircleImageView profilePicDrawer;
    private TextView userNameDrawer;
    private TextView userIDDrawer;

    CollectionReference userCol;
    String currentUserId;
    Toolbar toolbar;
    FloatingActionButton fab;
    CollectionReference boardRef;
    ProgressDialog loadingBar;

    RecyclerView boardRv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userCol = FirebaseFirestore.getInstance().collection("User");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fab = findViewById(R.id.fabMain);
        loadingBar = new ProgressDialog(this);

        boardRef = FirebaseFirestore.getInstance().collection("User").document(currentUserId).collection("Boards");
        boardRv = findViewById(R.id.boardRv);
        boardRv.setLayoutManager(new LinearLayoutManager(this));

        setUpRv();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boardDialog();
            }
        });

        drawerImplementation();

    }

    private void setUpRv() {

        Query query = boardRef.orderBy("timestamp");

        FirestoreRecyclerOptions<Board> options = new FirestoreRecyclerOptions.Builder<Board>()
                .setQuery(query,Board.class)
                .build();

        FirestoreRecyclerAdapter<Board,BoardHolder> adapter = new FirestoreRecyclerAdapter<Board, BoardHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull BoardHolder holder, int position, @NonNull Board model) {

                holder.boardTv.setText(getSnapshots().getSnapshot(position).getId());
                Timestamp timestamp = model.getTimestamp();
                GetTimeAgo gta = new GetTimeAgo();
                if (timestamp != null){
                    String time = gta.getTimeAgo(timestamp.getSeconds());
                    holder.timeTv.setText(time);
                }

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, BoardDetailActivity.class);
                        intent.putExtra("board",getSnapshots().getSnapshot(position).getId());
                        startActivity(intent);
                    }
                });

            }

            @NonNull
            @Override
            public BoardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.zunit_board,parent,false);
                return new BoardHolder(v);
            }
        };

        boardRv.setAdapter(adapter);
        adapter.startListening();
    }

    private void boardDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.zlayout_create_board);

        Button saveBt = dialog.findViewById(R.id.saveBt);
        TextInputEditText boardEt = dialog.findViewById(R.id.boardEt);
        Spinner visibilitySp = dialog.findViewById(R.id.visibilitySp);

        ArrayAdapter categorySpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.visibility, android.R.layout.simple_spinner_dropdown_item);
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        visibilitySp.setAdapter(categorySpinnerAdapter);

        final String[] visibility = new String[1];

        visibilitySp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                visibility[0] = (String) parent.getItemAtPosition(position);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        saveBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String board = boardEt.getText().toString();
                if (!TextUtils.isEmpty(board) && visibility[0] != null){
                    loadingBar.setTitle("Board");
                    loadingBar.setMessage("Please wait!");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    HashMap<String,Object> map = new HashMap<>();
                    map.put("timestamp", FieldValue.serverTimestamp());
                    map.put("visibility",visibility[0]);

                    boardRef.document(board).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){

                                loadingBar.dismiss();
                                Toast.makeText(MainActivity.this, "Board added successfully!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else{

                            }
                        }
                    });

                } else{
                    loadingBar.dismiss();
                    Toast.makeText(MainActivity.this, "Select all fields and continue", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();

    }

    private void drawerImplementation(){

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer,toolbar,R.string.open_drawer,R.string.close_drawer);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);

        toggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
            }
        });

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);

        userNameDrawer = (TextView) header.findViewById(R.id.userNameDrawer);
        userIDDrawer = (TextView) header.findViewById(R.id.userIDDrawer);
        profilePicDrawer = (CircleImageView) header.findViewById(R.id.userProfilePicDrawer);

        header.findViewById(R.id.editProfile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, UsersDetailActivity.class);
                intent.putExtra("purpose", "edit");
                startActivity(intent);
            }
        });

        userCol.document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (snapshot.exists()){

                    if (snapshot.contains("name")){
                        String name = snapshot.get("name").toString();
                        userNameDrawer.setText(name);
                    }

                    if (snapshot.contains("userID")){
                        String userID = snapshot.get("userID").toString();
                        userIDDrawer.setText(userID);
                    }


                    if (snapshot.contains("image")){
                        String image = snapshot.get("image").toString();
                        Picasso.get().load(image).placeholder(R.drawable.user_profile_image).into(profilePicDrawer);
                    }
                }
            }
        });
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (doubleBackToExitPressedOnce) {

            Intent a = new Intent(Intent.ACTION_MAIN);
            a.addCategory(Intent.CATEGORY_HOME);
            a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(a);
        }


        this.doubleBackToExitPressedOnce = true;

        Toast.makeText(this, "Press again to leave.", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 3000);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.log_out) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
            alertDialog.setTitle("Logout").setMessage("Do you want to logout?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build();
                    GoogleSignInClient mGoogleSignInClient;
                    mGoogleSignInClient = GoogleSignIn.getClient(MainActivity.this, gso);

                    mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull com.google.android.gms.tasks.Task<Void> task) {
                            Intent i2 = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(i2);
                            finish();
                            FirebaseAuth.getInstance().signOut();
                        }
                    });
                }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            }).show();

        } else if(id == R.id.chats){
            Intent intent = new Intent(MainActivity.this, ChatsListActivity.class);
            startActivity(intent);
        }else if(id == R.id.invites){
            Intent intent = new Intent(MainActivity.this, InvitedCardsActivity.class);
            startActivity(intent);
        }else if(id == R.id.requests){
            Intent intent = new Intent(MainActivity.this, RequestsActivity.class);
            startActivity(intent);
        }else if(id == R.id.friends){
            Intent intent = new Intent(MainActivity.this, FriendsActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;

    }

    private class BoardHolder extends RecyclerView.ViewHolder {

        TextView boardTv,timeTv;

        public BoardHolder(@NonNull View itemView) {
            super(itemView);

            boardTv = itemView.findViewById(R.id.nameTv);
            timeTv = itemView.findViewById(R.id.timeTv);

        }
    }
}
