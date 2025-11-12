package hu.uni_obuda.thesis.railways.util.scheduler.entity;

public interface CronEntity {
    Integer getId();
    Integer getJobId();
    String getChronExpression();
}
