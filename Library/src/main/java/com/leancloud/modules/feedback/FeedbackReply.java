package com.leancloud.modules.feedback;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVUtils;

import org.json.JSONObject;


@JSONType(asm = false)
public class FeedbackReply {

  // 回复类型：user 表示用户，dev 表示开发者
  public enum ReplyType {
    DEV("dev"), USER("user");
    String type;

    private ReplyType(String type) {
      this.type = type;
    }

    @Override
    public String toString() {
      return this.type;
    }
  }

  private Date createdAt;   // 创建日期
  private String objectId;  // object Id
  private String content;   // 回复内容
  private ReplyType replyType;     // 回复类型
  private boolean synced = false;  // 是否已经保存到 LeanCloud 云端的标志
  private AVFile attachment;       // 附件，可以是图片、音频等文件

  public FeedbackReply() {
    this(null, ReplyType.USER);
  }

  public FeedbackReply(String content, ReplyType type) {
    this.content = content;
    this.replyType = type;
    createdAt = new Date();
  }

  public FeedbackReply(String content) {
    this(content, ReplyType.USER);
  }

  public FeedbackReply(File attachment) throws AVException {
    this(null, ReplyType.USER);
    this.setAttachment(attachment);
  }
  public static FeedbackReply getInstanceFromJSONObject(JSONObject o) {
    FeedbackReply c = new FeedbackReply();
    try {
      c.setObjectId(o.getString("objectId"));
      if (o.has("content")) {
        String content = o.getString("content");
        if (!"null".equalsIgnoreCase(content)) {
          c.setContent(content);
        }
      }
      c.setCreatedAt(AVUtils.dateFromString(o.getString("createdAt")));
      if (AVUtils.isBlankString(o.getString("type"))) {
        c.setType("user");
      } else {
        c.setType(o.getString("type"));
      }
      if (o.has("attachment") && !AVUtils.isBlankString(o.getString("attachment"))) {
        c.setAttachment(new AVFile(AVUtils.md5(o.getString("attachment")), o
                .getString("attachment"), null));
      }
      c.setSynced(true);
    } catch (Exception e) {
      return null;
    }
    return c;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  protected String getObjectId() {
    return objectId;
  }

  protected void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public ReplyType getReplyType() {
    return replyType;
  }

  public void setType(String type) {
    if (ReplyType.DEV.toString().equalsIgnoreCase(type)) {
      this.replyType = ReplyType.DEV;
    } else {
      this.replyType = ReplyType.USER;
    }
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setReplyType(ReplyType type) {
    this.replyType = type;
  }

  protected boolean isSynced() {
    return synced;
  }

  protected void setSynced(boolean synced) {
    this.synced = synced;
  }

  public AVFile getAttachment() {
    return attachment;
  }

  /**
   * 上传附件作为一个反馈信息
   *
   * @param attachment 本地文件
   * @throws AVException
   */
  @JSONField(serialize = false)
  public void setAttachment(File attachment) throws AVException {
    try {
      this.attachment = AVFile.withFile(attachment.getName(), attachment);
    } catch (Exception e) {
      throw new AVException(e);
    }
  }

  public String getRestParameters() {
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("content", this.getContent());
    parameters.put("type", this.getReplyType().toString());
    if (this.getAttachment() != null) {
      parameters.put("attachment", this.getAttachment().getUrl());
    }
    return AVUtils.restfulServerData(parameters);
  }

  protected void setAttachment(AVFile attachment) throws AVException {
    this.attachment = attachment;
  }
}
