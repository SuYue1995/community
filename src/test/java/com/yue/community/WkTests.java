package com.yue.community;

import java.io.IOException;

public class WkTests {
    public static void main(String[] args) {
        String cmd = "d:/Program Files/wkhtmltopdf/bin/wkhtmltoimage --quality 75 http://www.baidu.com f:/work/project/data/wk-images/3.png";
        try {
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
