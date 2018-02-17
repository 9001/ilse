package me.ocv.ilse;

// https://stackoverflow.com/questions/156275/what-is-the-equivalent-of-the-c-pairl-r-in-java

public class Pair<A, B> {
	private final A first;
	private final B second;

	public Pair(A first, B second)
	{
		this.first  = first;
		this.second = second;
	}

	public int hashCode()
	{
		int hashFirst  =  first != null ?  first.hashCode() : 0;
		int hashSecond = second != null ? second.hashCode() : 0;

		return (hashFirst + hashSecond) * hashSecond + hashFirst;
	}

	public boolean equals(Object other)
	{
		if (other == null || this.getClass() != other.getClass())
		{
			return false;
		}

		Pair otherPair = (Pair)other;

		return
		(
			(
				this.first == otherPair.first ||
				(
					this.first != null &&
					this.first.equals(otherPair.first)
				)
			) &&
			(
				this.second == otherPair.second ||
				(
					this.second != null &&
					this.second.equals(otherPair.second)
				)
			)
		);
	}

	public String toString()
	{ 
		return "(" + first + ", " + second + ")"; 
	}

	public A v1() {
		return first;
	}

	public B v2() {
		return second;
	}
}
