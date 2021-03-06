package taskino;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import it.sauronsoftware.cron4j.SchedulingPattern;

public class CronTrigger implements Trigger {

    private String cronExpr;

    public CronTrigger() {}

    public CronTrigger(String cronExpr) {
        this.cronExpr = cronExpr;
    }

    public String getCronExpr() {
        return cronExpr;
    }

    @Override
    public TriggerType type() {
        return TriggerType.CRON;
    }

    @Override
    public void parse(String s) {
        this.cronExpr = s;
    }

    @Override
    public String serialize() {
        return this.cronExpr;
    }

    @Override
    public ScheduledFuture<?> schedule(ScheduledExecutorService scheduler, ExecutorService executor,
                    Predicate<Task> taskTaker, Task task) {
        var pattern = new SchedulingPattern(this.getCronExpr());
        // 将毫秒数清零，确保多进程同一时间争抢
        Calendar cal = Calendar.getInstance();
        var now = new Date();
        cal.setTime(now);
        cal.set(Calendar.MILLISECOND, 0);
        // 如果正好卡在分点上（second=0）那就立即执行
        // 否则延迟到下一分钟
        if (cal.get(Calendar.SECOND) != 0) {
            cal.set(Calendar.SECOND, 0);
            cal.add(Calendar.MINUTE, 1);
        }
        long delay = cal.getTimeInMillis() - now.getTime();
        return scheduler.scheduleAtFixedRate(() -> {
            if (pattern.match(System.currentTimeMillis())) {
                if (taskTaker.test(task)) {
                    executor.submit(task);
                }
            }
        }, delay, 60 * 1000, TimeUnit.MILLISECONDS);
    }

}
