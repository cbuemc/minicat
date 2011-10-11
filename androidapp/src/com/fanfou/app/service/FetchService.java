package com.fanfou.app.service;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.fanfou.app.App;
import com.fanfou.app.api.Api;
import com.fanfou.app.api.ApiException;
import com.fanfou.app.api.DirectMessage;
import com.fanfou.app.api.Parser;
import com.fanfou.app.api.Status;
import com.fanfou.app.api.User;
import com.fanfou.app.config.Commons;
import com.fanfou.app.db.Contents.BasicColumns;
import com.fanfou.app.db.Contents.DirectMessageInfo;
import com.fanfou.app.db.Contents.StatusInfo;
import com.fanfou.app.db.Contents.UserInfo;
import com.fanfou.app.util.Utils;

/**
 * @author mcxiaoke
 * @version 1.0 20110602
 * @version 2.0 20110714
 * @version 2.1 2011.10.10
 * 
 */
public class FetchService extends BaseIntentService {
	private static final String tag = FetchService.class.getSimpleName();

	ResultReceiver receiver;

	public void log(String message) {
		Log.i(tag, message);
	}

	private int mType;
	private Bundle mBundle;

	public FetchService() {
		super("FetchService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		mType = intent.getIntExtra(Commons.EXTRA_TYPE, -1);
		mBundle = intent.getBundleExtra(Commons.EXTRA_BUNDLE);
		receiver = intent.getParcelableExtra(Commons.EXTRA_RECEIVER);
		if (receiver != null) {
			receiver.send(Commons.RESULT_CODE_START, null);
		}
		switch (mType) {
		case Status.TYPE_USER:
		case Status.TYPE_FAVORITES:
		case Status.TYPE_HOME:
		case Status.TYPE_MENTION:
		case Status.TYPE_PUBLIC:
			doFetchStatuses();
			break;
		case DirectMessage.TYPE_IN:
			doFetchMessages();
			break;
		case DirectMessage.TYPE_OUT:
			doFetchMessages();
			break;
		case DirectMessage.TYPE_ALL:
			doFetchMessagesAll();
			break;
		case User.TYPE_FRIENDS:
		case User.TYPE_FOLLOWERS:
			doFetchUsers(mBundle);
			break;
		default:
			break;
		}
	}

	private void cleanUsers(String userId, int type) {
		ContentResolver cr = getContentResolver();
		String where = BasicColumns.OWNER_ID + "=? AND " + BasicColumns.TYPE
				+ "=? ";
		String[] whereArgs = new String[] { userId, String.valueOf(type) };
		int result = cr.delete(UserInfo.CONTENT_URI, where, whereArgs);
		if (App.DEBUG)
			log("cleanUsers ownerId=" + userId + " type=" + mType + " result="
					+ result);
	}

	private void doFetchUsers(Bundle bundle) {
		String ownerId = bundle.getString(Commons.EXTRA_ID);
		int page = bundle.getInt(Commons.EXTRA_PAGE);
		if (App.DEBUG)
			log("doFetchUsers ownerId=" + ownerId + " page=" + page);

		Api api = App.me.api;
		try {
			List<User> users = null;
			if (mType == User.TYPE_FRIENDS) {
				users = api.usersFriends(ownerId, page);
			} else if (mType == User.TYPE_FOLLOWERS) {
				users = api.usersFollowers(ownerId, page);
			}
			if (users != null && users.size() > 0) {
				int size = users.size();
				if (App.DEBUG)
					log("doFetchUsers size=" + size);

				if (page <= 1) {
					cleanUsers(ownerId, mType);
				}

				ContentResolver cr = getContentResolver();
				cr.bulkInsert(UserInfo.CONTENT_URI,
						Parser.toContentValuesArray(users));
				sendCountMessage(size);
			} else {
				sendCountMessage(0);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			handleError(e);
		}
	}

	private void sendCountMessage(int size) {
		Bundle update = new Bundle();
		update.putInt(Commons.EXTRA_TYPE, mType);
		update.putInt(Commons.EXTRA_COUNT, size);
		receiver.send(Commons.RESULT_CODE_FINISH, update);
	}

	private void doFetchMessages() {
		boolean doGetMore = mBundle.getBoolean(Commons.EXTRA_BOOLEAN);
		if (doGetMore) {
			sendCountMessage(doFetchMessagesMore(mType));
		} else {
			sendCountMessage(doFetchMessagesRefresh(mType));
		}
	}

	private void doFetchMessagesAll() {
		boolean doGetMore = mBundle.getBoolean(Commons.EXTRA_BOOLEAN);
		Cursor c = getContentResolver().query(DirectMessageInfo.CONTENT_URI,
				DirectMessageInfo.COLUMNS, null, null, null);
		String maxId = null;
		String sinceId = null;
		Api api = App.me.api;

		if (doGetMore) {
			maxId = Utils.getDmMaxId(c);
		} else {
			sinceId = Utils.getDmSinceId(c);
		}

		if (App.DEBUG) {
			log("doFetchMessagesAll doGetMore=" + doGetMore + " type=" + mType
					+ " maxId=" + (maxId == null ? "null" : maxId)
					+ " sinceId=" + (sinceId == null ? "null" : sinceId));
		}

		try {
			List<DirectMessage> messages = new ArrayList<DirectMessage>();
			List<DirectMessage> in = api.messagesInbox(0, 0, sinceId, maxId);
			if (in != null && in.size() > 0) {
				messages.addAll(in);
			}
			List<DirectMessage> out = api.messagesOutbox(0, 0, sinceId, maxId);
			if (out != null && out.size() > 0) {
				messages.addAll(out);
			}
			if (messages != null && messages.size() > 0) {
				ContentResolver cr = getContentResolver();
				int size = messages.size();
				log("doFetchMessagesAll size()=" + size);
				cr.bulkInsert(DirectMessageInfo.CONTENT_URI,
						Parser.toContentValuesArray(messages));
				sendCountMessage(size);
			} else {
				log("doFetchMessagesAll size()=0");
				sendCountMessage(0);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			handleError(e);
		}
	}

	private int doFetchMessagesMore(int type) {

		int result = 0;

		String[] projection = new String[] { BasicColumns.ID,
				BasicColumns.CREATED_AT };
		String where = BasicColumns.TYPE + "=?";
		String[] whereArgs = new String[] { String.valueOf(type) };
		Cursor c = getContentResolver().query(DirectMessageInfo.CONTENT_URI,
				projection, where, whereArgs, null);

		String maxId = Utils.getDmMaxId(c);
		Api api = App.me.api;
		try {

			if (App.DEBUG) {
				log("doFetchMessagesMore type=" + type + " maxId="
						+ (maxId == null ? "null" : maxId));
			}

			List<DirectMessage> messages = new ArrayList<DirectMessage>();
			if (type == DirectMessage.TYPE_IN) {

				messages = api.messagesInbox(0, 0, null, maxId);
			} else if (type == DirectMessage.TYPE_OUT) {
				messages = api.messagesInbox(0, 0, null, maxId);
			}
			if (messages != null && messages.size() > 0) {
				ContentResolver cr = getContentResolver();
				int size = messages.size();
				Log.d(tag, "doFetchMessagesMore size()=" + size);
				cr.bulkInsert(DirectMessageInfo.CONTENT_URI,
						Parser.toContentValuesArray(messages));
				result = size;
			} else {
				log("doFetchMessagesMore size()=0");
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			handleError(e);
		}
		return result;
	}

	private int doFetchMessagesRefresh(int type) {
		if (App.DEBUG) {
			log("doFetchMessagesRefresh type=" + type);
		}
		int result = 0;

		String[] projection = new String[] { BasicColumns.ID,
				BasicColumns.CREATED_AT };
		String where = BasicColumns.TYPE + "=?";
		String[] whereArgs = new String[] { String.valueOf(type) };
		Cursor c = getContentResolver().query(DirectMessageInfo.CONTENT_URI,
				projection, where, whereArgs, null);

		String sinceId = Utils.getDmSinceId(c);
		Api api = App.me.api;
		try {

			if (App.DEBUG) {
				log("doFetchMessagesRefresh type=" + type + " sinceId="
						+ (sinceId == null ? "null" : sinceId));
			}

			List<DirectMessage> messages = new ArrayList<DirectMessage>();
			if (type == DirectMessage.TYPE_IN) {
				messages = api.messagesInbox(0, 0, sinceId, null);
			} else if (type == DirectMessage.TYPE_OUT) {
				messages = api.messagesInbox(0, 0, sinceId, null);
			}
			if (messages != null && messages.size() > 0) {
				ContentResolver cr = getContentResolver();
				int size = messages.size();
				Log.d(tag, "doFetchMessagesRefresh size()=" + size);
				cr.bulkInsert(DirectMessageInfo.CONTENT_URI,
						Parser.toContentValuesArray(messages));
				result = size;
			} else {
				Log.d(tag, "doFetchMessagesRefresh size()=0");
			}

		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			handleError(e);
		}
		return result;
	}

	@SuppressWarnings("unused")
	private void cleanStatuses(String userId) {
		Uri cleanUri = Uri.withAppendedPath(StatusInfo.CONTENT_URI,
				"action/clean/" + mType);
		String where = null;
		String[] whereArgs = null;
		if (mType == Status.TYPE_USER) {
			where = StatusInfo.USER_ID + "=?";
			whereArgs = new String[] { userId };
		} else if (mType == Status.TYPE_FAVORITES) {
			where = BasicColumns.OWNER_ID + "=?";
			whereArgs = new String[] { userId };
		}
		int result = getContentResolver().delete(cleanUri, where, whereArgs);
		if (App.DEBUG)
			log("cleanStatuses() deleted statuses count=" + result + " userId="
					+ userId);
	}

	private void cleanStatuses() {
		Uri cleanUri = Uri.withAppendedPath(StatusInfo.CONTENT_URI,
				"action/clean/" + mType);
		int result = getContentResolver().delete(cleanUri, null, null);
		if (App.DEBUG)
			log("cleanStatuses() deleted statuses count=" + result);
	}

	private void doFetchStatuses() {
		if (App.DEBUG)
			Log.e(tag, "doFetchStatuses");
		Api api = App.me.api;
		List<Status> statuses = null;
		int count = mBundle.getInt(Commons.EXTRA_COUNT);
		int page = mBundle.getInt(Commons.EXTRA_PAGE);
		String userId = mBundle.getString(Commons.EXTRA_ID);
		String sinceId = mBundle.getString(Commons.EXTRA_SINCE_ID);
		String maxId = mBundle.getString(Commons.EXTRA_MAX_ID);
		boolean format = mBundle.getBoolean(Commons.EXTRA_FORMAT, false);
		format = true;
		try {
			switch (mType) {
			case Status.TYPE_HOME:
				if (App.DEBUG)
					Log.e(tag, "doFetchStatuses TYPE_HOME");
				statuses = api
						.homeTimeline(count, page, sinceId, maxId, format);

				break;
			case Status.TYPE_MENTION:
				if (App.DEBUG)
					Log.e(tag, "doFetchStatuses TYPE_MENTION");
				statuses = api.mentions(count, page, sinceId, maxId, format);
				break;
			case Status.TYPE_PUBLIC:
				if (App.DEBUG)
					Log.e(tag, "doFetchStatuses TYPE_PUBLIC");
				statuses = api.pubicTimeline(count, format);
				break;
			case Status.TYPE_FAVORITES:
				if (App.DEBUG)
					Log.e(tag, "doFetchStatuses TYPE_FAVORITES");
				statuses = api.favorites(count, page, userId, format);
				break;
			case Status.TYPE_USER:
				if (App.DEBUG)
					Log.e(tag, "doFetchStatuses TYPE_USER");
				statuses = api.userTimeline(count, page, userId, sinceId,
						maxId, format);
				break;
			default:
				break;
			}
			if (statuses == null || statuses.size() == 0) {
				sendCountMessage(0);
				if (App.DEBUG)
					Log.e(tag, "doFetchStatuses received no items.");
				return;
			} else {
				int size = statuses.size();
				if (App.DEBUG)
					Log.e(tag, "doFetchStatuses received items count=" + size);
				ContentResolver cr = getContentResolver();
				cr.bulkInsert(StatusInfo.CONTENT_URI,
						Parser.toContentValuesArray(statuses));
				sendCountMessage(size);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				log("doFetchStatuses [error]" + e.statusCode + ":"
						+ e.errorMessage);
				e.printStackTrace();
			}
			handleError(e);
		}
	}

	private void handleError(ApiException e) {
		Bundle error = new Bundle();
		error.putSerializable(Commons.EXTRA_ERROR, e.getCause());
		error.putInt(Commons.EXTRA_ERROR_CODE, e.statusCode);
		error.putString(Commons.EXTRA_ERROR_MESSAGE, e.getMessage());
		receiver.send(Commons.RESULT_CODE_ERROR, error);
	}

}
