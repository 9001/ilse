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

	String now()
	{
		return ZonedDateTime.now(ZoneOffset.UTC).toString();
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

	void print() { print(""); }
	void print(String msg)
	{
		System.out.println(msg);
	}

	void eprint() { eprint(""); }
	void eprint(String msg)
	{
		System.err.println(msg);
	}

	String[] scan_dir(String p_root, Map<String, Long> start_from) throws Exception
	{
		ArrayList<String> ret = new ArrayList<>();
		String[] nets = getdents(p_root);
		for (String net : nets)
		{
			String p_net = p_root + "/" + net;
			String[] chans = getdents(p_net);
			for (String chan : chans)
			{
				String net_chan_ws = net + " " + chan;

				ret.add(net_chan_ws);
				if (start_from == null)
				{
					continue;
				}

				long n_start_from = 0;
				Long nsf = start_from.get(net_chan_ws);
				
				if (false)
				{
					print("\nsize = " + start_from.size());
					print("find [" + net_chan_ws + "]");
					java.util.Iterator it = start_from.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry pair = (Map.Entry)it.next();
						System.out.println(pair.getKey() + " = " + pair.getValue());
					}
				}

				if (nsf != null)
				{
					n_start_from = nsf.longValue();
				}

				int chans_left = start_from.size() - ret.size();

				long msg_n = 0;
				String p_chan = p_net + "/" + chan;
				String[] days = getdents(p_chan);
				for (int a = 0; a < days.length; a++)
				{
					msg_n = index_day(p_root, net, chan, days[a], chans_left,
						msg_n, n_start_from, days.length == a + 1);
				}
				//return ret.toArray(new String[ret.size()]);
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	long index_day(String root, String net, String chan, String day, int chans_left,
		long msg_n, long start_indexing_from, boolean last_day) throws Exception
	{
		String subpath = net + "/" + chan + "/" + day;
		String fullpath = root + "/" + subpath;

		if (day.contains("-00"))
		{
			return msg_n;
		}
		long epoch = Instant.parse(day.substring(0,10) + "T00:00:00.000Z").getEpochSecond();

		print("\033[A [" + chans_left + "] " + subpath + "\033[K");

		BufferedReader br =
			new BufferedReader(
				new InputStreamReader(
					new FileInputStream(fullpath), "UTF-8")); //wowee
		
		String cur_line;
		String line = null;
		while ((cur_line = br.readLine()) != null)
		{
			// skip last line on last day
			// since printing might have stopped mid-sentence
			if (last_day)
			{
				if (line == null)
				{
					line = cur_line;
					continue;
				}
			}
			else
			{
				line = cur_line;
			}

			Matcher m = m_re_chat.matcher(line);
			if (m.matches())
			{
				if (msg_n >= start_indexing_from)
				{
					m_docs_indexed += 1;

					long ts = ((
						Integer.parseInt(m.group(1))) * 60 +
						Integer.parseInt(m.group(2))) * 60 +
						Integer.parseInt(m.group(3)) + epoch;

					String from = m.group(4);
					String msg = m.group(5);
					
					Document doc = new Document();
					doc.add(new SortedNumericDocValuesField("ts", ts));
					doc.add(new LongPoint    ("ts", ts));
					doc.add(new StoredField  ("ts", ts));
					doc.add(new Field("net",  net,  m_ft_key));
					doc.add(new Field("chan", chan, m_ft_key));
					doc.add(new Field("from", from, m_ft_key));
					doc.add(new Field("msg",  msg,  m_ft_text));
					m_writer.addDocument(doc);
				}
				msg_n += 1;
			}
			line = cur_line;
		}
		br.close();
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

		Map<String, Long> existing = new HashMap<>();
		
		// if we have a searcher
		// we use it to figure out how many messages we have from each chan
		// to determine which message we should start indexing from
		if (searcher != null && searcher.is_open())
		{
			print("  *  loading channel list\n");
			String[] net_chan_pairs = scan_dir(m_znc_dir, null);
			int remaining = net_chan_pairs.length;

			for (String net_chan_pair : net_chan_pairs)
			{
				print("\033[A [" + (--remaining) + "] " + net_chan_pair + "\033[K");

				String[] n_c = net_chan_pair.split(" ");
				if (n_c.length != 2)
				{
					throw new Exception("bad pair length");
				}
				long n_existing = searcher.hitcount(
					"+net:"   + searcher.esc(n_c[0]) +
					" +chan:" + searcher.esc(n_c[1]));
				
				existing.put(net_chan_pair, n_existing);
			}
		}

		Directory idx_dir = FSDirectory.open(Paths.get(m_idx_dir));
		IndexWriterConfig iwc = new IndexWriterConfig(m_pfa);
		iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		iwc.setRAMBufferSizeMB(512.0); // need -Xmx1g
		m_writer = new IndexWriter(idx_dir, iwc);
		print("  *  refreshing index\n");
		m_docs_indexed = 0;
		scan_dir(m_znc_dir, existing);
		print("  *  added " + m_docs_indexed + " lines");
		m_writer.close();
		idx_dir.close();
		searcher.open();
	}
}

