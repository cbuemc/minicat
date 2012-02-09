package com.fanfou.app.hd.api;

import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.fanfou.app.hd.App;
import com.fanfou.app.hd.db.Contents.BasicColumns;
import com.fanfou.app.hd.db.Contents.UserInfo;
import com.fanfou.app.hd.http.NetResponse;
import com.fanfou.app.hd.http.ResponseCode;
import com.fanfou.app.hd.service.Constants;
import com.fanfou.app.hd.util.StringHelper;

/**
 * @author mcxiaoke
 * @version 1.0 2011.04.30
 * @version 1.1 2011.05.01
 * @version 1.2 2011.05.02
 * @version 1.3 2011.07.21
 * @version 1.4 2011.10.21
 * @version 1.5 2011.11.04
 * @version 2.0 2011.11.10
 * @version 2.1 2011.11.11
 * @version 2.5 2011.11.15
 * @version 2.6 2011.12.16
 * @version 2.7 2011.12.19
 * @version 3.0 2011.12.21
 * 
 */
public class User implements Storable<User> {

	public static final String TAG = User.class.getSimpleName();

	public Date createdAt;
	public String id;
	public String ownerId;

	public String screenName;
	public String location;
	public String gender;
	public String birthday;

	public String description;
	public String profileImageUrl;
	public String url;
	public boolean protect;

	public int followersCount;
	public int friendsCount;
	public int favouritesCount;
	public int statusesCount;

	public boolean following;

	public int type;

	public User() {
	}

	public User(Parcel in) {
		id = in.readString();
		ownerId = in.readString();
		createdAt = new Date(in.readLong());
		type = in.readInt();

		screenName = in.readString();
		location = in.readString();
		gender = in.readString();
		birthday = in.readString();

		description = in.readString();
		profileImageUrl = in.readString();
		url = in.readString();
		protect = in.readInt() == 0 ? false : true;

		followersCount = in.readInt();
		friendsCount = in.readInt();
		favouritesCount = in.readInt();
		statusesCount = in.readInt();

		following = in.readInt() == 0 ? false : true;
	}

	@Override
	public int compareTo(User another) {
		return createdAt.compareTo(another.createdAt);
	}

	public boolean isNull() {
		return StringHelper.isEmpty(id);
	}

	public static ArrayList<User> parseUsers(NetResponse r) throws ApiException {
		return User.parseUsers(r.getJSONArray());
	}

	public static ArrayList<User> parseUsers(JSONArray a) throws ApiException {
		if (a == null) {
			return null;
		}
		ArrayList<User> users = new ArrayList<User>();
		try {
			for (int i = 0; i < a.length(); i++) {
				JSONObject o = a.getJSONObject(i);
				User u = parse(o);
				users.add(u);
			}
		} catch (JSONException e) {
			throw new ApiException(ResponseCode.ERROR_JSON_EXCEPTION, e);
		}
		return users;
	}

	public static User parse(Cursor c) {
		if (c == null) {
			return null;
		}
		User user = new User();
		user.createdAt = Parser.parseDate(c, BasicColumns.CREATED_AT);
		user.id = Parser.parseString(c, BasicColumns.ID);
		user.ownerId = Parser.parseString(c, BasicColumns.OWNER_ID);
		user.screenName = Parser.parseString(c, UserInfo.SCREEN_NAME);
		user.location = Parser.parseString(c, UserInfo.LOCATION);
		user.gender = Parser.parseString(c, UserInfo.GENDER);
		user.birthday = Parser.parseString(c, UserInfo.BIRTHDAY);
		user.description = Parser.parseString(c, UserInfo.DESCRIPTION);
		user.profileImageUrl = Parser
				.parseString(c, UserInfo.PROFILE_IMAGE_URL);
		user.url = Parser.parseString(c, UserInfo.URL);
		user.protect = Parser.parseBoolean(c, UserInfo.PROTECTED);
		user.followersCount = Parser.parseInt(c, UserInfo.FOLLOWERS_COUNT);
		user.friendsCount = Parser.parseInt(c, UserInfo.FRIENDS_COUNT);
		user.favouritesCount = Parser.parseInt(c, UserInfo.FAVORITES_COUNT);
		user.statusesCount = Parser.parseInt(c, UserInfo.STATUSES_COUNT);
		user.following = Parser.parseBoolean(c, UserInfo.FOLLOWING);
		user.type = Parser.parseInt(c, BasicColumns.TYPE);
		return user;
	}

	public static User parse(NetResponse r) throws ApiException {
		return parse(r.getJSONObject());
	}

