package me.ocv.ilse;

import me.ocv.ilse.SearchResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
//import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class LogSearcher
{
	String                  m_znc_dir;
	String                  m_idx_dir;
	IndexReader             m_reader;
	IndexSearcher           m_searcher;
	PerFieldAnalyzerWrapper m_pfa;
	GoodQueryParser         m_parser;
	Pattern                 m_cl_esc;

	public LogSearcher(String znc_dir, String idx_dir) throws Exception
	{
		m_znc_dir = znc_dir;
		m_idx_dir = idx_dir;

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

		m_parser = new GoodQueryParser("msg", m_pfa);
		m_parser.intfield = "ts";

		// + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
		m_cl_esc = Pattern.compile("([\\]\\[+!(){}^\"~*?:\\\\/\\-]|&&|\\|\\|)",
			Pattern.UNICODE_CASE | Pattern.CANON_EQ | Pattern.CASE_INSENSITIVE);
	}

	String now()
	{
		return ZonedDateTime.now(ZoneOffset.UTC).toString();
	}

	void print(String msg)
	{
		System.out.println(msg);
	}

	void eprint(String msg)
	{
		System.err.println(msg);
	}

	public void open() throws Exception
	{
		try
		{
			m_reader = DirectoryReader.open(FSDirectory.open(Paths.get(m_idx_dir)));
			m_searcher = new IndexSearcher(m_reader);
		}
		catch (Exception e)
		{
			eprint("\033[1;33msrch.open() err: \033[22m" + e.getMessage() + "\033[0m");
			close();
		}
	}

	public void close() throws Exception
	{
		try
		{
			m_reader.close();
		}
		catch (Exception e) {}

		m_reader = null;
		m_searcher = null;
	}

	public boolean is_open()
	{
		return m_searcher != null;
	}

	public long hitcount(String str_query) throws Exception
	{
		Query query = m_parser.parse(str_query);
		//print(query.toString());
		return hitcount(query);
	}

	public SearchResult search(String str_query) throws Exception
	{
		Query query = m_parser.parse(str_query);
		//print(query.toString());
		return search(query);
	}

	public long hitcount(Query query) throws Exception
	{
		return m_searcher.search(query, 1).totalHits;
	}

	public SearchResult search(Query query) throws Exception
	{
		int ret_len = 100;

		TopFieldDocs result = m_searcher.search(query, ret_len,
			new Sort(new SortedNumericSortField(
				"ts", SortField.Type.LONG, true)));
		
		if (ret_len > result.totalHits)
		{
			ret_len = (int)result.totalHits;
		}

		SearchResult ret = new SearchResult();
		ret.hitcount = result.totalHits;
		ScoreDoc[] hits = result.scoreDocs;
		
		ret.hits = new SearchResult.Hit[ret_len];
		for (int a = 0; a < ret_len; a++)
		{
			Document doc = m_searcher.doc(hits[a].doc);
        	SearchResult.Hit h = new SearchResult.Hit();
        	h.net  = doc.get("net");
        	h.chan = doc.get("chan");
        	h.ts   = Long.parseLong(doc.get("ts"));
        	h.from = doc.get("from");
        	h.msg  = doc.get("msg");
        	h.host = doc.get("host");
        	ret.hits[a] = h;
		}
		return ret;
	}

	public String esc(String q_s)
	{
		Matcher asdf = m_cl_esc.matcher(q_s);
		return asdf.replaceAll("\\\\$1");
	}

	void test_esc() throws Exception
	{
		// + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
		String bad  = "+ - && || ! ( ) { } [ ] ^ \" ~ * ? : \\ / & | # $ ;";
		String good = "\\+ \\- \\&& \\|| \\! \\( \\) \\{ \\} \\[ \\] \\^ \\\" \\~ \\* \\? \\: \\\\ \\/ & | # $ ;";
		String chk = esc(bad);
		
		//if (good == chk)      print("ok"); else print("err");  // err
		//if (good.equals(chk)) print("ok"); else print("err");  // ok
		
		if (!good.equals(chk))
			throw new Exception(
				"\nescaping [\033[37m " + bad +
				" \033[31m]\nexpected [\033[37m " + good +
				" \033[31m]\n     got [\033[37m " + chk +
				" \033[31m]\n");
	}
}
