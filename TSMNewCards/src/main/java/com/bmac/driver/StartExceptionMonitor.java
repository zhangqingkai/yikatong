package com.bmac.driver;

import com.bmac.sparktask.TenMinuteExceptionMonitor;

/**
 * @Author: zqk
 * @Date: 2018/12/06 17:17
 * @Description:
 */

public class StartExceptionMonitor {
    public static void main(String[] args) {
        TenMinuteExceptionMonitor tenMinuteExceptionMonitor = new TenMinuteExceptionMonitor();
        tenMinuteExceptionMonitor.tenMinuteExceptionMonitor();
    }
}
