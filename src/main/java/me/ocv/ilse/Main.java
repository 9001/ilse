package me.ocv.ilse;

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
		if (args.length != 4)
		{
			eprint("need argument 1:  znc log root");
			eprint("need argument 2:  lucene index dir");
			eprint("need argument 3:  websocket port");
			eprint("need argument 4:  websocket password");
			return;
		}

		try
		{
			print("  *  starting ilse core...");

			String znc_dir = args[0];
			String idx_dir = args[1];
			int    ws_port = Integer.parseInt(args[2]);
			String ws_pass = args[3];

			LogIndexer  idx  = new LogIndexer  ( znc_dir, idx_dir );
			LogSearcher srch = new LogSearcher ( znc_dir, idx_dir );
			WebsockSrv  ws   = new WebsockSrv  ( idx, srch, ws_port, ws_pass );

			// HEY LOOK UNIT TEST
			srch.test_esc();

			print("  *  starting searcher...");
			srch.open();
			
			print("  *  starting indexer...");
			idx.refresh(srch);

			ws.run_was_already_taken();
		}
		catch (Exception e)
		{
			eprint("\n\033[1;37m/!\\ \033[1;31m" + e.getClass() +
				"\033[37m :-(\033[0;31m");
			e.printStackTrace();
			eprint("\033[0m\n");
		}
	}

	void print(String msg)
	{
		System.out.println(msg);
	}

	void eprint(String msg)
	{
		System.err.println(msg);
	}
}

