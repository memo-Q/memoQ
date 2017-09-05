package com.memoq.android.models;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import timber.log.Timber;

public class User {

    private static User sCurrentUser;

    public String email;

    private String mKey;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String email) {
        this.email = email;
    }

    public static void setCurrent(User user) {
        sCurrentUser = user;
    }

    public static User getCurrent() {
        return sCurrentUser;
    }

    public static FirebaseUser getCurrentFirebaseUser() {
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Timber.d("Current user was not found.");
        }
        return firebaseUser;
    }

    private static DatabaseReference getReference(final Class clazz) {
        return FirebaseDatabase.getInstance().getReference(clazz.getSimpleName());
    }

    private static DatabaseReference getReferenceChild(final String key) {
        return getReference(User.class).child(key);
    }

    public static void find(final String key, final ValueEventListener listener) {
        final DatabaseReference reference = getReferenceChild(key);
        reference.keepSynced(true);
        reference.addListenerForSingleValueEvent(listener);
    }

    public void insertToFirebase() {
        final FirebaseUser firebaseUser = getCurrentFirebaseUser();
        if (firebaseUser == null) {
            return;
        }

        if (mKey == null) {
            mKey = firebaseUser.getUid();
            DatabaseReference reference = getReferenceChild(mKey);
            reference.setValue(this);
        }
    }

    public void updateToFirebase() {
        if (mKey != null) {
            DatabaseReference reference = getReferenceChild(mKey);
            reference.setValue(this);
        }
    }

    @Exclude
    public void setKey(final String key) {
        mKey = key;
    }

}
