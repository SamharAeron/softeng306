package com.softeng306team15.plantoid.Activities;

import static android.content.ContentValues.TAG;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.softeng306team15.plantoid.Adaptors.ItemAdaptor;
import com.softeng306team15.plantoid.ItemModels.IItem;
import com.softeng306team15.plantoid.UserModels.IUser;
import com.softeng306team15.plantoid.ItemModels.MainItem;
import com.softeng306team15.plantoid.UserModels.User;
import com.softeng306team15.plantoid.MyCallback;
import com.softeng306team15.plantoid.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class WishlistActivity extends AppCompatActivity {

    private class ViewHolder {
        LinearLayout discoverButton, logoutButton, navBar;
        TextView categoryNameText, emptyWishlistText;
        RecyclerView itemsRecyclerView;
        ScrollView scrollSection;
        RelativeLayout topBar;
        AnimationDrawable loadingAnimation;
        ImageView loadingAnimationImageView;

        public ViewHolder() {
            discoverButton = findViewById(R.id.discover_navbar_button);
            logoutButton = findViewById(R.id.profile_navbar_button);
            categoryNameText = findViewById(R.id.category_title_textView);
            emptyWishlistText = findViewById(R.id.emptyWishlistTextView);

            itemsRecyclerView = findViewById(R.id.categoryRecyclerView);

            loadingAnimationImageView = (ImageView) findViewById(R.id.leaf_animation);
            loadingAnimation = (AnimationDrawable) loadingAnimationImageView.getDrawable();

            navBar = findViewById(R.id.navbar);
            scrollSection = findViewById(R.id.scroll_section);
            topBar = findViewById(R.id.top_bar);
        }
    }

    ViewHolder vh;
    String userId;
    IUser user;
    int shortAnimationDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        userId = intent.getStringExtra("User");

        setContentView(R.layout.activity_wishlist);
        vh = new ViewHolder();
        vh.loadingAnimation.start();
        shortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        getUserData(() -> user.loadWishlist(() -> {
            List<String> wishlist = user.getWishlist();
            if (wishlist.size() == 0){
                removeLoadingAnimation();
                vh.emptyWishlistText.setVisibility(View.VISIBLE);
            }else{
                Log.d(TAG, "Wishlist items " + wishlist.size());
                getWishlistItemData(wishlist);
            }
        }));
        
        vh.categoryNameText.setText("Your Wishlist");
        vh.discoverButton.setOnClickListener(this::goDiscover);
        vh.logoutButton.setOnClickListener(this::goLogout);

    }

    public void getUserData(MyCallback callback) {
        // set to actual user's name
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference userDoc = db.collection("users").document(userId);
        userDoc.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot userDoc1 = task.getResult();
                if (userDoc1.exists()) {
                    user = userDoc1.toObject(User.class);
                    user.setId(userDoc1.getId());
                    callback.onCallback();
                } else {
                    Log.d(TAG, "No such document");
                }
            } else {
                Log.d(TAG, "get failed with ", task.getException());
            }
        });

    }

    /**
     * load all items in category from firestore
     */
    private void getWishlistItemData(List<String> wishlistIds){

        List<IItem> itemList = new LinkedList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (String itemId: wishlistIds){
            db.collection("items").document(itemId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot result = task.getResult();
                    if (result != null){
                        IItem item = result.toObject(MainItem.class);
                        item.setId(result.getId());
                        itemList.add(item);
                    }else {
                        Toast.makeText(getBaseContext(), "Item does not exist!", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "No such document");
                        removeLoadingAnimation();
                    }
                    if (itemList.size() == wishlistIds.size()){
                        Log.d(TAG, "got to run sub collection");
                        getItemSubCollections(itemList);
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                    Toast.makeText(getBaseContext(), "Loading item failed from Firestore!", Toast.LENGTH_LONG).show();
                    removeLoadingAnimation();
                }
            });
        }

    }

    /**
     * Load the image and tag subcollections for the wishlist items from firestore
     *
     * @param data items with type IItem
     */
    private void getItemSubCollections(List<IItem> data){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<IItem> wishlist = new LinkedList<>();

        //Due to async loading it is not known what finishes loading first. Callback counter ensures
        //adaptor is only called when all of the items have fully loaded.
        MyCallback callback = new MyCallback() {
            int responses = 0;
            @Override
            public void onCallback() {
                responses ++;
                Log.d(TAG, "callback called responses number " + responses);
                if (responses == 2*data.size()){
                    Log.d(TAG, "callback called adaptor");
                    propagateAdaptor(wishlist);
                    removeLoadingAnimation();
                }
            }
        };

        for (IItem item: data){
            db.collection("/items/"+item.getId()+"/images").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot results = task.getResult();
                    List<String> images = new ArrayList<>();
                    for(QueryDocumentSnapshot imageDoc: results){
                        images.add((String) imageDoc.get("image"));
                    }
                    item.setImages(images);
                    callback.onCallback();
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                    Toast.makeText(getBaseContext(), "Loading items images failed from Firestore!", Toast.LENGTH_LONG).show();
                }
            });
            //load all tags for item
            db.collection("items/"+item.getId()+"/tags").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot results = task.getResult();
                    List<String> tags = new ArrayList<>();
                    for(QueryDocumentSnapshot imageDoc: results){
                        tags.add((String) imageDoc.get("tagName"));
                    }
                    item.setTags(tags);
                    wishlist.add(item);
                    callback.onCallback();
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                    Toast.makeText(getBaseContext(), "Loading items tags failed from Firestore!", Toast.LENGTH_LONG).show();
                }
            });
        }


    }

    private void removeLoadingAnimation(){
        vh.loadingAnimation.stop();

        vh.navBar.setAlpha(0f);
        vh.navBar.setVisibility(View.VISIBLE);
        vh.navBar.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null);

        vh.topBar.setAlpha(0f);
        vh.topBar.setVisibility(View.VISIBLE);
        vh.topBar.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null);

        vh.scrollSection.setAlpha(0f);
        vh.scrollSection.setVisibility(View.VISIBLE);
        vh.scrollSection.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null);

        vh.loadingAnimationImageView.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        vh.loadingAnimationImageView.setVisibility(View.GONE);
                    }
                });
    }

    private void propagateAdaptor(List<IItem> data) {
        ItemAdaptor itemAdapter = new ItemAdaptor(data, R.layout.item_wishlist_card, userId);
        vh.itemsRecyclerView.setAdapter(itemAdapter);
        GridLayoutManager lm =new GridLayoutManager(this,calculateNumberOfColumns());
        vh.itemsRecyclerView.setLayoutManager(lm);
    }

    private int calculateNumberOfColumns(){
        DisplayMetrics displayMetrics = getBaseContext().getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int columnNo = (int) dpWidth/180; //180 is item card width
        if (columnNo < 1){ //show at least one column
            columnNo = 1;
        }
        return columnNo;
    }
    public void goDiscover(View v) {
        Intent mainActivityIntent = new Intent(getBaseContext(), MainActivity.class);
        mainActivityIntent.putExtra("User", userId);
        startActivity(mainActivityIntent);
    }

    public void goLogout(View v) {
        FirebaseAuth.getInstance().signOut();
        Intent logoutIntent = new Intent(getBaseContext(), LogInActivity.class);
        startActivity(logoutIntent);
    }

}
