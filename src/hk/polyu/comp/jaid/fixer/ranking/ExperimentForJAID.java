package hk.polyu.comp.jaid.fixer.ranking;

import hk.polyu.comp.jaid.fixer.Application;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;


public class ExperimentForJAID {
    public static void main(String[] args) {
        for(int i = 1; i < 3; i++){
            File file = new File("C:/Users/HDULAB601/Desktop/jaid/jaid/Overfitting/example" + i);
            if(file.exists()){
                System.out.println(file.getName());
                String[] strings = {"--JaidSettingFile", "C:/Users/HDULAB601/Desktop/jaid/jaid/Overfitting/example" + i +"/af_test/af_test.properties"};
                try {
                    Application application = new Application();
                    application.main(strings);
                    System.out.println("sdagf + asflkjhdf");
//                    System.exit(0);
                }catch (Exception e){
                    e.printStackTrace();
                }
                finally {
                    System.out.println("dsg");
                    continue;
                }

            }
        }
    }
    public static void testTimer1() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            int i = 175;
            public void run() {
                File file = new File("C:/Users/HDULAB601/Desktop/jaid/jaid/Overfitting/example" + i);
                if(file.exists()){
                    String[] strings = {"--JaidSettingFile", "C:/Users/HDULAB601/Desktop/jaid/jaid/Overfitting/example" + i +"/af_test/af_test.properties"};
                    Application.main(strings);
                }
                i++;
                if(i == 181)timer.cancel();
            }
        }, 350, 3 * 60 * 1000);
        // 设定指定的时间time为3500毫秒
    }
}
