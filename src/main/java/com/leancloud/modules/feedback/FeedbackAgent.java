package com.leancloud.modules.feedback;

import java.util.List;

import com.avos.avoscloud.AVException;
import com.leancloud.modules.feedback.FeedbackThread.SyncCallback;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class FeedbackAgent {

  FeedbackThread defaultThread;
  Context mContext;
  boolean contactSwitch = true;

  public FeedbackAgent(Context context) {
    this.mContext = context;
    defaultThread = FeedbackThread.getInstance();
  }

  public FeedbackThread getDefaultThread() {
    return defaultThread;
  }

  public void startDefaultThreadActivity() {
    Intent intent = new Intent(mContext, ThreadActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    mContext.startActivity(intent);
  }

  public boolean isContactEnabled() {
    return contactSwitch;
  }

  public void isContactEnabled(boolean flag) {
    this.contactSwitch = flag;
  }

  public void sync() {
    final int originalCount = defaultThread.commentList.size();
    defaultThread.sync(new SyncCallback() {

      @Override
      public void onCommentsSend(List<Comment> comments, AVException e) {}

      @Override
      public void onCommentsFetch(List<Comment> comments, AVException e) {
        if (comments.size() > originalCount) {
          Intent resultIntent = new Intent(mContext, ThreadActivity.class);
          PendingIntent pendingIntent =
              PendingIntent.getActivity(mContext, 0, resultIntent,
                  PendingIntent.FLAG_UPDATE_CURRENT);
          NotificationCompat.Builder mBuilder =
              new NotificationCompat.Builder(mContext)
                  .setSmallIcon(Resources.drawable.avoscloud_feedback_notification(mContext))
                  .setContentTitle(
                      mContext.getResources().getString(
                          Resources.string.avoscloud_feedback_new_item(mContext)))
                  .setContentText(comments.get(comments.size() - 1).getContent());
          mBuilder.setAutoCancel(true);
          mBuilder.setContentIntent(pendingIntent);

          int mNotificationId = 996;
          NotificationManager mNotifyMgr =
              (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
          mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
      }
    });
  }
}
