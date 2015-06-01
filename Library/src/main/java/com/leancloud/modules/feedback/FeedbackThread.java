package com.leancloud.modules.feedback;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVUtils;
import com.avos.avoscloud.GenericObjectCallback;
import com.avos.avoscloud.LogUtil;
import com.avos.avoscloud.PaasClient;
import com.avos.avoscloud.AVPersistenceUtils;

public class FeedbackThread {
  private static final String FEEDBACK_PATH = "feedback";
  private static final String FEEDBACK_PUT_PATH = "feedback/%s";
  private static final String FEEDBACK_REPLY_PATH = "feedback/%s/threads";
  private static FeedbackThread thread;
  private static Method currentInstallationMethod;

  private List<FeedbackReply> feedbackReplyList = new LinkedList<FeedbackReply>();
  private String contact = "";
  private String status = "";
  private String remarks = "";
  private AVFile attachment = null;
  private String content = "";
  private String installationId = "";

  private FeedbackThread() {
    String content = AVPersistenceUtils.readContentFromFile(getFeedbackCacheFile());
    this.contact = AVPersistenceUtils.readContentFromFile(getContactCacheFile());
    if (!AVUtils.isBlankString(content)) {
      try {
        this.feedbackReplyList = JSON.parseArray(content, FeedbackReply.class);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  public static synchronized FeedbackThread getInstance() {
    if (thread == null) {
      thread = new FeedbackThread();
    }
    return thread;
  }

  public void addReply(FeedbackReply feedbackReply) {
    feedbackReplyList.add(feedbackReply);
  }

  public List<FeedbackReply> getFeedbackReplyList() {
    return feedbackReplyList;
  }

  public String getContact() {
    return contact;
  }

  public void setContact(String contact) {
    if (!AVUtils.isBlankString(contact) && !contact.equals(this.contact)) {
      this.contact = contact;
      if (feedbackReplyList.size() > 0) {
        feedbackReplyList.clear();
        saveContact();
      }
    }
  }

  public void setRemarks(String remarks) {
    if (!AVUtils.isBlankString(remarks)) {
      this.remarks = remarks;
    }
  }

  public synchronized void sync(final SyncCallback callback) {
    if (feedbackReplyList.size() > 0) {
      // 先保存后取
      new AsyncTask<Void, Integer, Exception>() {
        boolean flag = true;
        Exception sendException;

        @Override
        protected Exception doInBackground(Void... params) {
          for (int i = 0; i < feedbackReplyList.size() && flag; i++) {
            if (!feedbackReplyList.get(i).isSynced()) {
              final FeedbackReply currentFeedbackReply = feedbackReplyList.get(i);
              if (currentFeedbackReply.getAttachment() != null) {
                try {
                  currentFeedbackReply.getAttachment().save();
                } catch (AVException e) {
                  return e;
                }
              }
              if (i == 0 && !AVUtils.isBlankString(currentFeedbackReply.getObjectId())) {
                PaasClient.storageInstance().putObject(
                    String.format(FEEDBACK_PUT_PATH, currentFeedbackReply.getObjectId()),
                    generateRestParameters(currentFeedbackReply), true, null, new GenericObjectCallback() {
                      @Override
                      public void onSuccess(String content, AVException e) {
                        if (e != null) {
                          sendException = e;
                          flag = false;
                          return;
                        } else {
                          JSONObject resp;
                          try {
                            resp = new JSONObject(content);
                            if (currentFeedbackReply.getObjectId().equals(resp.getString("objectId"))) {
                              currentFeedbackReply.setSynced(true);
                            }
                          } catch (JSONException e1) {
                            sendException = e;
                          }
                        }
                      }

                      @Override
                      public void onFailure(Throwable error, String content) {
                        LogUtil.log.d(content);
                        sendException = new Exception(error);
                        flag = false;
                      }
                    }, currentFeedbackReply.getObjectId(), null);
              } else {
                PaasClient.storageInstance().postObject(
                    i == 0 ? FEEDBACK_PATH : String.format(FEEDBACK_REPLY_PATH, feedbackReplyList.get(0)
                        .getObjectId()), i == 0? generateRestParameters(currentFeedbackReply):currentFeedbackReply.getRestParameters(), true,
                    new GenericObjectCallback() {
                      @Override
                      public void onSuccess(String content, AVException e) {
                        if (e != null) {
                          sendException = e;
                          flag = false;
                          return;
                        } else {
                          JSONObject resp;
                          try {
                            resp = new JSONObject(content);
                            currentFeedbackReply.setObjectId(resp.getString("objectId"));
                            currentFeedbackReply.setSynced(true);
                            currentFeedbackReply.setCreatedAt(AVUtils.dateFromString(resp
                                .getString("createdAt")));
                          } catch (JSONException e1) {
                            sendException = e;
                          }
                        }
                      }

                      @Override
                      public void onFailure(Throwable error, String content) {
                        LogUtil.log.d(content);
                        sendException = new Exception(error);
                        flag = false;
                      }
                    });
              }
            } // end if (!feedbackReplyList.get(i).isSynced())
          } // end for
          return sendException;
        }

        @Override
        public void onPostExecute(Exception ex) {
          saveLocal();
          if (callback != null) {
            callback.onRepliesSend(feedbackReplyList, ex == null ? null : new AVException(ex));
          }
          if (!AVUtils.isBlankString(feedbackReplyList.get(0).getObjectId())) {
            PaasClient.storageInstance().getObject(
                String.format(FEEDBACK_REPLY_PATH, feedbackReplyList.get(0).getObjectId()), null, false,
                null, new GenericObjectCallback() {
                  @Override
                  public void onSuccess(String content, AVException e) {
                    if (e != null) {
                      if (callback != null) {
                        callback.onRepliesFetch(feedbackReplyList, e);
                      }
                      return;
                    } else {
                      try {
                        JSONObject resp = new JSONObject(content);
                        String results = resp.getString("results");
                        JSONArray replyJsonArray = new JSONArray(results);
                        List<FeedbackReply> replies = new LinkedList<FeedbackReply>();
                        for (int i = 0; i < replyJsonArray.length(); i++) {
                          JSONObject o = replyJsonArray.getJSONObject(i);
                          FeedbackReply c = FeedbackReply.getInstanceFromJSONObject(o);
                          if (c != null && !AVUtils.isBlankString(c.getObjectId())) {
                            c.setSynced(true);
                            replies.add(c);
                          }
                        }
                        FeedbackReply first = feedbackReplyList.get(0);
                        feedbackReplyList.clear();
                        feedbackReplyList.add(first);
                        feedbackReplyList.addAll(replies);
                        if (callback != null) {
                          callback.onRepliesFetch(feedbackReplyList, e);
                        }
                        saveLocal();
                      } catch (Exception ex) {
                        if (callback != null) {
                          callback.onRepliesFetch(feedbackReplyList, new AVException(ex));
                        }
                      }
                    }
                  }

                  @Override
                  public void onFailure(Throwable error, String content) {
                    if (callback != null) {
                      callback.onRepliesFetch(feedbackReplyList, new AVException(content, error));
                    }
                  }
                });
          }
        }
      }.execute((Void) null);
    }
  }

  public interface SyncCallback {
    public void onRepliesSend(List<FeedbackReply> feedbackReplies, AVException e);

    public void onRepliesFetch(List<FeedbackReply> feedbackReplies, AVException e);
  }

  protected void saveLocal() {
    String content = JSON.toJSONString(this.feedbackReplyList);
    AVPersistenceUtils.saveContentToFile(content, getFeedbackCacheFile());
    saveContact();
  }

  private void saveContact() {
    if (!AVUtils.isBlankString(contact)) {
      AVPersistenceUtils.saveContentToFile(contact, getContactCacheFile());
    }
  }

  private static File getFeedbackCacheFile() {
    return new File(getFeedbackCacheDir(), "feedback");
  }

  private static File getContactCacheFile() {
    return new File(getFeedbackCacheDir(), "contact");
  }

  private static File getFeedbackCacheDir() {
    File dir = new File(AVPersistenceUtils.getCacheDir(), "FeedbackCache");
    dir.mkdirs();
    return dir;
  }

  protected String generateRestParameters(FeedbackReply feedbackReply) {

    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("content", feedbackReply.getContent());
    if (!AVUtils.isBlankContent(this.contact)) {
      parameters.put("contact", this.contact);
    }
    if (!AVUtils.isBlankString(this.remarks)) {
      parameters.put("remarks", this.remarks);
    }
    if (feedbackReply.getAttachment() != null) {
      parameters.put("attachment", feedbackReply.getAttachment().getUrl());
    }
    try {
      if (currentInstallationMethod == null) {
        Class installationClass = Class.forName("com.avos.avoscloud.AVInstallation");
        currentInstallationMethod = installationClass.getMethod("getCurrentInstallation");
      }
      AVObject o = (AVObject) currentInstallationMethod.invoke(null);
      if (!AVUtils.isBlankString(o.getObjectId())) {
        parameters.put("iid", o.getObjectId());
      }
      // ignore all exceptions
    } catch (IllegalAccessException e) {

    } catch (InvocationTargetException e) {

    } catch (ClassNotFoundException e) {

    } catch (NoSuchMethodException e) {

    }

    return AVUtils.restfulServerData(parameters);
  }
}