	public static User parse(JSONObject o) throws ApiException {
		if (null == o) {
			return null;
		}
		try {
			User user = new User();
			user.id = o.getString(BasicColumns.ID);
			user.screenName = o.getString(UserInfo.SCREEN_NAME);
			user.location = o.getString(UserInfo.LOCATION);
			user.gender = o.getString(UserInfo.GENDER);
			user.birthday = o.getString(UserInfo.BIRTHDAY);
			user.description = o.getString(UserInfo.DESCRIPTION);
			user.profileImageUrl = o.getString(UserInfo.PROFILE_IMAGE_URL);
			user.url = o.getString(UserInfo.URL);
			user.protect = o.getBoolean(UserInfo.PROTECTED);
			user.followersCount = o.getInt(UserInfo.FOLLOWERS_COUNT);
			user.friendsCount = o.getInt(UserInfo.FRIENDS_COUNT);
			user.favouritesCount = o.getInt(UserInfo.FAVORITES_COUNT);
			user.statusesCount = o.getInt(UserInfo.STATUSES_COUNT);
			user.following = o.getBoolean(UserInfo.FOLLOWING);
			user.createdAt = Parser.date(o.getString(BasicColumns.CREATED_AT));

			user.type = Constants.TYPE_NONE;
			user.ownerId = App.getUserId();
			return user;
		} catch (JSONException e) {
			throw new ApiException(ResponseCode.ERROR_JSON_EXCEPTION,
					e.getMessage(), e);
		}
	}

	public ContentValues toSimpleContentValues() {
		User u = this;
		ContentValues cv = new ContentValues();

		cv.put(UserInfo.SCREEN_NAME, u.screenName);
		cv.put(UserInfo.LOCATION, u.location);
		cv.put(UserInfo.GENDER, u.gender);
		cv.put(UserInfo.BIRTHDAY, u.birthday);

		cv.put(UserInfo.DESCRIPTION, u.description);
		cv.put(UserInfo.PROFILE_IMAGE_URL, u.profileImageUrl);
		cv.put(UserInfo.URL, u.url);
		cv.put(UserInfo.PROTECTED, u.protect);

		cv.put(UserInfo.FOLLOWERS_COUNT, u.followersCount);
		cv.put(UserInfo.FRIENDS_COUNT, u.friendsCount);
		cv.put(UserInfo.FAVORITES_COUNT, u.favouritesCount);
		cv.put(UserInfo.STATUSES_COUNT, u.statusesCount);

		cv.put(UserInfo.FOLLOWING, u.following);

		return cv;
	}

	@Override
	public ContentValues toContentValues() {
		User u = this;
		ContentValues cv = new ContentValues();

		cv.put(BasicColumns.ID, u.id);
		cv.put(BasicColumns.OWNER_ID, u.ownerId);

		cv.put(UserInfo.SCREEN_NAME, u.screenName);
		cv.put(UserInfo.LOCATION, u.location);
		cv.put(UserInfo.GENDER, u.gender);
		cv.put(UserInfo.BIRTHDAY, u.birthday);

		cv.put(UserInfo.DESCRIPTION, u.description);
		cv.put(UserInfo.PROFILE_IMAGE_URL, u.profileImageUrl);
		cv.put(UserInfo.URL, u.url);
		cv.put(UserInfo.PROTECTED, u.protect);

		cv.put(UserInfo.FOLLOWERS_COUNT, u.followersCount);
		cv.put(UserInfo.FRIENDS_COUNT, u.friendsCount);
		cv.put(UserInfo.FAVORITES_COUNT, u.favouritesCount);
		cv.put(UserInfo.STATUSES_COUNT, u.statusesCount);

		cv.put(UserInfo.FOLLOWING, u.following);
		cv.put(BasicColumns.CREATED_AT, u.createdAt.getTime());

		cv.put(BasicColumns.TYPE, u.type);

		return cv;
	}

	@Override
	public String toString() {
		return "[User] " + BasicColumns.ID + "=" + id + " "
				+ UserInfo.SCREEN_NAME + "=" + screenName + " "
				+ UserInfo.LOCATION + "=" + location + " " + UserInfo.GENDER
				+ "=" + gender + " " + UserInfo.BIRTHDAY + "=" + birthday + " "
				+ UserInfo.DESCRIPTION + "=" + description + " "
				+ UserInfo.PROFILE_IMAGE_URL + "=" + profileImageUrl + " "
				+ UserInfo.URL + "=" + url + " " + UserInfo.PROTECTED + "="
				+ protect + " " + UserInfo.FOLLOWERS_COUNT + "="
				+ followersCount + " " + UserInfo.FRIENDS_COUNT + "="
				+ friendsCount + " " + UserInfo.FAVORITES_COUNT + "="
				+ favouritesCount + " " + UserInfo.STATUSES_COUNT + "="
				+ statusesCount + " " + UserInfo.FOLLOWING + "=" + following
				+ " " + BasicColumns.CREATED_AT + "=" + createdAt + " "
				+ BasicColumns.TYPE + "=" + type + " ";
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof User) {
			User u = (User) o;
			if (id.equals(u.id)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(id);
		dest.writeString(ownerId);
		dest.writeLong(createdAt.getTime());
		dest.writeInt(type);

		dest.writeString(screenName);
		dest.writeString(location);
		dest.writeString(gender);
		dest.writeString(birthday);

		dest.writeString(description);
		dest.writeString(profileImageUrl);
		dest.writeString(url);
		dest.writeInt(protect ? 1 : 0);

		dest.writeInt(followersCount);
		dest.writeInt(friendsCount);
		dest.writeInt(favouritesCount);
		dest.writeInt(statusesCount);

		dest.writeInt(following ? 1 : 0);

	}

	public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {

		@Override
		public User createFromParcel(Parcel source) {
			return new User(source);
		}

		@Override
		public User[] newArray(int size) {
			return new User[size];
		}
	};

}