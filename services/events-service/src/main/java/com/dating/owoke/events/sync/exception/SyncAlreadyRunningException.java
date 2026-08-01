package com.dating.owoke.events.sync.exception;

public class SyncAlreadyRunningException extends RuntimeException {
    public SyncAlreadyRunningException() { super("KudaGo event synchronization is already running"); }
}
