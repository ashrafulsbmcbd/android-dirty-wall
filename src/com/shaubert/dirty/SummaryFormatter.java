package com.shaubert.dirty;

import com.shaubert.dirty.client.DirtyBlog;
import com.shaubert.dirty.client.DirtyPost;
import com.shaubert.dirty.db.BlogsCursor;
import com.shaubert.dirty.db.CommentsCursor;
import com.shaubert.dirty.db.PostsCursor;
import com.shaubert.util.Dates;
import com.shaubert.util.PluralHelper;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SummaryFormatter {

    private SimpleDateFormat dateFormat;
    private final SimpleDateFormat todayFormat;
    private final SimpleDateFormat yesterdayFormat;

    private final Context context;

    public SummaryFormatter(Context context) {
        this.context = context;
        
        dateFormat = new SimpleDateFormat(context.getString(R.string.post_date_format));
        todayFormat = new SimpleDateFormat(context.getString(R.string.post_today_format));
        yesterdayFormat = new SimpleDateFormat(context.getString(R.string.post_yesterday_format));
    }
    
    public String formatCreationDate(Date creationDate) {
        return formatCreationDate(creationDate, false);
    }
    
    public String formatCreationDate(Date creationDate, boolean fullDate) {
        final String resStr;
        if (creationDate.getTime() > 0) {
            if (!fullDate && Dates.isToday(creationDate, TimeZone.getDefault())) {
                resStr = todayFormat.format(creationDate);
            } else if (!fullDate && Dates.isYesterday(creationDate, TimeZone.getDefault())) {
                resStr = yesterdayFormat.format(creationDate);
            } else {
                resStr = dateFormat.format(creationDate);
            }
        } else {
            resStr = "? в ?";
        }
        return resStr;
    }
    
    public String formatComments(int count) {
        int commentsResFormat = R.string.many_comments;
        switch (PluralHelper.getForm(count)) {
            case ONE:
                commentsResFormat = R.string.one_comment;
                break;
            case FEW:
                commentsResFormat = R.string.few_comments;
                break;
        }
        return String.format(context.getResources().getString(commentsResFormat), count);
    }
    
    public String formatVotes(int count) {
        int votesResFormat = R.string.many_votes;
        switch (PluralHelper.getForm(count)) {
            case ONE:
                votesResFormat = R.string.one_vote;
                break;
            case FEW:
                votesResFormat = R.string.few_votes;
                break;
        }
        return String.format(context.getResources().getString(votesResFormat), count);
    }
    
    public CharSequence formatSummaryText(DirtyPost dirtyPost) {
        String summaryFormat = context.getString(R.string.post_summary_format);
        String author = dirtyPost.getAuthor();
        String subBlogUrl = TextUtils.isEmpty(dirtyPost.getSubBlogName()) ? "d3.ru" : dirtyPost.getSubBlogName();
        String text = String.format(summaryFormat, author,
        		subBlogUrl,
                formatCreationDate(new Date(dirtyPost.getCreationDateAsMillis())),
                formatComments(dirtyPost.getCommentsCount()),
                formatVotes(dirtyPost.getVotesCount()));
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int authorIndex = text.indexOf(author);
        builder.setSpan(new URLSpan(dirtyPost.getAuthorLink()), authorIndex, authorIndex + author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!TextUtils.isEmpty(dirtyPost.getSubBlogName())) {
        	int subBlogIndex = text.indexOf(subBlogUrl);
        	builder.setSpan(new URLSpan("http://" + dirtyPost.getSubBlogHost()), subBlogIndex, subBlogIndex + subBlogUrl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (dirtyPost.isGolden()) {
            builder.append("\u00A0\u00A0");
            builder.setSpan(new ImageSpan(context, R.drawable.stars, DynamicDrawableSpan.ALIGN_BASELINE), 
                    builder.length() - 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }

    public CharSequence formatCompactSummaryText(PostsCursor dirtyPost) {
        String summaryFormat = context.getString(R.string.post_summary_format);
        String author = dirtyPost.getAuthor();
        String subBlogUrl = TextUtils.isEmpty(dirtyPost.getSubBlogName()) ? "d3.ru" : dirtyPost.getSubBlogName();
        String text = String.format(summaryFormat, author, subBlogUrl,
                formatCreationDate(dirtyPost.getCreationDate()),
                formatComments(dirtyPost.getCommentsCount()),
                formatVotes(dirtyPost.getVotesCount()));
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int authorIndex = text.indexOf(author);
        builder.setSpan(new URLSpan(dirtyPost.getAuthorLink()), authorIndex, authorIndex + author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!TextUtils.isEmpty(dirtyPost.getSubBlogName())) {
        	int subBlogIndex = text.indexOf(subBlogUrl);
        	builder.setSpan(new URLSpan("http://" + subBlogUrl), subBlogIndex, subBlogIndex + subBlogUrl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (dirtyPost.isGolden()) {
            builder.append("\u00A0\u00A0");
            builder.setSpan(new ImageSpan(context, R.drawable.stars, DynamicDrawableSpan.ALIGN_BASELINE),
                    builder.length() - 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }
    
    public String formatSummaryTextForExport(DirtyPost dirtyPost) {
        String summaryFormat = context.getString(R.string.post_summary_format);
        String author = dirtyPost.getAuthor();
        String subBlogUrl = TextUtils.isEmpty(dirtyPost.getSubBlogName()) ? "d3.ru" : dirtyPost.getSubBlogName();
        String text = String.format(summaryFormat, author,
                subBlogUrl,
                formatCreationDate(new Date(dirtyPost.getCreationDateAsMillis())),
                formatComments(dirtyPost.getCommentsCount()),
                formatVotes(dirtyPost.getVotesCount()));
        if (dirtyPost.isGolden()) {
            text += "\u00A0\u00A0\u2605\u2605\u2605\u2605\u2605";//five black stars
        }
        return text;
    }
    
    public CharSequence formatSummaryText(CommentsCursor comment) {
        String summaryFormat = context.getString(R.string.comment_summary_format);
        String author = comment.getAuthor();
        String text = String.format(summaryFormat, author,
                formatCreationDate(comment.getCreationDate()),
                formatVotes(comment.getVotesCount()));
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int authorIndex = TextUtils.isEmpty(author) ? -1 : text.indexOf(author);
        if (authorIndex >= 0) {
            builder.setSpan(new URLSpan(comment.getAuthorLink()),
                    authorIndex, authorIndex + author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }
 
    public String formatSubscribers(int count) {
        int subscrResFormat = R.string.many_subscribers;
        switch (PluralHelper.getForm(count)) {
            case ONE:
                subscrResFormat = R.string.one_subscriber;
                break;
            case FEW:
                subscrResFormat = R.string.few_subscribers;
                break;
        }
        return String.format(context.getResources().getString(subscrResFormat), count);
    }
    
    public CharSequence formatSummaryText(BlogsCursor blog) {
    	String summaryFormat = context.getString(R.string.blog_summary_format);
        String blogUrl = blog.getUrl();
        String author = blog.getAuthor();
        String text = String.format(summaryFormat, blogUrl, author,
                formatSubscribers(blog.getReadersCount()));
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int blogUrlIndex = text.indexOf(blogUrl);
        builder.setSpan(new URLSpan("http://" + blogUrl), blogUrlIndex, blogUrlIndex + blogUrl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        int authorIndex = text.indexOf(author);
        builder.setSpan(new URLSpan(DirtyBlog.getInstance().getAuthorLink(author)), authorIndex, authorIndex + author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }
    
}
