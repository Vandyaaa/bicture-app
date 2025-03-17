package com.example.imaginate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.imaginate.models.Album;
import com.example.imaginate.models.BookmarkData;
import com.example.imaginate.models.CollageItem;
import com.example.imaginate.models.Comment;
import com.example.imaginate.models.SearchItem;
import com.example.imaginate.models.UserRecommendation;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;



public class SearchFragment extends Fragment {
    private EditText searchEditText;
    private RecyclerView recyclerView;
    private SearchAdapter searchAdapter;
    private List<SearchItem> searchItems;
    private RecyclerView suggestionsRecyclerView;
    private DatabaseReference databaseRef;
    private SharedPreferences sharedPreferences;
    private static final String SEARCH_HISTORY_KEY = "search_history";
    private static final int MAX_SEARCH_HISTORY = 10;
    private RecyclerView recommendationRecyclerView;
    private RecommendationAdapter recommendationAdapter;
    private List<UserRecommendation> recommendationList;
    private String currentUserId;
    private static final String KEY_RECYCLER_STATE = "recycler_state";
    private Parcelable recyclerViewState;
    private Map<String, Boolean> likeStatusCache = new HashMap<>();
    private Map<String, Boolean> followStatusCache = new HashMap<>();
    private Map<String, Boolean> bookmarkStatusCache = new HashMap<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private View loadingPage;
    private LinearLayout noneSearch;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        searchEditText = view.findViewById(R.id.searchEditText);
        recyclerView = view.findViewById(R.id.searchRecyclerView);
        loadingPage = view.findViewById(R.id.loading_page);
        noneSearch = view.findViewById(R.id.noneSearch);
        recyclerView.setItemViewCacheSize(20);
        if (savedInstanceState != null) {
            recyclerViewState = savedInstanceState.getParcelable(KEY_RECYCLER_STATE);
            if (recyclerViewState != null && recyclerView.getLayoutManager() != null) {
                recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
            }
        }
        // Initialize RecyclerView
        searchItems = new ArrayList<>();
        searchAdapter = new SearchAdapter(getContext(), searchItems);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(searchAdapter);

        // Get userId from SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("userId", null);

        recommendationRecyclerView = view.findViewById(R.id.recommendationRecyclerView);
        recommendationList = new ArrayList<>();
        recommendationAdapter = new RecommendationAdapter(getContext(), recommendationList);
        recommendationRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recommendationRecyclerView.setAdapter(recommendationAdapter);

