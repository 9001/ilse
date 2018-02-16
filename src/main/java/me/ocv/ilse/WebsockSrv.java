package me.ocv.ilse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.StringBuilder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class WebsockSrv extends WebSocketServer
{
	LogIndexer  m_idx;
	LogSearcher m_srch;
	int         m_port;
	String      m_pass;
	boolean     m_running;
	HashSet<WebSocket> m_authed;
	Pattern     m_ctl_codes;

	public WebsockSrv(LogIndexer idx, LogSearcher srch, int ws_port, String ws_pass) throws Exception
	{
		// listen to 127.0.0.1 only,
		// use nginx as reverse proxy for TLS / access control
		super(new InetSocketAddress(InetAddress.getByAddress(
			new byte[] { 127, 0, 0, 1 }), ws_port));

		m_idx = idx;
		m_srch = srch;
		m_port = ws_port;
		m_pass = ws_pass;
		m_running = false;
		m_authed = new HashSet<>();

		m_ctl_codes = Pattern.compile("[\\x00-\\x1f]",
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

	void cprint(WebSocket conn, String msg)
	{
		print(now() + "  " + conn.getRemoteSocketAddress().getAddress().getHostAddress() + "  " + msg);
	}
	
	@Override
	public void onStart()
	{
		print("  *  ilse is up");
	}

	@Override
	public void onError(WebSocket conn, Exception ex)
	{
		ex.printStackTrace();
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake)
	{
		String utc = now();
		conn.send("server utc: " + utc);
		rm_auth(conn);
		cprint(conn, "join");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote)
	{
		rm_auth(conn);
		cprint(conn, "left");
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer message)
	{
		try
		{
			onMessage(conn, new String(message.array(), "UTF-8"));
		}
		catch (Exception e)
		{
			cprint(conn, "garbage");
		}
	}

	@Override
	public void onMessage(WebSocket conn, String message)
	{
		if (m_authed.contains(conn))
		{
			proc_cmd(conn, message);
			return;
		}
		if (message.contains(" " + m_pass + " "))
		{
			cprint(conn, "authed!");
			conn.send("[AUTH_OK]");
			set_auth(conn);
			return;
		}
		cprint(conn, "NG: " + message);
		conn.send("[AUTH_NG]");
	}

	void set_auth(WebSocket conn)
	{
		rm_auth(conn);
		m_authed.add(conn);
	}

	void rm_auth(WebSocket conn)
	{
		if (m_authed.contains(conn))
		{
			m_authed.remove(conn);
		}
	}

	public void run_was_already_taken()
	{
		//WebSocketImpl.DEBUG = true;

		print("  *  starting ws...");
		
		m_running = true;
		this.start();
		
		try
		{
			while (m_running)
			{
				Thread.sleep(500);
			}
			this.stop(1000);
		}
		catch (Exception dontcare) {}
	}

	void proc_cmd(WebSocket conn, String cmd)
	{
		try
		{
			cprint(conn, "ok: " + cmd);

			if (cmd.substring(0, 1).equals(":"))
			{
				if (cmd.equals(":upd"))
				{
					conn.send("[REFRESH_START]");
					m_idx.refresh(m_srch);
					conn.send("[REFRESH_DONE]");
				}

				if (cmd.equals(":end"))
				{
					print("  *  shutting down...");
					m_srch.close();
					this.stop();
					m_running = false;
				}
				return;
			}

			if (!m_srch.is_open())
			{
				print("???");
				return;
			}

			//print(String.valueOf(m_srch.hitcount(cmd)));
			SearchResult sr = m_srch.search(cmd);

			StringBuilder sb = new StringBuilder();
			sb.append("{\"hitcount\":");
			sb.append(sr.hitcount);
			sb.append(", \"docs\":[\n");
			for (SearchResult.Hit hit : sr.hits)
			{
				sb.append("{ \"net\": \"");
				sb.append(hit.net.replace("\\", "\\\\").replace("\"", "\\\""));
				sb.append("\", \"chan\": \"");
				sb.append(hit.chan.replace("\\", "\\\\").replace("\"", "\\\""));
				sb.append("\", \"ts\": ");
				sb.append(hit.ts);
				sb.append(", \"from\": \"");
				sb.append(hit.from.replace("\\", "\\\\").replace("\"", "\\\""));
				sb.append("\", \"msg\": \"");
				sb.append(hit.msg.replace("\\", "\\\\").replace("\"", "\\\""));
				sb.append("\" },\n");
			}
			sb.append("{ \"eof\": \"eof\" }\n");
			sb.append("]}\n");
			//conn.send(sb.toString());

			Matcher ctl_codes_matcher = m_ctl_codes.matcher(sb.toString());
			conn.send(ctl_codes_matcher.replaceAll(" "));
		}
		catch (Exception e)
		{
			eprint("\n\033[1;37m/!\\ \033[1;31m" + e.getClass() +
				"\033[37m :-(\033[0;31m");
			e.printStackTrace();
			eprint("\033[0m\n");
		}
	}
}

