package me.ocv.ilse;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;

public class GoodQueryParser extends org.apache.lucene.queryparser.classic.QueryParser
{
	public GoodQueryParser(String f, Analyzer a)
	{
		super(f, a);
	}

	public String intfield;

	protected Query newRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive)
	{
		if (field.equals(intfield))
		{
			//return NumericRangeQuery.newLongRange(
			//	field, Long.parseLong(part1), Long.parseLong(part2), startInclusive, endInclusive);

			//return new SortedNumericDocValuesRangeQuery(field, , Long.parseLong(part2));

			long v1, v2;

			if (part1 != null && !part1.equals("*"))
			{
				v1 = Long.parseLong(part1);
			}
			else
			{
				v1 = Math.addExact(Long.MIN_VALUE, +3);
			}

			if (part2 != null && !part2.equals("*"))
			{
				v2 = Long.parseLong(part2);
			}
			else
			{
				v2 = Math.addExact(Long.MAX_VALUE, -3);
			}

			if (!startInclusive)
			{
				Math.addExact(v1, -1);
			}
			if (!endInclusive)
			{
				Math.addExact(v2, +1);
			}

			return LongPoint.newRangeQuery(field, v1, v2);
		}
		return (TermRangeQuery) super.newRangeQuery(field, part1, part2, startInclusive, endInclusive);
	}

	protected Query newTermQuery(Term term)
	{
		if (intfield.equals(term.field()))
		{
			return LongPoint.newExactQuery(field, Long.parseLong(term.text()));
		}
		return super.newTermQuery(term);
	}

	protected Query newFieldQuery(Analyzer analyzer, String field, String queryText, boolean quoted) throws ParseException
	{
		if (intfield.equals(field))
		{
			return LongPoint.newExactQuery(field, Long.parseLong(queryText));
		}
		return super.newFieldQuery(analyzer, field, queryText, quoted);
	}
}