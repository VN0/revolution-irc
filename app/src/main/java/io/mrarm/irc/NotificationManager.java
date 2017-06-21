package io.mrarm.irc;

import android.content.Context;
import android.text.style.ForegroundColorSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.irc.util.IRCColorUtils;

public class NotificationManager {

    public static final int CHAT_NOTIFICATION_ID_START = 10000;

    private static int mNextChatNotificationId = CHAT_NOTIFICATION_ID_START;

    public static NotificationRule sNickMentionRule;
    public static NotificationRule sDirectMessageRule;
    public static NotificationRule sNoticeRule;
    public static NotificationRule sChannelNoticeRule;
    public static NotificationRule sZNCPlaybackRule;
    private static List<NotificationRule> sDefaultRules;

    private ServerConnectionInfo mConnection;
    private Map<String, ChannelNotificationData> mChannelData = new HashMap<>();
    Map<NotificationRule, Pattern> mCompiledPatterns = new HashMap<>();

    private static void initDefaultRules() {
        sDefaultRules = new ArrayList<>();
        sNickMentionRule = new NotificationRule(R.string.notification_rule_nick, "${nick}", true);
        sDirectMessageRule = new NotificationRule(R.string.notification_rule_direct, "", true);
        sNoticeRule = new NotificationRule(R.string.notification_rule_notice, "", true);
        sChannelNoticeRule = new NotificationRule(R.string.notification_rule_chan_notice, "", true);
        sZNCPlaybackRule = new NotificationRule(R.string.notification_rule_zncplayback, "", true);
        sDefaultRules.add(sNickMentionRule);
    }

    public NotificationManager(ServerConnectionInfo connection) {
        mConnection = connection;
    }

    public NotificationRule findRule(String message) {
        for (NotificationRule rule : sDefaultRules) {
            if (rule.appliesTo(this, message) && rule.settings.enabled)
                return rule;
        }
        return null;
    }

    public ChannelNotificationData getChannelNotificationData(String channel, boolean create) {
        ChannelNotificationData ret = mChannelData.get(channel);
        if (ret == null && create) {
            ret = new ChannelNotificationData(channel);
            mChannelData.put(channel, ret);
        }
        return ret;
    }

    public Collection<ChannelNotificationData> getChannelNotificationDataList() {
        return mChannelData.values();
    }

    public String getUserNick() { // TODO: Register for nick updates
        return ((ServerConnectionApi) mConnection.getApiInstance()).getServerConnectionData().getUserNick();
    }

    static {
        initDefaultRules();
    }

    public static class ChannelNotificationData {

        private final String mChannel;
        private final int mNotificationId = mNextChatNotificationId++;
        private List<NotificationMessage> mMessages = new ArrayList<>();

        public ChannelNotificationData(String channel) {
            mChannel = channel;
        }

        public String getChannel() {
            return mChannel;
        }

        public int getNotificationId() {
            return mNotificationId;
        }

        public List<NotificationMessage> getNotificationMessages() {
            return mMessages;
        }

        public NotificationMessage addNotificationMessage(MessageInfo messageInfo) {
            NotificationMessage ret = new NotificationMessage(messageInfo);
            mMessages.add(ret);
            return ret;
        }

    }

    public static class NotificationMessage {

        private String mSender;
        private String mText;
        private CharSequence mBuilt;

        public NotificationMessage(String sender, String text) {
            this.mSender = sender;
            this.mText = text;
        }

        public NotificationMessage(MessageInfo messageInfo) {
            this(messageInfo.getSender().getNick(), messageInfo.getMessage());
        }

        private CharSequence buildNotificationText(Context context) {
            int nickColor = IRCColorUtils.getNickColor(context, mSender.toString());
            ColoredTextBuilder builder = new ColoredTextBuilder();
            builder.append(mSender + ": ", new ForegroundColorSpan(nickColor));
            builder.append(mText);
            return mBuilt = builder.getSpannable();
        }

        public CharSequence getNotificationText(Context context) {
            if (mBuilt == null)
                return buildNotificationText(context);
            return mBuilt;
        }

    }

}
