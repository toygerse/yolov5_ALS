package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

/*
通过读用户浏览记录表
user_id pid
0       1
1       2
计算用户对于计划评分（基于运动标签）
计算过程如下：
求用户浏览所有计划中个标签占比，计划得分=max（所含各标签占比）
生成txt文件，格式为
user_id pid score
0       1    3.98
1       2    2.74
 */
public class DataBase {
    private static final String URL = "jdbc:mysql://localhost/health?useUnicode = true & characterEncoding = utf-8 & useSSL = false & &serverTimezone = GMT";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    private Connection connection = null;
    private int sportType = 10;

    private List<Long> userInfo = null;
    private Map<Long,List<Long>> uidToPid;
    private Map<Long,List<Integer>> pidToSportType;
    private List<String> messageList;

    private String recordTable = "memory";//浏览记录数据表
    //private String txtFolderName = "glanceScoreData";//输出txt文件夹
    //private String fileName = "score.txt";//输出txt文件名

    public DataBase() {
        userInfo = new ArrayList();
        uidToPid = new HashMap<>();
        pidToSportType = new HashMap<>();
        try {
            connection = DriverManager.getConnection( URL,USERNAME, PASSWORD );
            messageList = new ArrayList<>();
            PreparedStatement select = connection.prepareStatement("select distinct user_id from memory;");
            ResultSet resultSet = select.executeQuery();
            while (resultSet.next()) {
                userInfo.add(resultSet.getLong("user_id"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public ResultSet fromUidGetPid(long userId) {
        PreparedStatement select = null;
        ResultSet resultSet = null;
        try {
            select = connection.prepareStatement("select * from " + recordTable + " where user_id=" + String.valueOf(userId));
            resultSet = select.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultSet;
    }

    public ResultSet fromPidGetSportType(Long pid) {
        PreparedStatement select = null;
        ResultSet resultSet = null;
        try {
            select = connection.prepareStatement("select sport_type from p_sport_dtl where pid=" + String.valueOf(pid));
            resultSet = select.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultSet;
    }

    public int fromPidGetSportCal(int pid) {
        int allSportCal = -1;
        int totalDay = -1;
        PreparedStatement select = null;
        ResultSet resultSet = null;
        try {
            select = connection.prepareStatement("select sum(total_kcal) from p_sport_sta where pid=" + String.valueOf(pid));
            resultSet = select.executeQuery();
            while (resultSet.next()) {
                allSportCal = resultSet.getInt(1);
            }
            select = connection.prepareStatement("select sum(total_day) from p_sport_dtl where pid=" + String.valueOf(pid));
            resultSet = select.executeQuery();
            while (resultSet.next()) {
                totalDay = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allSportCal/totalDay;
    }

    public ResultSet fromPidGetFoodCal(int pid) {
        PreparedStatement select = null;
        ResultSet resultSet = null;
        try {
            select = connection.prepareStatement("select k_cal from p_recipe_dtl where pid=" + String.valueOf(pid));
            resultSet = select.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultSet;
    }

    public void writeToTxt(String folder, String file, List<String> messageList) throws FileNotFoundException {

        File files = new File(folder);
        files.mkdirs();
        FileOutputStream fos = new FileOutputStream(folder + File.separator + file);

        // 逐行写入
        PrintWriter pw = new PrintWriter(fos);
        for (String message : messageList) {
            pw.println(message);
        }
        pw.close();

    }

    //获得用户对于每个健身标签的浏览比例
    public double[] makeSportTypeScore(long userId) {
        ResultSet resultSet = this.fromUidGetPid(userId);
        double[] array = new double[sportType];
        List<Long> pidSet = new ArrayList<>();//uid下pid的集合
        int sum = 0;
        try {
            while (resultSet.next()) {
                Long pid = resultSet.getLong("pid");
                List<Integer> sportType = new ArrayList<>();
                pidSet.add(pid);
                ResultSet resultSet1 = this.fromPidGetSportType(pid);
                while (resultSet1.next()) {
                    int type = resultSet1.getInt("sport_type");
                    sportType.add(type);
                    array[type]++;
                    sum++;
                }
                pidToSportType.put(pid,sportType);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (int n=0;n<sportType;n++) {
            array[n] = array[n]/sum;
        }
        uidToPid.put(userId,pidSet);
        return array;
    }

    public List getScore() {
        Map<Integer,Map<Integer,Double>> map = new HashMap<>();
        for (long userId:userInfo) {
            double[] typeScore = makeSportTypeScore(userId);//获取该用户标签评分
            List<Long> pidSet = uidToPid.get(userId);//获取该用户浏览计划
            for (long pid:pidSet) {
                double temp = 0;//记录计划下标签评分
                int count = 0;
                List<Integer> typeSet = pidToSportType.get(pid);
                for (int type:typeSet) {
                    temp += typeScore[type];
                    count++;
                }
                double score = temp/count;
                String message = String.valueOf(userId) + "," + String.valueOf(pid) + "," + String.valueOf(score*10);
                messageList.add(message);
            }
        }
        return messageList;
    }
}