        if (currentUserId != null) {
            // Reference to Firebase
            databaseRef = FirebaseDatabase.getInstance().getReference("users");

            // Set up search on Enter key press
            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String query = searchEditText.getText().toString().trim();
                    if (!query.isEmpty()) {
                        performSearch(query);
                        return true;
                    }
                }
                return false;
            });



            // Display search history when search EditText is empty
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                // Ubah bagian onTextChanged di TextWatcher
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim();
                    if (query.length() > 0) {
                        suggestionsRecyclerView.setVisibility(View.VISIBLE);
                        recommendationRecyclerView.setVisibility(View.GONE);
                        searchEditText.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.search_24, 0, R.drawable.silang, 0);
                        searchEditText.setCompoundDrawablePadding(10);
                        displayAlbumSuggestions(query); // Menampilkan saran album saat mengetik
                    } else {
                        suggestionsRecyclerView.setVisibility(View.GONE);
                        recommendationRecyclerView.setVisibility(View.VISIBLE);
                        searchEditText.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.search_24, 0, 0, 0);
                        searchItems.clear();
                        searchAdapter.notifyDataSetChanged();
                        noneSearch.setVisibility(View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            searchEditText.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (searchEditText.getCompoundDrawables()[2] != null) { // Check if end drawable exists
                        boolean isXButtonClicked = event.getRawX() >= (searchEditText.getRight()
                                - searchEditText.getCompoundDrawables()[2].getBounds().width()
                                - searchEditText.getPaddingEnd());

                        if (isXButtonClicked) {
                            searchEditText.setText("");
                            searchEditText.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.search_24, 0, 0, 0);
                            noneSearch.setVisibility(View.GONE);

                            // Clear focus and hide keyboard
                            searchEditText.clearFocus();
                            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                            }
                            return true;
                        }
                    }
                }
                return false;
            });


            // Handle focus changes
            searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    if (searchEditText.getText().toString().trim().isEmpty()) {
                        recommendationRecyclerView.setVisibility(View.VISIBLE);
                        searchItems.clear();
                        searchAdapter.notifyDataSetChanged();
                    }
                } else {
                    String searchText = searchEditText.getText().toString().trim();
                    if (searchText.isEmpty()) {
                        recommendationRecyclerView.setVisibility(View.VISIBLE);
                        searchItems.clear();
                        searchAdapter.notifyDataSetChanged();
                    } else {
                        recommendationRecyclerView.setVisibility(View.GONE);
                        displaySearchHistory();
                    }
                }
            });
        } else {
            Toast.makeText(getContext(), "User not found, please log in again", Toast.LENGTH_SHORT).show();
        }
            // Show search history initially
        loadUserRecommendations();


        swipeRefreshLayout.setColorSchemeResources(R.color.yellow);
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.blacksmooth2);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchData();
            swipeRefreshLayout.setRefreshing(false);
        });

        fetchData();
        setupSuggestionRecyclerView(view);
        return view;
    }

    private void setupSuggestionRecyclerView(View view) {
        // Initialize suggestion list
        searchItems = new ArrayList<>();
        suggestionsRecyclerView = view.findViewById(R.id.suggestionsRecyclerView);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup Adapter
        searchAdapter = new SearchAdapter(getContext(), searchItems);
        suggestionsRecyclerView.setAdapter(searchAdapter);
    }

    // Fungsi untuk menampilkan saran pencarian dari judul album
    private void displayAlbumSuggestions(String searchQuery) {
        if (searchQuery.isEmpty()) {
            searchItems.clear();
            searchAdapter.notifyDataSetChanged();
            noneSearch.setVisibility(View.GONE);
            return;
        }

        // Konversi searchQuery ke lowercase untuk pencarian case-insensitive
        final String searchTermLower = searchQuery.toLowerCase();

        // Referensi ke database users
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Ambil semua judul album yang cocok
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear items
                searchItems.clear();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();

                    // Ambil semua album dari user ini
                    DataSnapshot albumsSnapshot = userSnapshot.child("album");
                    for (DataSnapshot albumSnapshot : albumsSnapshot.getChildren()) {
                        String title = albumSnapshot.child("title").getValue(String.class);
                        String albumId = albumSnapshot.getKey();

                        // Periksa apakah title mengandung searchQuery (case-insensitive)
                        if (title != null && title.toLowerCase().contains(searchTermLower)) {
                            // Buat SearchItem sederhana yang hanya berisi judul
                            SearchItem suggestionItem = new SearchItem(title, "", "suggestion", null);
                            suggestionItem.setAlbumId(albumId);
                            searchItems.add(suggestionItem);

                            // Batasi jumlah saran (opsional)
                            if (searchItems.size() >= 10) {
                                break;
                            }
                        }
                    }

                    // Jika sudah mencapai batas maksimum, hentikan iterasi user
                    if (searchItems.size() >= 10) {
                        break;
                    }
                }

                // Update tampilan
                searchAdapter.notifyDataSetChanged();
                if (searchItems.isEmpty()) {
                    noneSearch.setVisibility(View.VISIBLE);
                } else {
                    noneSearch.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("AlbumSuggestions", "Error getting suggestions: " + databaseError.getMessage());
            }
        });
    }


    // Fungsi helper untuk mengambil data pengguna (username dan foto profil)
    private class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {
        private List<SearchItem> suggestions;

        public SuggestionAdapter(List<SearchItem> suggestions) {
            this.suggestions = suggestions;
        }

        @NonNull
        @Override
        public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_suggestion, parent, false);


            return new SuggestionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
            SearchItem suggestion = suggestions.get(position);
            holder.titleTextView.setText(suggestion.getTitle());

            // Set event klik pada suggestion item
            holder.itemView.setOnClickListener(v -> {
                // Masukkan teks ke dalam searchEditText
                searchEditText.setText(suggestion.getTitle());
                searchEditText.setSelection(searchEditText.getText().length()); // Pindahkan kursor ke akhir teks

                // Sembunyikan daftar saran
                searchItems.clear();
                notifyDataSetChanged();

                // Optional: langsung memulai pencarian dengan teks ini
                performSearch(suggestion.getTitle());
            });
        }

        @Override
        public int getItemCount() {
            return suggestions.size();
        }

        public class SuggestionViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView;

            public SuggestionViewHolder(@NonNull View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.textViewSuggestion);
            }
        }
    }


    private void fetchData() {
        loadingPage.setVisibility(View.VISIBLE);
        // Your logic to fetch data and update the searchItems list
        // For example:
        searchItems.clear();
        // Add new data to searchItems
        searchAdapter.notifyDataSetChanged();
        loadingPage.setVisibility(View.GONE);
    }

    private void loadUserRecommendations() {
        loadingPage.setVisibility(View.VISIBLE);
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.orderByChild("followers").startAt(1000).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recommendationList.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String username = userSnapshot.child("username").getValue(String.class);
                    String photoProfile = userSnapshot.child("photoprofile").getValue(String.class);
                    Long followers = userSnapshot.child("followers").getValue(Long.class);

                    if (username != null && followers != null && followers >= 1000) {
                        UserRecommendation recommendation = new UserRecommendation(
                                userId,
                                username,
                                photoProfile != null ? photoProfile : "",
                                followers
                        );
                        recommendationList.add(recommendation);
                    }
                }
                recommendationAdapter.notifyDataSetChanged();
                loadingPage.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load recommendations", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder> {
        private Context context;
        private List<UserRecommendation> recommendations;

        public RecommendationAdapter(Context context, List<UserRecommendation> recommendations) {
            this.context = context;
            this.recommendations = recommendations;
        }

        @NonNull
        @Override
        public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_profile_recomendation, parent, false);
            return new RecommendationViewHolder(view);
        }

        private void navigateToUserProfile(String userId) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Bundle bundle = new Bundle();
                        bundle.putString("userId", userId);
                        bundle.putString("username", snapshot.child("username").getValue(String.class));
                        bundle.putString("photoprofile", snapshot.child("photoprofile").getValue(String.class));
                        bundle.putString("bio", snapshot.child("bio").getValue(String.class));
                        bundle.putString("location", snapshot.child("location").getValue(String.class));
                        bundle.putString("tagname", snapshot.child("tagname").getValue(String.class));
                        bundle.putLong("followers", snapshot.child("followers").getValue(Long.class) != null ?
                                snapshot.child("followers").getValue(Long.class) : 0);
                        bundle.putLong("following", snapshot.child("followed").getValue(Long.class) != null ?
                                snapshot.child("followed").getValue(Long.class) : 0);
                        bundle.putLong("post", snapshot.child("post").getValue(Long.class) != null ?
                                snapshot.child("post").getValue(Long.class) : 0);

                        ProfileShowFragment profileShowFragment = new ProfileShowFragment();
                        profileShowFragment.setArguments(bundle);

                        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();

                        // Get the current active fragment's tag
                        String currentFragmentTag = null;
                        Fragment currentFragment = fragmentManager.findFragmentById(R.id.frame_layout);
                        if (currentFragment != null && currentFragment.isVisible()) {
                            currentFragmentTag = currentFragment.getTag();
                        }

                        // Create a unique backstack name that includes the parent fragment tag
                        String backStackName = "ProfileShowFragment_" + currentFragmentTag;

                        FragmentTransaction transaction = fragmentManager.beginTransaction();

                        // Keep the current fragment visible, but add the profile fragment on top
                        transaction.add(R.id.frame_layout, profileShowFragment, "ProfileShowFragment");

                        // Add to back stack with the unique name
                        transaction.addToBackStack(backStackName);
                        transaction.commit();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Failed to load user profile", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
            UserRecommendation recommendation = recommendations.get(position);

            holder.usernameRec.setText(recommendation.getUsername());
            String formattedFollowers = formatLikesCount(recommendation.getFollowers());
            holder.followersRec.setText(formattedFollowers + " Followers");
            // Load profile image using Glide
            Glide.with(context)
                    .load(recommendation.getPhotoProfile())
                    .placeholder(R.drawable.default_profile_bct_v1)
                    .error(R.drawable.default_profile_bct_v1)
                    .into(holder.photoprofileRec);

            // Handle visit profile click
            holder.visitProfile.setOnClickListener(v -> {
                navigateToUserProfile(recommendation.getUserId());
            });
        }

        @Override
        public int getItemCount() {
            return recommendations.size();
        }

        class RecommendationViewHolder extends RecyclerView.ViewHolder {
            ShapeableImageView photoprofileRec;
            TextView usernameRec;
            TextView followersRec;  // Added TextView for followers
            TextView visitProfile;

            public RecommendationViewHolder(@NonNull View itemView) {
                super(itemView);
                photoprofileRec = itemView.findViewById(R.id.photoprofileRec);
                usernameRec = itemView.findViewById(R.id.usernameRec);
                followersRec = itemView.findViewById(R.id.followersRec);  // Initialize followers TextView
                visitProfile = itemView.findViewById(R.id.visitProfile);
            }
        }

        private String formatLikesCount(long count) {
            if (count >= 1_000_000_000) {
                return String.format("%.1fb", count / 1_000_000_000.0);
            } else if (count >= 1_000_000) {
                return String.format("%.1fm", count / 1_000_000.0);
            } else if (count >= 1_000) {
                return String.format("%.1fk", count / 1_000.0);
            }
            return String.valueOf(count);
        }
    }

    private void saveSearchHistory(String query) {
        if (currentUserId == null) return;

        DatabaseReference historyRef = databaseRef
                .child(currentUserId)
                .child("settings")
                .child("historysearch");

        // Get current history
        historyRef.orderByChild("timestamp").limitToLast(MAX_SEARCH_HISTORY)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Check if query already exists
                        boolean exists = false;
                        for (DataSnapshot historySnapshot : snapshot.getChildren()) {
                            if (query.equals(historySnapshot.child("query").getValue(String.class))) {
                                exists = true;
                                // Update timestamp of existing query
                                historySnapshot.getRef().child("timestamp").setValue(ServerValue.TIMESTAMP);
                                break;
                            }
                        }

                        if (!exists) {
                            // Add new search query
                            String historyId = historyRef.push().getKey();
                            if (historyId != null) {
                                Map<String, Object> historyData = new HashMap<>();
                                historyData.put("query", query);
                                historyData.put("timestamp", ServerValue.TIMESTAMP);
                                historyRef.child(historyId).setValue(historyData);
                            }

                            // Remove oldest entry if exceeding MAX_SEARCH_HISTORY
                            if (snapshot.getChildrenCount() >= MAX_SEARCH_HISTORY) {
                                DataSnapshot oldestSnapshot = null;
                                long oldestTimestamp = Long.MAX_VALUE;

                                for (DataSnapshot historySnapshot : snapshot.getChildren()) {
                                    Long timestamp = historySnapshot.child("timestamp").getValue(Long.class);
                                    if (timestamp != null && timestamp < oldestTimestamp) {
                                        oldestTimestamp = timestamp;
                                        oldestSnapshot = historySnapshot;
                                    }
                                }

                                if (oldestSnapshot != null) {
                                    oldestSnapshot.getRef().removeValue();
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to save search history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performSearch(String query) {
        // Clear recommendation view
        recommendationRecyclerView.setVisibility(View.GONE);
        if (recyclerView != null && recyclerView.getLayoutManager() != null) {
            recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
        }
        // Clear all search items and update adapter
        searchItems.clear();
        searchAdapter.notifyDataSetChanged();

        // Save search query to history
        saveSearchHistory(query);

        // Set LinearLayoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Determine search type
        boolean isUserSearch = query.startsWith("@");
        String searchTerm = isUserSearch ? query.substring(1).toLowerCase() : query.toLowerCase();

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear items again to ensure no leftovers
                searchItems.clear();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    if (isUserSearch) {
                        // Get both username and tagName
                        String username = userSnapshot.child("username").getValue(String.class);
                        String tagName = userSnapshot.child("tagName").getValue(String.class);
                        String photoProfile = userSnapshot.child("photoprofile").getValue(String.class);
                        String userId = userSnapshot.getKey();

                        // Search in both username and tagName
                        if ((username != null && username.toLowerCase().contains(searchTerm)) ||
                                (tagName != null && tagName.toLowerCase().contains(searchTerm))) {
                            SearchItem userItem = new SearchItem(
                                    username != null ? username : "", // Set username
                                    photoProfile != null ? photoProfile : "",
                                    "user",
                                    userId
                            );
                            // Add tagName to the SearchItem
                            userItem.setTagName(tagName != null ? tagName : "");
                            searchItems.add(userItem);
                        }
                    } else {
                        // Album search logic remains unchanged
                        DataSnapshot albumsSnapshot = userSnapshot.child("album");
                        for (DataSnapshot albumSnapshot : albumsSnapshot.getChildren()) {
                            String title = albumSnapshot.child("title").getValue(String.class);
                            String description = albumSnapshot.child("description").getValue(String.class);
                            String url = albumSnapshot.child("url").getValue(String.class);
                            String albumId = albumSnapshot.getKey();
                            String albumUserId = userSnapshot.getKey();
                            String username = userSnapshot.child("username").getValue(String.class);
                            String photoProfile = userSnapshot.child("photoprofile").getValue(String.class);
                            Long timestamp = albumSnapshot.child("timestamp").getValue(Long.class);

                            boolean matchesTitle = title != null && title.toLowerCase().contains(searchTerm);
                            boolean matchesDescription = description != null && description.toLowerCase().contains(searchTerm);

                            if (matchesTitle || matchesDescription) {
                                SearchItem albumItem = new SearchItem(
                                        title,
                                        url != null ? url : "",
                                        "album",
                                        userSnapshot.getKey(),
                                        username,
                                        photoProfile,
                                        description
                                );
                                albumItem.setAlbumId(albumId);
                                if (timestamp != null) {
                                    albumItem.setUploadDate(formatUploadDate(timestamp));
                                }
                                searchItems.add(albumItem);
                            }
                        }
                    }
                }

                // Hide keyboard after search
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                View focusedView = requireActivity().getCurrentFocus();
                if (focusedView != null) {
                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    focusedView.clearFocus();
                }

                // Update UI
                searchAdapter.notifyDataSetChanged();
                if (searchItems.isEmpty()) {
                    noneSearch.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "No results found", Toast.LENGTH_SHORT).show();
                } else {
                    noneSearch.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Search failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        if (recyclerViewState != null && recyclerView.getLayoutManager() != null) {
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        }
    }

    private String formatUploadDate(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }

    private void displaySearchHistory() {
        if (currentUserId == null) return;

        DatabaseReference historyRef = databaseRef
                .child(currentUserId)
                .child("settings")
                .child("historysearch");

        historyRef.orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        searchItems.clear();
                        List<SearchItem> tempItems = new ArrayList<>();

                        for (DataSnapshot historySnapshot : snapshot.getChildren()) {
                            String query = historySnapshot.child("query").getValue(String.class);
                            if (query != null) {
                                SearchItem searchHistoryItem = new SearchItem(
                                        query,
                                        "",
                                        "history",
                                        historySnapshot.getKey()  // Store history ID for deletion
                                );
                                tempItems.add(searchHistoryItem);
                            }
                        }

                        // Sort by timestamp (newest first)
                        tempItems.sort((a, b) -> {
                            Long timestampA = snapshot.child(a.getUserId()).child("timestamp").getValue(Long.class);
                            Long timestampB = snapshot.child(b.getUserId()).child("timestamp").getValue(Long.class);
                            return Long.compare(timestampB != null ? timestampB : 0,
                                    timestampA != null ? timestampA : 0);
                        });

                        searchItems.addAll(tempItems);
                        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                        searchAdapter.notifyDataSetChanged();

                        if (searchItems.isEmpty()) {
                            noneSearch.setVisibility(View.VISIBLE);
                        } else {
                            noneSearch.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to load search history", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_USER = 1;
        private static final int TYPE_ALBUM = 2;
        private static final int TYPE_HISTORY = 3;
        private Context context;
        private List<SearchItem> searchItems;

        public SearchAdapter(Context context, List<SearchItem> searchItems) {
            this.context = context;
            this.searchItems = searchItems;
        }

        @Override
        public int getItemViewType(int position) {
            SearchItem item = searchItems.get(position);
            if (item.getType().equals("user")) return TYPE_USER;
            if (item.getType().equals("album")) return TYPE_ALBUM;
            return TYPE_HISTORY;
        }

        @Override
        public int getItemCount() {
            return searchItems.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(context);
            if (viewType == TYPE_USER) {
                View view = inflater.inflate(R.layout.item_preson_search, parent, false);
                return new UserSearchViewHolder(view);
            } else if (viewType == TYPE_ALBUM) {
                // Use the item_upload layout for albums
                View view = inflater.inflate(R.layout.item_upload, parent, false);
                return new AlbumSearchViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_search_history, parent, false);
                return new HistorySearchViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            SearchItem item = searchItems.get(position);

            if (holder instanceof UserSearchViewHolder) {
                UserSearchViewHolder userHolder = (UserSearchViewHolder) holder;
                userHolder.usernameTextView.setText(item.getTitle());
                userHolder.tagNameTextView.setText(item.getTagName());

                Glide.with(context)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.profiledefault)
                        .error(R.drawable.profiledefault)
                        .into(userHolder.profileImageView);

                // Add click listener to navigate to user profile
                userHolder.itemView.setOnClickListener(v -> {
                    String userId = item.getUserId();
                    navigateToUserProfile(userId);
                });
            } else if (holder instanceof AlbumSearchViewHolder) {
                AlbumSearchViewHolder albumHolder = (AlbumSearchViewHolder) holder;
                String currentUserId = sharedPreferences.getString("userId", null);




                DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("users")
                        .child(item.getUserId())
                        .child("album")
                        .child(item.getAlbumId());

                albumRef.child("userLikes").orderByChild("userId").equalTo(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                boolean hasLiked = snapshot.exists();
                                albumHolder.btnLike.setImageResource(hasLiked ?
                                        R.drawable.heart_24filled : R.drawable.heart_24);

                                // Get likes count
                                albumRef.child("likes").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot likesSnapshot) {
                                        Long likesCount = likesSnapshot.getValue(Long.class);
                                        if (likesCount != null) {
                                            albumHolder.likeCount.setText(formatLikesCount(likesCount));
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(context, "Failed to load likes count", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(context, "Failed to check like status", Toast.LENGTH_SHORT).show();
                            }
                        });
                albumHolder.btnLike.setOnClickListener(v -> {
                    String albumId = item.getAlbumId();
                    String userId = item.getUserId();

                    // Start the like button animation
                    Animation likeAnimation = AnimationUtils.loadAnimation(context, R.anim.like_button_animation);
                    albumHolder.btnLike.startAnimation(likeAnimation);

                    // Dapatkan jumlah likes saat ini dari database
                    DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference("users")
                            .child(userId)
                            .child("album")
                            .child(albumId)
                            .child("likes");

                    likesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            // Ambil jumlah likes saat ini
                            Long currentLikes = snapshot.getValue(Long.class);
                            currentLikes = (currentLikes != null) ? currentLikes : 0;

                            // Cek apakah user sudah like
                            DatabaseReference userLikesRef = snapshot.getRef().getParent().child("userLikes");
                            Long finalCurrentLikes = currentLikes;
                            userLikesRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userLikeSnapshot) {
                                    boolean hasLiked = userLikeSnapshot.exists();

                                    // Update likes
                                    long newLikesCount = hasLiked ? finalCurrentLikes - 1 : finalCurrentLikes + 1;

                                    // Update database
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("/users/" + userId + "/album/" + albumId + "/likes", newLikesCount);

                                    if (hasLiked) {
                                        // Hapus like
                                        updates.put("/users/" + userId + "/album/" + albumId + "/userLikes/" + currentUserId, null);
                                    } else {
                                        // Tambah like
                                        updates.put("/users/" + userId + "/album/" + albumId + "/userLikes/" + currentUserId, true);
                                    }

                                    FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                // Update UI
                                                albumHolder.likeCount.setText(formatLikesCount(newLikesCount));
                                                albumHolder.btnLike.setImageResource(hasLiked ?
                                                        R.drawable.heart_24 : R.drawable.heart_24filled);
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(context, "Failed to update likes", Toast.LENGTH_SHORT).show();
                                            });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(context, "Failed to check like status", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(context, "Failed to load likes", Toast.LENGTH_SHORT).show();
                        }
                    });
                });

                albumHolder.titleTextView.setText(item.getTitle());
                albumHolder.uploadDateTextView.setText(item.getUploadDate());
                albumHolder.descriptionTextView.setText(item.getDescription());
                albumHolder.usernameTextView.setText(item.getUsername());

                // Load album image
                Glide.with(context)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.blacksmooth2_drawable)
                        .error(R.drawable.blacksmooth2_drawable)
                        .into(albumHolder.albumImageView);

                // Load profile image
                Glide.with(context)
                        .load(item.getPhotoProfile())
                        .placeholder(R.drawable.default_profile_bct_v1)
                        .error(R.drawable.default_profile_bct_v1)
                        .into(albumHolder.profileImageView);

                if (currentUserId != null && item.getUserId() != null) {
                    if (currentUserId.equals(item.getUserId())) {
                        albumHolder.followButton.setVisibility(View.GONE);
                    } else {
                        albumHolder.followButton.setVisibility(View.VISIBLE);
                        checkFollowStatus(currentUserId, item.getUserId(), albumHolder);
                        albumHolder.followButton.setOnClickListener(v ->
                                handleFollow(currentUserId, item.getUserId(), albumHolder));
                    }
                }

                albumHolder.btnComment.setOnClickListener(v -> {
                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(holder.itemView.getContext(), R.style.BottomSheetDialogTheme);
                    View bottomSheetView = LayoutInflater.from(holder.itemView.getContext())
                            .inflate(R.layout.fragment_comment, null);

                    bottomSheetDialog.setContentView(bottomSheetView);

                    Window window = bottomSheetDialog.getWindow();
                    if (window != null) {
                        // Remove the SOFT_INPUT_ADJUST_RESIZE flag
                        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
                    }



                    bottomSheetDialog.setContentView(bottomSheetView);
                    bottomSheetDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);



                    RecyclerView commentRecyclerView = bottomSheetView.findViewById(R.id.commentRecyclerView);
                    EditText commentEditText = bottomSheetView.findViewById(R.id.commentEditText);
                    ImageButton sendButton = bottomSheetView.findViewById(R.id.sendButton);
                    ConstraintLayout commentBox = bottomSheetView.findViewById(R.id.commentBox);
                    ConstraintLayout containerTag = bottomSheetView.findViewById(R.id.containerTag);
                    TextView usernameTag = bottomSheetView.findViewById(R.id.usernameTag);
                    ImageButton cancelTag = bottomSheetView.findViewById(R.id.cancelTag);
                    LinearLayout noneTextView = bottomSheetView.findViewById(R.id.noneTextView);
                    ShapeableImageView photoProfileCmb = bottomSheetView.findViewById(R.id.photoprofilecmb);

                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String photoProfileUrl = snapshot.child("photoprofile").getValue(String.class);

                            if (photoProfileUrl != null && !photoProfileUrl.isEmpty()) {
                                // Load image using Glide
                                Glide.with(holder.itemView.getContext())
                                        .load(photoProfileUrl)
                                        .placeholder(R.color.blacksmooth2)
                                        .error(R.color.blacksmooth2)
                                        .into(photoProfileCmb);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle error
                            photoProfileCmb.setImageResource(R.color.blacksmooth2);
                        }
                    });


                    ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                        private final Rect r = new Rect();
                        private final int screenHeight = holder.itemView.getContext().getResources().getDisplayMetrics().heightPixels;
                        private int lastVisibleDecorViewHeight = 0;
                        private final int BOTTOM_OFFSET_DP = -2; // Jarak tambahan 20dp
                        private final float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
                        private final int bottomOffset = (int) (BOTTOM_OFFSET_DP * density); // Konversi ke piksel

                        @Override
                        public void onGlobalLayout() {
                            // Get visible screen height
                            window.getDecorView().getWindowVisibleDisplayFrame(r);
                            int visibleDecorViewHeight = r.height();

                            if (lastVisibleDecorViewHeight != 0) {
                                if (visibleDecorViewHeight > lastVisibleDecorViewHeight) {
                                    // Keyboard is hidden
                                    commentBox.setTranslationY(0);
                                } else if (visibleDecorViewHeight < lastVisibleDecorViewHeight) {
                                    // Keyboard is shown
                                    int keyboardHeight = screenHeight - visibleDecorViewHeight;
                                    commentBox.setTranslationY(-(keyboardHeight + bottomOffset)); // Tambahkan offset 20dp
                                }
                            }

                            lastVisibleDecorViewHeight = visibleDecorViewHeight;
                        }
                    };
                    window.getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);


                    bottomSheetDialog.setOnDismissListener(dialog -> {
                        window.getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
                    });

