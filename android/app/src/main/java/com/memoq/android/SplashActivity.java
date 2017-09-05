package com.memoq.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.memoq.android.models.User;

import timber.log.Timber;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mFirebaseAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mFirebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        findUser();
    }

    private void findUser() {
        final FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            Timber.d("onAuthStateChanged:signed_out");
            FirebaseAuth.getInstance().signOut();
            goToNextActivity(LoginActivity.class);
            return;
        }

        Timber.d("onAuthStateChanged:signed_in:%s", firebaseUser.getUid());
        User.find(firebaseUser.getUid(), new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final User user = dataSnapshot.getValue(User.class);
                if (user == null) {
                    FirebaseAuth.getInstance().signOut();
                    goToNextActivity(LoginActivity.class);
                    return;
                }
                user.setKey(dataSnapshot.getKey());
                User.setCurrent(user);
                goToNextActivity(MainActivity.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.e(databaseError.toException());
            }
        });
    }

    private void goToNextActivity(final Class<?> clz) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, clz);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                finish();
            }
        }, 500);
    }

}