package me.ocv.ilse;

public class SearchResult
{
	public Hit[] hits;
	public long hitcount;

	public SearchResult() {}

	public static class Hit
	{
		public String net;
		public String chan;
		public long   ts;
		public String from;
		public String msg;
		public String host;

		public Hit() {}
	}
}
