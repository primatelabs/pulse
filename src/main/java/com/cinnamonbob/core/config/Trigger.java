package com.cinnamonbob.core.config;

/**
 * 
 *
 */
public interface Trigger
{
    void setSchedule(Schedule schedule);
    void trigger();
    void enable();
    void disable();
    boolean isEnabled();}
