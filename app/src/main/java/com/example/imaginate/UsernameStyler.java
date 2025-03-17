package com.example.imaginate;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;

public class UsernameStyler {
    public static SpannableString styleUsername(Context context, String username) {
        SpannableString spannableString = new SpannableString(username);
        spannableString.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(context, R.color.blue)),
                0,
                username.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return spannableString;
    }

    public static SpannableString styleReplyText(Context context, String username, String additionalText) {
        // Make sure to include the entire username including spaces
        String fullText = "@" + username + " " + additionalText;
        SpannableString spannableString = new SpannableString(fullText);

        // Style the entire "@username" part including spaces
        spannableString.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(context, R.color.blue)),
                0,
                username.length() + 1, // +1 for the @ symbol
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return spannableString;
    }
}