// Set up comment box initial state
                    commentBox.post(() -> {
                        commentBox.setTranslationY(-10);
                    });

                    commentEditText.setOnFocusChangeListener((v1, hasFocus) -> {
                        if (hasFocus && window != null) {
                            window.clearFlags(
                                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                            );
                        }
                    });


                    containerTag.setVisibility(View.GONE);

                    String albumId = item.getAlbumId();
                    ArrayList<Comment> commentList = new ArrayList<>();
                    CommentAdapter commentAdapter = new CommentAdapter(commentList, albumId, currentUserId, item.getUserId());

                    commentRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
                    commentRecyclerView.setAdapter(commentAdapter);

                    commentBox.setTranslationY(200);
                    commentBox.setAlpha(0f);
                    commentBox.animate()
                            .translationY(0)
                            .alpha(1f)
                            .setDuration(300)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();

                    // Variable to track which comment is being replied to
                    final Comment[] currentReplyingTo = {null};
                    final String[] currentParentCommentId = {null}; // Tambahkan ini
                    final int[] protectedLength = {0};

                    TextWatcher replyTextWatcher = new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                            // No action needed
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            // No action needed
                        }



                        @Override
                        public void afterTextChanged(Editable s) {
                            if (currentReplyingTo[0] != null && s.length() < protectedLength[0]) {
                                // Restore the protected text if it was deleted
                                String replyPrefix = "@" + currentReplyingTo[0].getUsername() + " ";
                                SpannableString spannableString = new SpannableString(replyPrefix);
                                spannableString.setSpan(
                                        new ForegroundColorSpan(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue)),
                                        replyPrefix.indexOf("@"),
                                        replyPrefix.indexOf("@") + currentReplyingTo[0].getUsername().length() + 1,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                );
                                commentEditText.removeTextChangedListener(this);
                                commentEditText.setText(spannableString);
                                commentEditText.setSelection(spannableString.length());
                                commentEditText.addTextChangedListener(this);
                            }
                        }
                    };

