package com.fanfou.app.hd.dao.model;

import java.util.Date;

/**
 * @author mcxiaoke
 * @version 1.0 2011.05.15
 * @version 2.0 2012.02.21
 * 
 */
public class Search{
	public static final int TYPE_SAVED_SEARCH=701;
	public static final int TYPE_TREND=702;
	
	public String id;
	public String name;
	public String query;
	public Date createdAt;
	public String url;
	public int type;
}
