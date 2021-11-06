package com.qilinxx.rms;

import com.qilinxx.rms.domain.model.UserInfo;
import com.qilinxx.rms.domain.model.eo.UserInfoEo;
import com.qilinxx.rms.util.DateKit;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.*;

/**
 * @Auther: dqsdc
 * @Date: 2019-03-25 15:42
 * @Description:
 */
public class Test {
    public static void main(String args[]) {
        List<String> list1 = new ArrayList<>();
        list1.add("a1");
        list1.add("a2");
        list1.add("a3");
        list1.add("a11");
        list1.add("a21");
        list1.add("a31");
        List<String> list2 = new ArrayList<>();
        list2.add("5");
        list2.add("a2");
        list2.add("a3");

        for (String s : list1) {
            for (String s2 : list2) {
                if (s.equals(s2)){
                    list1.remove(s);
                    System.out.println(s);
                }
            }
        }
        System.out.println();
        for (String s : list1) {
            System.out.println(s);
        }

    }


    public static void test() throws IllegalAccessException {
        UserInfo u1 = new UserInfo();
        u1.setName("Tom");
        u1.setPassword("123456");
        UserInfoEo eo = new UserInfoEo();
        Field[] fields = u1.getClass().getDeclaredFields();
        System.out.println(Arrays.toString(fields));

        Field[] fields2 = eo.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++)
            for (int j = 0; j < fields2.length; j++) {
                fields[i].setAccessible(true);
                fields2[j].setAccessible(true);
                if (fields[i].getName().equals(fields2[j].getName())) {
                    fields2[j].set(eo, fields[i].get(u1));
                }
            }
        System.out.println(eo);

    }
}