//                bottomSheetDialog.setOnShowListener(dialog -> {
//                    BottomSheetDialog d = (BottomSheetDialog) dialog;
//                    FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
//                    if (bottomSheet != null) {
//                        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
//                        behavior.setSkipCollapsed(true);
//                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//                    }
//                });

                    // Di dalam setup BottomSheetDialog

                    commentAdapter.setOnReplyClickListener(comment -> {
                        currentReplyingTo[0] = comment;
                        // Set parent comment ID berdasarkan tipe reply
                        currentParentCommentId[0] = comment.getParentCommentId() != null ?
                                comment.getParentCommentId() : comment.getId();

                        containerTag.setVisibility(View.VISIBLE);
                        usernameTag.setText("@" + comment.getUsername());

                        // Remove existing TextWatcher if any
                        if (commentEditText.getTag() instanceof TextWatcher) {
                            commentEditText.removeTextChangedListener((TextWatcher) commentEditText.getTag());
                        }

                        String replyPrefix = "@" + comment.getUsername() + " ";
                        SpannableString spannableString = new SpannableString(replyPrefix);
                        spannableString.setSpan(
                                new ForegroundColorSpan(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue)),
                                replyPrefix.indexOf("@"),
                                replyPrefix.indexOf("@") + comment.getUsername().length() + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );

                        protectedLength[0] = replyPrefix.length();
                        commentEditText.setTag(replyTextWatcher);
                        commentEditText.addTextChangedListener(replyTextWatcher);
                        commentEditText.setText(spannableString);
                        commentEditText.setSelection(spannableString.length());
                        commentEditText.requestFocus();

                        InputMethodManager imm = (InputMethodManager) holder.itemView.getContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(commentEditText, InputMethodManager.SHOW_IMPLICIT);
                    });

                    cancelTag.setOnClickListener(v1 -> {
                        containerTag.setVisibility(View.GONE);
                        currentReplyingTo[0] = null;
                        // Remove TextWatcher when canceling reply
                        if (commentEditText.getTag() instanceof TextWatcher) {
                            commentEditText.removeTextChangedListener((TextWatcher) commentEditText.getTag());
                        }
                        commentEditText.setText("");
                        protectedLength[0] = 0;
                    });

                    DatabaseReference commentsRef = FirebaseDatabase.getInstance().getReference("users")
                            .child(item.getUserId())
                            .child("album")
                            .child(albumId)
                            .child("comments");

                    // Load existing comments
                    // Di dalam bagian ValueEventListener untuk comments
                    commentsRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            commentList.clear();
                            for (DataSnapshot commentSnapshot : snapshot.getChildren()) {
                                String commentId = commentSnapshot.getKey();
                                String userId = commentSnapshot.child("userId").getValue(String.class);
                                String username = commentSnapshot.child("username").getValue(String.class);
                                String photoProfile = commentSnapshot.child("photoprofile").getValue(String.class);
                                String text = commentSnapshot.child("text").getValue(String.class);
                                Long timestamp = commentSnapshot.child("timestamp").getValue(Long.class);

                                if (userId != null && username != null && text != null) {
                                    Comment comment = new Comment(userId, username, text,
                                            photoProfile != null ? photoProfile : "",
                                            timestamp != null ? timestamp : 0L);
                                    comment.setId(commentId);

                                    // Load replies
                                    ArrayList<Comment> replies = new ArrayList<>();
                                    DataSnapshot repliesSnapshot = commentSnapshot.child("replies");
                                    if (repliesSnapshot.exists()) {  // Pastikan node replies ada
                                        for (DataSnapshot replySnapshot : repliesSnapshot.getChildren()) {
                                            String replyId = replySnapshot.getKey();
                                            String replyUserId = replySnapshot.child("userId").getValue(String.class);
                                            String replyUsername = replySnapshot.child("username").getValue(String.class);
                                            String replyText = replySnapshot.child("text").getValue(String.class);
                                            String replyPhotoProfile = replySnapshot.child("photoprofile").getValue(String.class);
                                            Long replyTimestamp = replySnapshot.child("timestamp").getValue(Long.class);

                                            if (replyUserId != null && replyUsername != null && replyText != null) {
                                                Comment reply = new Comment(replyUserId, replyUsername, replyText,
                                                        replyPhotoProfile != null ? replyPhotoProfile : "",
                                                        replyTimestamp != null ? replyTimestamp : 0L);
                                                reply.setId(replyId);  // Simpan ID reply
                                                replies.add(reply);
                                            }
                                        }

                                        // Sort replies by timestamp (newest first)
                                        replies.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                                    }
                                    comment.setReplies(replies);
                                    commentList.add(comment);
                                }
                            }

                            if (commentList.isEmpty()) {
                                noneTextView.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.VISIBLE);
                            } else {
                                noneTextView.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            }
                            // Sort main comments by timestamp (newest first)
                            commentList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                            commentAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(holder.itemView.getContext(), "Failed to load comments", Toast.LENGTH_SHORT).show();
                        }
                    });

                    bottomSheetDialog.show();

                    // Send comment handler
                    // Inside the sendButton click listener, modify the reply handling section:

                    sendButton.setOnClickListener(v1 -> {
                        String commentText = commentEditText.getText().toString().trim();
                        if (!commentText.isEmpty()) {
                            long timestamp = System.currentTimeMillis();

                            DatabaseReference userRef2 = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
                            userRef2.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String username = snapshot.child("username").getValue(String.class);
                                    String photoProfileUrl = snapshot.child("photoprofile").getValue(String.class);

                                    if (username != null) {
                                        if (currentReplyingTo[0] != null) {
                                            // Handle reply to comment or reply
                                            DatabaseReference commentRef = FirebaseDatabase.getInstance().getReference("users")
                                                    .child(item.getUserId())
                                                    .child("album")
                                                    .child(albumId)
                                                    .child("comments")
                                                    .child(currentParentCommentId[0]); // Gunakan currentParentCommentId

                                            // Create reply data
                                            Map<String, Object> replyData = new HashMap<>();
                                            replyData.put("userId", currentUserId);
                                            replyData.put("username", username);
                                            replyData.put("text", commentText);
                                            replyData.put("photoprofile", photoProfileUrl != null ? photoProfileUrl : "");
                                            replyData.put("timestamp", timestamp);

                                            // Generate new reply ID
                                            String replyId = commentRef.child("replies").push().getKey();

                                            if (replyId != null) {
                                                // Add reply to the specific comment
                                                // Add reply to the specific comment
                                                // Add reply to the specific comment
                                                commentRef.child("replies").child(replyId)
                                                        .setValue(replyData)
                                                        .addOnSuccessListener(aVoid -> {
                                                            // Reset current replying state
                                                            currentReplyingTo[0] = null;
                                                            currentParentCommentId[0] = null; // Reset ID parent

                                                            // Hapus mention di EditText dengan benar
                                                            commentEditText.setText(""); // Kosongkan text
                                                            commentEditText.getText().clear(); // Pastikan benar-benar bersih

                                                            // Sembunyikan container mention jika ada
                                                            containerTag.setVisibility(View.GONE);

                                                            // Hapus TextWatcher jika ada
                                                            if (commentEditText.getTag() instanceof TextWatcher) {
                                                                commentEditText.removeTextChangedListener((TextWatcher) commentEditText.getTag());
                                                                commentEditText.setTag(null);
                                                            }

                                                            // Reset panjang teks yang dilindungi (jika ada mekanisme untuk itu)
                                                            protectedLength[0] = 0;

                                                            // Pastikan keyboard tertutup agar tidak ada kesalahan input
                                                            InputMethodManager imm = (InputMethodManager) holder.itemView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                            if (imm != null) {
                                                                imm.hideSoftInputFromWindow(commentEditText.getWindowToken(), 0);
                                                            }

                                                            Toast.makeText(holder.itemView.getContext(), "Reply added successfully", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(holder.itemView.getContext(), "Failed to add reply: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        });

                                            }
                                        } else {
                                            // Handle new comment (kode tetap sama seperti sebelumnya)
                                            Comment newComment = new Comment(currentUserId, username, commentText,
                                                    photoProfileUrl != null ? photoProfileUrl : "", timestamp);
                                            newComment.setParentUserId(item.getUserId()); // Set ID pemilik album

                                            String commentId = commentsRef.push().getKey();
                                            if (commentId != null) {
                                                commentsRef.child(commentId).setValue(newComment)
                                                        .addOnSuccessListener(aVoid -> {
                                                            commentEditText.setText("");
                                                            Toast.makeText(holder.itemView.getContext(),
                                                                    "Comment added", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(holder.itemView.getContext(),
                                                                    "Failed to add comment", Toast.LENGTH_SHORT).show();
                                                        });
                                            }
                                        }
                                    } else {
                                        Toast.makeText(holder.itemView.getContext(),
                                                "Failed to retrieve user data", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(holder.itemView.getContext(),
                                            "Failed to load user profile", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(holder.itemView.getContext(),
                                    "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                        }
                    });

                    bottomSheetDialog.show();
                });

                DatabaseReference userBookmarksRef = FirebaseDatabase.getInstance().getReference("users")
                        .child(currentUserId)
                        .child("bookmark");

                // Check bookmark status across all collections
                userBookmarksRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot allCollagesSnapshot) {
                        boolean isBookmarkedAnywhere = false;

                        // Check if the album is bookmarked in any collection
                        for (DataSnapshot collageSnapshot : allCollagesSnapshot.getChildren()) {
                            if (collageSnapshot.hasChild(item.getAlbumId())) {
                                isBookmarkedAnywhere = true;
                                break;
                            }
                        }

                        // Update bookmark icon based on whether it's bookmarked anywhere
                        albumHolder.bookmark.setImageResource(isBookmarkedAnywhere ?
                                R.drawable.bookmark_24filled : R.drawable.bookmark_24filledgrey);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(holder.itemView.getContext(),
                                "Failed to load bookmark status", Toast.LENGTH_SHORT).show();
                    }
                });

                albumHolder.bookmark.setOnClickListener(v -> {
                    userBookmarksRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot allCollagesSnapshot) {
                            boolean isBookmarkedAnywhere = false;
                            String existingCollageId = null;

                            // Check if album is bookmarked in any collection
                            for (DataSnapshot collageSnapshot : allCollagesSnapshot.getChildren()) {
                                if (collageSnapshot.hasChild(item.getAlbumId())) {
                                    isBookmarkedAnywhere = true;
                                    existingCollageId = collageSnapshot.getKey();
                                    break;
                                }
                            }

                            if (isBookmarkedAnywhere && existingCollageId != null) {
                                // Remove from existing collection
                                userBookmarksRef.child(existingCollageId).child(item.getAlbumId()).removeValue()
                                        .addOnSuccessListener(aVoid -> {
                                            albumHolder.bookmark.setImageResource(R.drawable.bookmark_24filledgrey);
                                            Toast.makeText(holder.itemView.getContext(),
                                                    "Removed from collection", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(holder.itemView.getContext(),
                                                    "Failed to remove from collection", Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                // Show bottom sheet to select collection
                                showCollageSelectionBottomSheet(albumHolder, new Album(
                                        item.getUserId(),
                                        item.getAlbumId(),
                                        item.getTitle(),
                                        item.getUsername(),
                                        item.getDescription(),
                                        item.getImageUrl(),
                                        item.getPhotoProfile(),
                                        0, // likes count
                                        false, // userHasLiked
                                        System.currentTimeMillis()
                                ));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(holder.itemView.getContext(),
                                    "Failed to check bookmark status", Toast.LENGTH_SHORT).show();
                        }
                    });
                });

                // Set click listener for the whole ite
            } else if (holder instanceof HistorySearchViewHolder) {
                TextView textHistory = holder.itemView.findViewById(R.id.textHistory);

                if (textHistory != null) {
                    textHistory.setText(item.getTitle());

                    textHistory.setOnClickListener(v -> {
                        searchEditText.setText(item.getTitle());
                        performSearch(item.getTitle());
                    });
                }

            }
        }

        private String formatUploadDate(long timestamp) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            } catch (Exception e) {
                return "";
            }
        }

        // Fungsi untuk menampilkan saran pencarian dari judul album



        private void showCollageSelectionBottomSheet(AlbumSearchViewHolder holder , Album album) {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(holder.itemView.getContext(),
                    R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(holder.itemView.getContext())
                    .inflate(R.layout.bottom_sheet_collage_list, null);
            String currentUserId = sharedPreferences.getString("userId", null);
            RecyclerView collageRecyclerView = bottomSheetView.findViewById(R.id.collageRecyclerView);
            ArrayList<CollageItem> collageList = new ArrayList<>();

            CollageSelectionAdapter collageAdapter = new CollageSelectionAdapter(collageList,
                    (collageId, collageName) -> {
                        DatabaseReference selectedCollageRef = FirebaseDatabase.getInstance().getReference("users")
                                .child(currentUserId)
                                .child("bookmark")
                                .child(collageId)
                                .child(album.getId());

                        BookmarkData bookmarkData = new BookmarkData(
                                album.getId(),
                                album.getUserId(),
                                album.getId(),
                                album.getTitle(),
                                album.getDescription(),
                                album.getUrl(),
                                album.getUsername(),
                                album.getPhotoprofile(),
                                album.getLikes(),
                                album.isUserHasLiked(),
                                System.currentTimeMillis()
                        );

                        selectedCollageRef.setValue(bookmarkData)
                                .addOnSuccessListener(aVoid -> {
                                    holder.bookmark.setImageResource(R.drawable.bookmark_24filled);
                                    Toast.makeText(holder.itemView.getContext(),
                                            "Saved to " + collageName, Toast.LENGTH_SHORT).show();
                                    bottomSheetDialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(holder.itemView.getContext(),
                                            "Failed to save bookmark", Toast.LENGTH_SHORT).show();
                                });
                    });

            collageRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
            collageRecyclerView.setAdapter(collageAdapter);


            // Load collages
            loadCollageList(holder, collageList, collageAdapter);

            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show();
        }

        private void loadCollageList(AlbumSearchViewHolder holder, ArrayList<CollageItem> collageList,
                                     CollageSelectionAdapter collageAdapter) {
            String currentUserId = sharedPreferences.getString("userId", null);
            DatabaseReference collagesRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUserId)
                    .child("bookmark");



            collagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    collageList.clear();

                    if (!snapshot.hasChild("allCollage")) {
                        DatabaseReference allCollageRef = collagesRef.child("allCollage");
                        allCollageRef.child("name").setValue("All Collage");
                        allCollageRef.child("timestamp").setValue(ServerValue.TIMESTAMP);
                    }

                    for (DataSnapshot collageSnapshot : snapshot.getChildren()) {
                        String collageName = collageSnapshot.child("name").getValue(String.class);
                        String collageId = collageSnapshot.getKey();
                        long timestamp = collageSnapshot.child("timestamp").getValue(Long.class) != null ?
                                collageSnapshot.child("timestamp").getValue(Long.class) : 0;

                        if (collageName != null && collageId != null) {
                            CollageItem collage = new CollageItem(collageId, collageName, timestamp);
                            collageList.add(collage);
                        }
                    }

                    collageList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    collageAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(holder.itemView.getContext(),
                            "Failed to load collages", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void checkFollowStatus(String currentUserId, String targetUserId, AlbumSearchViewHolder holder) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
            userRef.child(currentUserId).child("followedUser").child(targetUserId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                holder.followButton.setText("Following");
                                holder.followButton.setSelected(true);
                            } else {
                                holder.followButton.setText("Follow");
                                holder.followButton.setSelected(false);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(context, "Failed to load follow status", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        private void handleFollow(String currentUserId, String targetUserId, AlbumSearchViewHolder holder) {
            // Optimistic UI update
            boolean currentFollowState = followStatusCache.getOrDefault(targetUserId, false);
            boolean newFollowState = !currentFollowState;

            // Update UI immediately
            holder.followButton.setText(newFollowState ? "Following" : "Follow");
            holder.followButton.setSelected(newFollowState);

            // Update cache
            followStatusCache.put(targetUserId, newFollowState);

            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
            Map<String, Object> updates = new HashMap<>();

            if (newFollowState) {
                updates.put(String.format("/users/%s/followers", targetUserId), ServerValue.increment(1));
                updates.put(String.format("/users/%s/followed", currentUserId), ServerValue.increment(1));
                updates.put(String.format("/users/%s/followersUser/%s", targetUserId, currentUserId), true);
                updates.put(String.format("/users/%s/followedUser/%s", currentUserId, targetUserId), true);
            } else {
                updates.put(String.format("/users/%s/followers", targetUserId), ServerValue.increment(-1));
                updates.put(String.format("/users/%s/followed", currentUserId), ServerValue.increment(-1));
                updates.put(String.format("/users/%s/followersUser/%s", targetUserId, currentUserId), null);
                updates.put(String.format("/users/%s/followedUser/%s", currentUserId, targetUserId), null);
            }

            FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                    .addOnFailureListener(e -> {
                        // Revert UI if update failed
                        followStatusCache.put(targetUserId, currentFollowState);
                        holder.followButton.setText(currentFollowState ? "Following" : "Follow");
                        holder.followButton.setSelected(currentFollowState);
                    });
        }




        private void handleLike(AlbumSearchViewHolder holder, SearchItem item) {
            String albumId = item.getAlbumId();
            String userId = item.getUserId();

            // Optimistic UI update
            boolean currentLikeState = likeStatusCache.getOrDefault(albumId, false);
            boolean newLikeState = !currentLikeState;

            // Update UI immediately
            holder.btnLike.setImageResource(newLikeState ? R.drawable.heart_24filled : R.drawable.heart_24);
            int currentLikes = (int) parseFormattedCount(holder.likeCount.getText().toString());
            holder.likeCount.setText(formatLikesCount(newLikeState ? currentLikes + 1 : currentLikes - 1));

            // Play animation
            Animation likeAnimation = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.like_button_animation);
            holder.btnLike.startAnimation(likeAnimation);

            // Update cache
            likeStatusCache.put(albumId, newLikeState);

            // Prepare references
            DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(userId)
                    .child("album").child(albumId);
            DatabaseReference likesRef = albumRef.child("userLikes").child(currentUserId);

            // Use transaction for atomic operation
            albumRef.child("likes").runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    Long likes = mutableData.getValue(Long.class);
                    if (likes == null) likes = 0L;
                    mutableData.setValue(newLikeState ? likes + 1 : likes - 1);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                    if (!committed) {
                        // Revert UI if transaction failed
                        likeStatusCache.put(albumId, currentLikeState);
                        holder.btnLike.setImageResource(currentLikeState ? R.drawable.heart_24filled : R.drawable.heart_24);
                        holder.likeCount.setText(formatLikesCount(currentLikes));
                    }
                }
            });

            // Update like status
            if (newLikeState) {
                likesRef.setValue(true);
            } else {
                likesRef.removeValue();
            }
        }

        // Perbaikan untuk handle bookmark
        private void handleBookmark(AlbumSearchViewHolder holder, Album album, String collageId) {
            String albumId = album.getId();

            // Optimistic UI update
            boolean currentBookmarkState = bookmarkStatusCache.getOrDefault(albumId, false);
            boolean newBookmarkState = !currentBookmarkState;

            // Update UI immediately
            holder.bookmark.setImageResource(newBookmarkState ?
                    R.drawable.bookmark_24filled : R.drawable.bookmark_24filledgrey);

            // Update cache
            bookmarkStatusCache.put(albumId, newBookmarkState);

            DatabaseReference bookmarkRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUserId)
                    .child("bookmark")
                    .child(collageId)
                    .child(albumId);

            if (newBookmarkState) {
                BookmarkData bookmarkData = new BookmarkData(
                        album.getId(),
                        album.getUserId(),
                        album.getId(),
                        album.getTitle(),
                        album.getDescription(),
                        album.getUrl(),
                        album.getUsername(),
                        album.getPhotoprofile(),
                        album.getLikes(),
                        album.isUserHasLiked(),
                        System.currentTimeMillis()
                );

                bookmarkRef.setValue(bookmarkData)
                        .addOnFailureListener(e -> {
                            // Revert UI if update failed
                            bookmarkStatusCache.put(albumId, currentBookmarkState);
                            holder.bookmark.setImageResource(currentBookmarkState ?
                                    R.drawable.bookmark_24filled : R.drawable.bookmark_24filledgrey);
                        });
            } else {
                bookmarkRef.removeValue()
                        .addOnFailureListener(e -> {
                            // Revert UI if update failed
                            bookmarkStatusCache.put(albumId, currentBookmarkState);
                            holder.bookmark.setImageResource(currentBookmarkState ?
                                    R.drawable.bookmark_24filled : R.drawable.bookmark_24filledgrey);
                        });
            }
        }


        private void navigateToUserProfile(String userId) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Bundle bundle = new Bundle();
                        bundle.putString("userId", userId);
                        bundle.putString("username", snapshot.child("username").getValue(String.class));
                        bundle.putString("photoprofile", snapshot.child("photoprofile").getValue(String.class));
                        bundle.putString("bio", snapshot.child("bio").getValue(String.class));
                        bundle.putString("location", snapshot.child("location").getValue(String.class));
                        bundle.putString("tagname", snapshot.child("tagname").getValue(String.class));
                        bundle.putLong("followers", snapshot.child("followers").getValue(Long.class) != null ?
                                snapshot.child("followers").getValue(Long.class) : 0);
                        bundle.putLong("following", snapshot.child("followed").getValue(Long.class) != null ?
                                snapshot.child("followed").getValue(Long.class) : 0);
                        bundle.putLong("post", snapshot.child("post").getValue(Long.class) != null ?
                                snapshot.child("post").getValue(Long.class) : 0);

                        ProfileShowFragment profileShowFragment = new ProfileShowFragment();
                        profileShowFragment.setArguments(bundle);

                        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();

                        // Get the current active fragment's tag
                        String currentFragmentTag = null;
                        Fragment currentFragment = fragmentManager.findFragmentById(R.id.frame_layout);
                        if (currentFragment != null && currentFragment.isVisible()) {
                            currentFragmentTag = currentFragment.getTag();
                        }

                        // Create a unique backstack name that includes the parent fragment tag
                        String backStackName = "ProfileShowFragment_" + currentFragmentTag;

                        FragmentTransaction transaction = fragmentManager.beginTransaction();

                        // Keep the current fragment visible, but add the profile fragment on top
                        transaction.add(R.id.frame_layout, profileShowFragment, "ProfileShowFragment");

                        // Add to back stack with the unique name
                        transaction.addToBackStack(backStackName);
                        transaction.commit();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Failed to load user profile", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private String formatLikesCount(long count) {
            if (count >= 1_000_000_000) {
                return String.format("%.1fb", count / 1_000_000_000.0);
            } else if (count >= 1_000_000) {
                return String.format("%.1fm", count / 1_000_000.0);
            } else if (count >= 1_000) {
                return String.format("%.1fk", count / 1_000.0);
            }
            return String.valueOf(count);
        }

        // Metode untuk parsing yang lebih aman
        private long parseFormattedCount(String formattedCount) {
            try {
                // Periksa apakah sudah berupa angka murni
                if (formattedCount.matches("\\d+")) {
                    return Long.parseLong(formattedCount);
                }

                // Hapus karakter non-numerik kecuali titik
                String numericPart = formattedCount.replaceAll("[^0-9.]", "");
                double numValue = Double.parseDouble(numericPart);

                if (formattedCount.contains("b")) {
                    return (long)(numValue * 1_000_000_000);
                } else if (formattedCount.contains("m")) {
                    return (long)(numValue * 1_000_000);
                } else if (formattedCount.contains("k")) {
                    return (long)(numValue * 1_000);
                }

                return Long.parseLong(numericPart);
            } catch (NumberFormatException e) {
                Log.e("LikeCount", "Failed to parse count: " + formattedCount);
                return 0;
            }
        }



        // ViewHolders for different item types
        public class UserSearchViewHolder extends RecyclerView.ViewHolder {
            ImageView profileImageView;
            TextView usernameTextView;
            TextView tagNameTextView;

            public UserSearchViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImageView = itemView.findViewById(R.id.searchPersonImage);
                usernameTextView = itemView.findViewById(R.id.searchPersonUsername);
                tagNameTextView = itemView.findViewById(R.id.tagName);
            }
        }



        public class AlbumSearchViewHolder extends RecyclerView.ViewHolder {
            ImageView albumImageView;
            TextView titleTextView;
            TextView descriptionTextView;
            TextView usernameTextView;
            ImageView profileImageView;
            TextView uploadDateTextView;
            TextView likeCount;
            ImageButton btnLike;
            ImageButton btnComment;
            ImageButton bookmark;
            TextView followButton;

            public AlbumSearchViewHolder(@NonNull View itemView) {
                super(itemView);
                albumImageView = itemView.findViewById(R.id.imageView);
                titleTextView = itemView.findViewById(R.id.textTitle);
                descriptionTextView = itemView.findViewById(R.id.textDescription);
                usernameTextView = itemView.findViewById(R.id.usernameUploader);
                profileImageView = itemView.findViewById(R.id.photoProfile);
                uploadDateTextView = itemView.findViewById(R.id.uploadDateTextView);
                likeCount = itemView.findViewById(R.id.likeCount);
                btnLike = itemView.findViewById(R.id.btnLike);
                btnComment = itemView.findViewById(R.id.btnComment);
                bookmark = itemView.findViewById(R.id.bookmark);
                followButton = itemView.findViewById(R.id.follow);
                long likes = Long.parseLong(likeCount.getText().toString());
                likeCount.setText(formatLikesCount(likes));

                albumImageView.setOnClickListener(v -> {
                    // Get the adapter position
                    int position = getAdapterPosition();
                    // Check if position is valid
                    if (position != RecyclerView.NO_POSITION) {
                        SearchItem item = searchItems.get(position);
                        Intent intent = new Intent(itemView.getContext(), FullscreenImageActivity.class);
                        intent.putExtra("imageUrl", item.getImageUrl());
                        intent.putExtra("userId", item.getUserId());
                        intent.putExtra("username", item.getUsername());
                        intent.putExtra("imageTitle", item.getTitle());
                        intent.putExtra("imageDescription", item.getDescription());

                        itemView.getContext().startActivity(intent);
                        ((Activity) itemView.getContext()).overridePendingTransition(0, 0);
                    }
                });
            }



        }

        public class HistorySearchViewHolder extends RecyclerView.ViewHolder {
            public HistorySearchViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }

    // Inner class for search items


    @Override
    public void onPause() {
        super.onPause();
        // Save scroll state
        if (recyclerView != null) {
            recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Restore scroll state
        if (recyclerView != null && recyclerViewState != null) {
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save scroll state to bundle
        if (recyclerView != null) {
            outState.putParcelable(KEY_RECYCLER_STATE,
                    recyclerView.getLayoutManager().onSaveInstanceState());
        }
    }


}
