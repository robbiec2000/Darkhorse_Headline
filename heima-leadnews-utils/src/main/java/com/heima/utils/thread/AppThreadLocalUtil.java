package com.heima.utils.thread;

import com.heima.model.user.pojos.ApUser;
import com.heima.model.wemedia.pojos.WmUser;

public class AppThreadLocalUtil {
    private final static ThreadLocal<ApUser> AP_USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void setUser(ApUser apUser){
        AP_USER_THREAD_LOCAL.set(apUser);
    }

    public static ApUser getUser(){
        return AP_USER_THREAD_LOCAL.get();
    }

    public static void clear(){
        AP_USER_THREAD_LOCAL.remove();
    }
}
