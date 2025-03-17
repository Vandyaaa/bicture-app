package com.example.imaginate;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;

public class CommentHandler {
    private CommentHandler() {
        // Private constructor to prevent instantiation
    }

    public static String formatReplyComment(String replyToUsername, String commentText) {
        if (replyToUsername != null && !replyToUsername.isEmpty()) {
            return "@" + replyToUsername + " " + commentText;
        }
        return commentText;
    }

    public static SpannableString styleCommentWithUsername(Context context, String replyToUsername, String commentText) {
        if (replyToUsername == null || replyToUsername.isEmpty()) {
            return new SpannableString(commentText);
        }

        String fullText = "@" + replyToUsername + " " + commentText;
        SpannableString spannableString = new SpannableString(fullText);

        // Style the username part in blue
        spannableString.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(context, R.color.blue)),
                0,
                replyToUsername.length() + 1, // +1 for the @ symbol
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return spannableString;
    }
}
