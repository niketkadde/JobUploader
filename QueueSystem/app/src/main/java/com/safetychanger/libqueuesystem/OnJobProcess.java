package com.safetychanger.libqueuesystem;

/**
 * Created by niket on 14/12/14.
 */
public abstract interface OnJobProcess {

    abstract public void success(String response, int status, Job job);

    abstract public void failure(String response, int status, Job job);

    abstract public void allJobsFinished(String string);
}
