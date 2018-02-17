package me.ocv.ilse;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class z
{
	public static String now()
	{
		return ZonedDateTime.now(ZoneOffset.UTC).toString();
	}

	public static void print() { print(""); }
	public static void print(String msg)
	{
		System.out.println(msg);
	}

	public static void eprint() { eprint(""); }
	public static void eprint(String msg)
	{
		System.err.println(msg);
	}

	public static void spent(long t0)
	{
		print("     \033[1;30m" + ((System.currentTimeMillis() - t0) / 1000.0) + " seconds\033[0m");
	}
}
