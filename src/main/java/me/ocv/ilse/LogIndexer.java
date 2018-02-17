package me.ocv.ilse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class LogIndexer
{
	private class Msg
	{
		public long ts;
		public String net, chan, from, msg;
		
		public Msg(long ts, String net, String chan, String from, String msg)
		{
			this.ts = ts;
			this.net = net;
			this.chan = chan;
			this.from = from;
			this.msg = msg;
		}
	}
	
	private class Feed
	{
		String net;
		String chan;
		long epoch;
		boolean last_day;

		String next_line;
		BufferedReader br;
		
		public Feed(String path, String net, String chan,
			long epoch, boolean last_day) throws Exception
		{
			this.net = net;
			this.chan = chan;
			this.epoch = epoch;
			this.last_day = last_day;
			this.next_line = null;
			this.br =
				new BufferedReader(
					new InputStreamReader(
						new FileInputStream(path), "UTF-8"));
		}
		
		public Object get() throws Exception
		{
			String from_file;
			while ((from_file = br.readLine()) != null)
			{
				// skip last line on last day
				// since printing might have stopped mid-sentence
				if (last_day)
				{
					if (next_line == null)
					{
						next_line = from_file;
						continue;
					}
				}
				else
				{
					next_line = from_file;
				}
				
				String line = next_line;
				next_line = from_file;

				Matcher m = m_re_chat.matcher(line);
				if (m.matches())
				{
					long ts = ((
						Integer.parseInt(m.group(1))) * 60 +
						Integer.parseInt(m.group(2))) * 60 +
						Integer.parseInt(m.group(3)) + epoch;

					String from = m.group(4);
					String msg = m.group(5);
					
					return new Msg(ts, net, chan, from, msg);
				}
			}
			close();
			return null;					
		}
		
		public void close()
		{
			try
			{
				br.close();
			}
			catch (Exception e) {}
		}
	}


	
	String                  m_znc_dir;
	String                  m_idx_dir;
	Pattern                 m_re_chat;
	FieldType               m_ft_key;
	FieldType               m_ft_text;
	PerFieldAnalyzerWrapper m_pfa;
	IndexWriter             m_writer;
	long                    m_docs_indexed;

	public LogIndexer(String znc_dir, String idx_dir) throws Exception
	{
		m_znc_dir = znc_dir;
		m_idx_dir = idx_dir;

		m_re_chat = Pattern.compile("^\\[(..):(..):(..)\\] <([^> ]*)> (.*)$");
		
		Analyzer lc_keyword = CustomAnalyzer.builder()
			.withTokenizer(KeywordTokenizerFactory.class)
			.addTokenFilter(LowerCaseFilterFactory.class)
			.build();

		Map<String,Analyzer> apf = new HashMap<>();
		apf.put("net",  lc_keyword);
		apf.put("chan", lc_keyword);
		apf.put("from", lc_keyword);

		m_pfa = new PerFieldAnalyzerWrapper(
			//new SimpleAnalyzer(Version.LUCENE_7_2_1), apf);
			new StandardAnalyzer(), apf);

		m_ft_key = new FieldType();
		m_ft_key.setIndexOptions(IndexOptions.DOCS_AND_FREQS);  // the default doesn't work (???)
		m_ft_key.setStored(true);
		m_ft_key.setTokenized(true);

		m_ft_text = new FieldType();
		m_ft_text.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		m_ft_text.setStored(true);
		m_ft_text.setTokenized(true);
		//m_ft_text.setStoreTermVectors(true);
		//m_ft_text.setStoreTermVectorsPositions(true);
		//m_ft_text.setStoreTermVectorsOffsets(true);
		//m_ft_text.setStoreTermVectorsPayloads(true);
	}

	String[] getdents(String root)
	{
		String[] ret = new File(root).list();
		if (ret != null)
		{
			Arrays.sort(ret);
			return ret;
		}
		return new String[0];
	}
	
	String[] log_file_range(String p_root) throws Exception
	{
		ArrayList<String> ret = new ArrayList<>();
		String[] nets = getdents(p_root);
		for (String net : nets)
		{
			String p_net = p_root + "/" + net;
			String[] chans = getdents(p_net);
			for (String chan : chans)
			{
				String p_chan = p_net + "/" + chan;
				String[] days = getdents(p_chan);
				
				String newest_with_content = null;
				for (int a = days.length - 1; a >= 0; a--)
				{
					Feed feed = new Feed(
						p_chan + "/" + days[a], "", "", 0, false);
					
					if (feed.get() != null)
					{
						newest_with_content = days[a].substring(0,10);
						feed.close();
						break;
					}
				}
				
				if (newest_with_content != null)
				{
					String oldest_file = days[0].substring(0,10);
					
					ret.add(
						net + " " +
						chan + " " +
						oldest_file + " " +
						newest_with_content);
				}
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	void index_dir(String p_root, Map<String, Pair<String, Integer>> start_from_tab) throws Exception
	{
		String today = z.now().substring(0, 10) + ".log";
		
		int chans_seen = 0;
		String[] nets = getdents(p_root);
		for (String net : nets)
		{
			String p_net = p_root + "/" + net;
			String[] chans = getdents(p_net);
			for (String chan : chans)
			{
				String net_chan_ws = net + " " + chan;
				Pair<String, Integer> start_from = start_from_tab.get(net_chan_ws);
				if (start_from == null)
				{
					continue;
				}
				String first_file = start_from.v1() + ".log";
				int start_msg_n = start_from.v2();
				int chans_left = start_from_tab.size() - (chans_seen++);

				long msg_n = 0;
				boolean active = false;
				String p_chan = p_net + "/" + chan;
				String[] days = getdents(p_chan);
				for (int a = 0; a < days.length; a++)
				{
					if (days[a].equals(first_file))
						active = true;
					
					if (!active)
						continue;
					
					msg_n = index_day(p_root, net, chan, days[a], chans_left,
						msg_n, start_msg_n, days[a].equals(today));
				}
				//return ret.toArray(new String[ret.size()]);
			}
		}
	}

	long index_day(String root, String net, String chan, String day, int chans_left,
		long msg_n, long start_msg_n, boolean last_day) throws Exception
	{
		String subpath = net + "/" + chan + "/" + day;
		String fullpath = root + "/" + subpath;

		if (day.contains("-00"))
		{
			return msg_n;
		}
		long epoch = Instant.parse(
			day.substring(0,10) + "T00:00:00.000Z").getEpochSecond();

		z.print("\033[A [" + chans_left + "] " + subpath + "\033[K");

		Feed feed = new Feed(fullpath, net, chan, epoch, last_day);
		while (true)
		{
			Object obj = feed.get();
			
			if (obj == null)
			{
				break;
			}
			
			if (obj instanceof Msg)
			{
				if (msg_n >= start_msg_n)
				{
					m_docs_indexed += 1;
					
					Msg m = (Msg)obj;
					//z.print("\033[A[" + m.chan + "] <" + m.from + "> " + m.msg + "\n");
					
					Document doc = new Document();
					doc.add(new SortedNumericDocValuesField("ts", m.ts));
					doc.add(new LongPoint   ("ts", m.ts));
					doc.add(new StoredField ("ts", m.ts));
					doc.add(new Field("net",  m.net,  m_ft_key));
					doc.add(new Field("chan", m.chan, m_ft_key));
					doc.add(new Field("from", m.from, m_ft_key));
					doc.add(new Field("msg",  m.msg,  m_ft_text));
					m_writer.addDocument(doc);
				}
				
				msg_n += 1;
			}
			else
			{
				z.print("???");
			}
		}

		return msg_n;
	}

	public void refresh(LogSearcher searcher) throws Exception
	{
		// scope for why_tho.txt
		{
			File x = new File(m_idx_dir);
			if (!x.exists())
				x.mkdir();
		}

		// <"net chan", <2009-09-09, 123>>
		Map<String, Pair<String, Integer>> existing = new HashMap<>();
		
		z.print("  *  loading channel list\n");
		String[] ncfl_list = log_file_range(m_znc_dir);
		int remaining = ncfl_list.length;

		for (String ncfl : ncfl_list)
		{
			String[] ncfla  = ncfl.split(" ");
			String   net    = ncfla[0];
			String   chan   = ncfla[1];
			String   oldest = ncfla[2];
			String   newest = ncfla[3];
			
			z.print("\033[A [" + (--remaining) + "] " +
				net + " " + chan + "\033[K");

			Pair<String, Integer> last_hit = null;
			Pair<String, Integer> genesis = new Pair<>(oldest, 0);
			
			if (searcher == null || !searcher.is_open())
			{
				existing.put(net + " " + chan, genesis);
				continue;
			}
			
			String[] lymd = newest.split("-");
			int y0 = Integer.parseInt(lymd[0]);
			int m0 = Integer.parseInt(lymd[1]);
			int d0 = Integer.parseInt(lymd[2]);
			
			String query_base =
				"+chan:" + searcher.esc(chan) +
				" +net:" + searcher.esc(net);
			
			for (int y = y0; (y > 1970 && last_hit == null); y--)
			{
				for (int m = m0; (m > 0 && last_hit == null); m--)
				{
					m0 = 12;
					for (int d = d0; d > 0; d--)
					{
						d0 = 31;
						String ymd = String.format(
							"%04d-%02d-%02d", y, m, d);
						
						long epoch;
						try
						{
							epoch = Instant.parse(
								ymd + "T00:00:00.000Z").getEpochSecond();
						}
						catch (Exception e)
						{
							continue;
						}
						
						String query = query_base + " +ts:[" + epoch + " TO *]";
						long hits = searcher.hitcount(query);
						
						if (hits > 0)
						{
							last_hit = new Pair<String, Integer>(
								ymd, (int)hits);

							break;
						}
						else if (ymd.equals(oldest))
						{
							last_hit = genesis;
							break;
						}
					}
				}
			}
			
			if (last_hit == null)
			{
				last_hit = genesis;
			}
			existing.put(net + " " + chan, last_hit);
		}

		Directory idx_dir = FSDirectory.open(Paths.get(m_idx_dir));
		IndexWriterConfig iwc = new IndexWriterConfig(m_pfa);
		iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		iwc.setRAMBufferSizeMB(512.0); // need -Xmx1g
		m_writer = new IndexWriter(idx_dir, iwc);
		z.print("  *  refreshing index\n");
		m_docs_indexed = 0;
		index_dir(m_znc_dir, existing);
		z.print("  *  added " + m_docs_indexed + " lines");
		m_writer.close();
		idx_dir.close();
		searcher.open();
	}
}

