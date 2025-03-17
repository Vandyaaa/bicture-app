package com.example.imaginate;

public class Upload {
    private String userId;
    private String username;
    private String title;
    private String description;
    private String imageUrl;
    private int likes;  // Tambahkan atribut likes

    // Constructor yang diperbarui
    public Upload(String userId, String username, String title, String description, String imageUrl, int likes) {
        this.userId = userId;
        this.username = username;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.likes = likes;  // Inisialisasi likes
    }

    // Getter untuk semua atribut
    public String userId() {
        return userId;
    }

    public String getUsername() { return username; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public int getLikes() { return likes; }  // Tambahkan getter untuk likes

    // Setter untuk likes (opsional, jika diperlukan untuk memperbarui jumlah like di memori)
    public void setLikes(int likes) { this.likes = likes; }
}
