package me.ocv.ilse;

import java.time.Instant;

import me.ocv.ilse.LogIndexer;
import me.ocv.ilse.LogSearcher;
import me.ocv.ilse.WebsockSrv;

public class Main
{
	public static void main(String[] args)
	{
		new Main().run(args);
	}

	private Main() {}

	public void run(String[] args)
	{
		long t0;
		
		if (args.length != 4)
		{
			z.eprint("need argument 1:  znc log root");
			z.eprint("need argument 2:  lucene index dir");
			z.eprint("need argument 3:  websocket port");
			z.eprint("need argument 4:  websocket password");
			return;
		}

		try
		{
			z.print("  *  starting ilse core...");

			String znc_dir = args[0];
			String idx_dir = args[1];
			int    ws_port = Integer.parseInt(args[2]);
			String ws_pass = args[3];

			LogIndexer  idx  = new LogIndexer  ( znc_dir, idx_dir );
			LogSearcher srch = new LogSearcher ( znc_dir, idx_dir );
			WebsockSrv  ws   = new WebsockSrv  ( idx, srch, ws_port, ws_pass );

			// HEY LOOK UNIT TEST
			srch.test_esc();

			z.print("  *  starting searcher...");
			t0 = System.currentTimeMillis();
			srch.open();
			z.spent(t0);
			
			z.print("  *  starting indexer...");
			t0 = System.currentTimeMillis();
			idx.refresh(srch);
			z.spent(t0);

			ws.run_was_already_taken();
		}
		catch (Exception e)
		{
			z.eprint("\n\033[1;37m/!\\ \033[1;31m" + e.getClass() +
				"\033[37m :-(\033[0;31m");
			e.printStackTrace();
			z.eprint("\033[0m\n");
		}
	}
}

