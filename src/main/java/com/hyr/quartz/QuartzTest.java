package com.hyr.quartz;

import com.hyr.quartz.job.MyJob;
import com.hyr.quartz.listener.DefaultJobListener;
import com.hyr.quartz.listener.DefaultSchedulerListener;
import com.hyr.quartz.listener.DefaultTriggerListener;
import com.hyr.quartz.service.JobService;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.plugins.history.LoggingJobHistoryPlugin;
import org.quartz.plugins.history.LoggingTriggerHistoryPlugin;
import org.quartz.plugins.management.ShutdownHookPlugin;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.simpl.SimpleClassLoadHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Properties;

/*******************************************************************************
 * @date 2018-11-01 下午 3:50
 * @author: <a href=mailto:huangyr>黄跃然</a>
 * @Description: Quartz Demo
 *
 * @see MyJob 定时任务类,封装任务的执行逻辑
 * @see JobService 定时任务业务类,封装了定时任务具体的业务逻辑,枚举单例模式
 * @see QuartzUtilsTest QuartzUtils工具测试类
 ******************************************************************************/
public class QuartzTest {

    private final static Logger log = LoggerFactory.getLogger(DefaultSchedulerListener.class);

    public static void main(String[] args) throws SchedulerException {


        //1.创建Scheduler的工厂
        Properties props = new Properties();
        props.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        props.setProperty("org.quartz.threadPool.threadCount", "4"); // 线程数
        props.setProperty("org.quartz.threadPool.threadPriority", "5"); // 线程优先级 5默认优先级
        props.setProperty("org.quartz.threadPool.threadNamePrefix", "quartz_"); // 工作线程池中线程名称的前缀将被附加前缀
        props.setProperty("org.quartz.scheduler.instanceName", "scheduler_1"); // 实例名称
        props.setProperty("org.quartz.scheduler.skipUpdateCheck","true");


        StdSchedulerFactory sf = new StdSchedulerFactory(props);
        //2.从工厂中获取调度器实例
        Scheduler scheduler1 = sf.getScheduler();
        log.info("name:{}",scheduler1.getSchedulerName());
        log.info("size:{}",sf.getAllSchedulers().size());

        // shutdownhook plugin
        ShutdownHookPlugin shutdownHookPlugin = new ShutdownHookPlugin();
        shutdownHookPlugin.initialize("QUARTZ_SHUTDOWN_HOOK_PLUGIN", scheduler1, new SimpleClassLoadHelper());
        shutdownHookPlugin.start();

        // trigger log plugin
        LoggingTriggerHistoryPlugin triggerLogPlugin = new LoggingTriggerHistoryPlugin();
        triggerLogPlugin.initialize("QUARTZ_TRIGGER_LOG_PLUGIN", scheduler1, new SimpleClassLoadHelper());
        triggerLogPlugin.start();

        // job log plugin
        LoggingJobHistoryPlugin jobLogPlugin = new LoggingJobHistoryPlugin();
        jobLogPlugin.initialize("QUARTZ_JOB_LOG_PLUGIN", scheduler1, new CascadingClassLoadHelper());
        jobLogPlugin.start();


        ListenerManager listenerManager = scheduler1.getListenerManager();
        listenerManager.addSchedulerListener(new DefaultSchedulerListener("DefaultSchedulerListener"));
        listenerManager.addJobListener(new DefaultJobListener("DefaultJobListener")); // 任务监听
        listenerManager.addTriggerListener(new DefaultTriggerListener("DefaultTriggerListener")); // 触发器监听

        //3.创建JobDetail
        JobDetail jb = JobBuilder.newJob(MyJob.class)
                .storeDurably(true) // 如果一个job是非持久的，当没有活跃的trigger与之关联的时候，会被自动地从scheduler中删除
                .requestRecovery(true) // job可恢复。scheduler发生硬关闭（hard shutdown)（比如运行的进程崩溃了，或者关机了），则当scheduler重新启动的时候，该job会被重新执行。
                .withDescription("this is a ram job") //job的描述
                .withIdentity("ramJob", "ramGroup") //job 的name和group
                .usingJobData("jobDesc", "job_01") // 属性注入
                .build();

        JobDetail jb2 = JobBuilder.newJob(MyJob.class)
                .storeDurably(true) // 如果一个job是非持久的，当没有活跃的trigger与之关联的时候，会被自动地从scheduler中删除
                .requestRecovery(true) // job可恢复。scheduler发生硬关闭（hard shutdown)（比如运行的进程崩溃了，或者关机了），则当scheduler重新启动的时候，该job会被重新执行。
                .withDescription("this is a ram job2") //job的描述
                .withIdentity("ramJob2", "ramGroup") //job 的name和group
                .usingJobData("jobDesc", "job_02") // 属性注入
                .build();

        //任务运行的时间，SimpleSchedle类型触发器有效
        long time = System.currentTimeMillis() + 3 * 1000L; //3秒后启动任务
        Date statTime = new Date(time);

        //4.创建Trigger
        //使用SimpleScheduleBuilder或者CronScheduleBuilder
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule("0/2 * * * * ?");

        //SimpleScheduleBuilder
        //SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule();
        //simpleScheduleBuilder.withIntervalInSeconds(5);

        // 设置misfire错失补偿机制

        // 以当前时间为触发频率立刻触发一次执行,然后按照Cron频率依次执行.会合并部分的misfire,正常执行下一个周期的任务.
        // 假设9，10的任务都misfire了，系统在10：15分起来了。只会执行一次misfire，下次正点执行。
        cronScheduleBuilder.withMisfireHandlingInstructionFireAndProceed(); // 默认
        // cronScheduleBuilder.withMisfireHandlingInstructionDoNothing(); // 所有的misfire不管，执行下一个周期的任务)
        // cronScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires(); //所有misfire的任务会马上执行

        Trigger trigger1 = TriggerBuilder.newTrigger()
                .withDescription("")
                .withPriority(Trigger.DEFAULT_PRIORITY) // 优先级
                .withIdentity("ramTrigger1", "ramTriggerGroup")
                //.withSchedule(SimpleScheduleBuilder.simpleSchedule())
                .startAt(statTime)  //默认当前时间启动
                .withSchedule(cronScheduleBuilder) //两秒执行一次
                .build();
        Trigger trigger2 = TriggerBuilder.newTrigger()
                .withDescription("")
                .withPriority(Trigger.DEFAULT_PRIORITY) // 优先级
                .withIdentity("ramTrigger2", "ramTriggerGroup")
                //.withSchedule(SimpleScheduleBuilder.simpleSchedule())
                .startAt(statTime)  //默认当前时间启动
                .withSchedule(cronScheduleBuilder) //两秒执行一次
                .build();

        //5.注册任务和定时器
        scheduler1.scheduleJob(jb, trigger1);
        scheduler1.scheduleJob(jb2, trigger2);

        //6.启动 调度器
        scheduler1.start();
        log.info("启动时间 ： " + new Date());

//        try {
//            Thread.sleep(5000);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        //scheduler1.shutdown();
        //System.exit(-1);

        // scheduler.deleteJob(jb.getKey()); // 删除job

    }
}