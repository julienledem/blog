import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

public class MemoryTest {
    
    static boolean stop = false;
    static MemoryPoolMXBean tenuredGenPool = null;
    
    public static void main(String[] args) {
        for (MemoryPoolMXBean pool :
            ManagementFactory.getMemoryPoolMXBeans()) {
            // see http://www.javaspecialists.eu/archive/Issue092.html
          if (pool.getType() == MemoryType.HEAP && pool.isUsageThresholdSupported()) {
              tenuredGenPool = pool;
          }
        }
        
        // setting the threshold to 80% usage of the memory
        tenuredGenPool.setCollectionUsageThreshold((int)Math.floor(tenuredGenPool.getUsage().getMax()*0.8));
        tenuredGenPool.setUsageThreshold((int)Math.floor(tenuredGenPool.getUsage().getMax()*0.8));
        
        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        NotificationEmitter emitter = (NotificationEmitter) mbean;
        emitter.addNotificationListener(new NotificationListener() {
          public void handleNotification(Notification n, Object hb) {
            if (n.getType().equals(
                MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED)) {
                // this is th warning we want
                System.err.println("memory collection threshold exceeded !!! : \n       "+memString());
                stop=true;

            } else if (n.getType().equals(
                    MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
                // just FYI
                System.out.println("memory threshold exceeded !!! : \n       "+memString());
            }
          }
        }, null, null);
        
        // big string so that it does not take to long
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 1000; i++) {
            sb.append("all work and no play... makes this a long string\n");
        }
        
        long t0 = 0;
        // I don't want the list resizing to interfere so initialize a big capacity
        int capacity = 1000000;
        List<String> strings = new ArrayList<String>(capacity);
        for (int i = 0; i < capacity && !stop; i++) {
            // increase memory usage
            strings.add("randomString:"+Math.random()+sb);
            // and also make sure that we generate objects that need to be garbage collected to see the difference between the 2 thresholds
            for (int j = 0; j < 10; j++) {
                strings.set((int)Math.floor(Math.random()*strings.size()), "randomString:"+Math.random()+sb);
            }
            if (i%100 == 0) {
                long t1 = System.currentTimeMillis();
                if (t1-t0 > 100) {
                    t0 = t1;
                    // print out the memory usage every now and then
                    System.out.println("it=" + pan(String.valueOf(i), 4) + memString());
                }
            }
        }
        
    }
    
    private static String pan(String str, int i) {
        while (str.length()<i) {
            str = " "+str;
        }
        return str;
    }

    private static String memString() {
        StringBuffer sb = new StringBuffer();
        for (MemoryPoolMXBean pool :
            ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType().equals(MemoryType.HEAP)) {
                sb.append(" - ")
                .append(pool.getName())
                // usage includes unreachable objects that are not collected yet
                .append(": u=").append(toString(pool.getUsage()))
                // usage as of right after the last collection
                .append(" cu=").append(toString(pool.getCollectionUsage()))
                // the threshold set for notification
                .append(" th=").append(pool.getCollectionUsageThreshold()*100/pool.getUsage().getMax()+"%");
            }
        }   
        return sb.toString();
    }

    private static String toString(MemoryUsage memoryUsage) {
        String string = memoryUsage.getUsed()*100/memoryUsage.getMax()+"%";
        return pan(string, 4);
    }
}
