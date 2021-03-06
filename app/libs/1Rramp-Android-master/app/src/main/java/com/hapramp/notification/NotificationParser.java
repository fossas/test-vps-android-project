package com.hapramp.notification;

import com.hapramp.notification.model.BaseNotificationModel;
import com.hapramp.notification.model.CompetitionResultNotificationModel;
import com.hapramp.notification.model.CompetitionWinnerNotificationModel;
import com.hapramp.notification.model.ContestCreatedNotificationModel;
import com.hapramp.notification.model.ContestStartedNotificationModel;
import com.hapramp.notification.model.ContestWinnerDeclaredNotificationModel;
import com.hapramp.notification.model.FollowNotificationModel;
import com.hapramp.notification.model.MentionNotificationModel;
import com.hapramp.notification.model.NewCompetitionNotificationModel;
import com.hapramp.notification.model.ReblogNotificationModel;
import com.hapramp.notification.model.ReplyNotificationModel;
import com.hapramp.notification.model.TransferNotificationModel;
import com.hapramp.notification.model.VoteNotificationModel;

import java.util.Map;

public class NotificationParser {
  public static BaseNotificationModel parseNotification(Map<String, String> map) {
    BaseNotificationModel baseNotificationType = null;
    try {
      if (map != null) {
        baseNotificationType = new BaseNotificationModel();
        String type = map.get(NotificationKey.KEY_TYPE);
        if (type != null) {
          baseNotificationType.setType(type);
          if (type.equals(NotificationKey.NOTIFICATION_TYPE_FOLLOW)) {

            baseNotificationType = new FollowNotificationModel(
              String.valueOf(map.get(NotificationKey.KEY_TYPE)),
              String.valueOf(map.get(NotificationKey.KEY_FOLLOWER)),
              String.valueOf(map.get(NotificationKey.KEY_TIMESTAMP)));

          } else if (type.equals(NotificationKey.NOTIFICATION_TYPE_VOTE)) {

            baseNotificationType = new VoteNotificationModel(
              String.valueOf(map.get(NotificationKey.KEY_TYPE)),
              String.valueOf(map.get(NotificationKey.KEY_VOTER)),
              String.valueOf(map.get(NotificationKey.KEY_PERMLINK)),
              String.valueOf(map.get(NotificationKey.KEY_WEIGHT)),
              String.valueOf(map.get(NotificationKey.KEY_TIMESTAMP)),
              String.valueOf(map.get(NotificationKey.KEY_PARENT_PERMLINK) != null ? map.get(NotificationKey.KEY_PARENT_PERMLINK) : "")
            );

          } else if (type.equals(NotificationKey.NOTIFICATION_TYPE_MENTION)) {

            baseNotificationType = new MentionNotificationModel(
              String.valueOf(map.get(NotificationKey.KEY_TYPE)),
              Boolean.valueOf(map.get(NotificationKey.KEY_IS_ROOT_POST)),
              String.valueOf(map.get(NotificationKey.KEY_AUTHOR)),
              String.valueOf(map.get(NotificationKey.KEY_PARENT_PERMLINK)),
              String.valueOf(map.get(NotificationKey.KEY_PERMLINK)),
              String.valueOf(map.get(NotificationKey.KEY_TIMESTAMP)));
          } else if (type.equals(NotificationKey.NOTIFICATION_TYPE_REBLOG)) {

            baseNotificationType = new ReblogNotificationModel(
              String.valueOf(map.get(NotificationKey.KEY_TYPE)),
              String.valueOf(map.get(NotificationKey.KEY_ACCOUNT)),
              String.valueOf(map.get(NotificationKey.KEY_PERMLINK)),
              String.valueOf(map.get(NotificationKey.KEY_TIMESTAMP)),
              String.valueOf(map.get(NotificationKey.KEY_PARENT_PERMLINK) != null ? map.get(NotificationKey.KEY_PARENT_PERMLINK) : ""));

          } else if (type.equals(NotificationKey.NOTIFICATION_TYPE_REPLY)) {

            baseNotificationType = new ReplyNotificationModel(
              String.valueOf(map.get(NotificationKey.KEY_TYPE)),
              String.valueOf(map.get(NotificationKey.KEY_PARENT_PERMLINK) != null ? map.get(NotificationKey.KEY_PARENT_PERMLINK) : ""),
              String.valueOf(map.get(NotificationKey.KEY_AUTHOR)),
              String.valueOf(map.get(NotificationKey.KEY_PERMLINK)),
              String.valueOf(map.get(NotificationKey.KEY_TIMESTAMP)));

          } else if (type.equals(NotificationKey.NOTIFICATION_TYPE_TRANSFER)) {

            baseNotificationType = new TransferNotificationModel(
              map.get(NotificationKey.KEY_TYPE),
              String.valueOf(map.get(NotificationKey.KEY_SENDER)),
              String.valueOf(map.get(NotificationKey.KEY_AMOUNT)),
              String.valueOf(map.get(NotificationKey.KEY_MEMO)),
              String.valueOf(map.get(NotificationKey.KEY_TIMESTAMP)));
          } else if (type.equals(NotificationKey.NOTIFICATION_TYPE_CONTEST_CREATED)) {
            baseNotificationType = new ContestCreatedNotificationModel(
              String.valueOf(map.get(NotificationKey.STARTS_AT)),
              String.valueOf(map.get(NotificationKey.DESC)),
              String.valueOf(map.get(NotificationKey.ENDS_AT)),
              map.get(NotificationKey.PRIZES).split(""), //need parsing
              String.valueOf(map.get(NotificationKey.ID)),
              String.valueOf(map.get(NotificationKey.TAG)),
              String.valueOf(map.get(NotificationKey.TYPE)),
              String.valueOf(map.get(NotificationKey.IMAGE)),
              String.valueOf(map.get(NotificationKey.TITLE)));
          } else if(type.equals(NotificationKey.NOTIFICATION_TYPE_CONTEST_STARTED)){
            baseNotificationType = new ContestStartedNotificationModel(
              String.valueOf(map.get(NotificationKey.TITLE)),
              String.valueOf(map.get(NotificationKey.IMAGE)),
              String.valueOf(map.get(NotificationKey.TYPE)),
              String.valueOf(map.get(NotificationKey.ID)),
              String.valueOf(map.get(NotificationKey.TAG))
            );
          }else if(type.equals(NotificationKey.NOTIFICATION_TYPE_CONTEST_WINNERS_ANNOUNCED)){
            baseNotificationType = new ContestWinnerDeclaredNotificationModel(
              String.valueOf(map.get(NotificationKey.TITLE)),
              String.valueOf(map.get(NotificationKey.ID)),
              String.valueOf(map.get(NotificationKey.TYPE)),
              String.valueOf(map.get(NotificationKey.IMAGE))
            );
          }
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return baseNotificationType;
  }
}
