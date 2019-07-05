package com.incquerylabs.conptest;

import java.time.Instant;

public interface Receiver{
	
	public Instant getEnd(Integer n);	
	public Instant getMid(Integer n);	
	public void start();
}
