package model;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.mllib.recommendation.ALS;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import org.apache.spark.rdd.RDD;
import scala.Tuple2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ALSRecommend implements Serializable {
    private MatrixFactorizationModel model;

    public ALSRecommend(List txtList) {
        SparkConf conf = new SparkConf().setAppName("Test").setMaster("local[2]")
                .set("spark.serializer","org.apache.spark.serializer.KryoSerializer");
        JavaSparkContext jsc = new JavaSparkContext(conf);
        JavaRDD<String> lines =  jsc.parallelize(txtList);
        // 映射
        RDD<Rating> ratingRDD = lines.map(new Function<String, Rating>() {
            public Rating call(String line) throws Exception {
                String[] arr = line.split(",");
                return new Rating(new Integer(arr[0]), new Integer(arr[1]), Double.parseDouble(arr[2]));
            }
        }).rdd();
        model = ALS.train(ratingRDD, 10, 10,0.01);
    }

    //向指定id的用户推荐n件商品
    public List<Integer> recommendProducts(int uid, int num) {
        Rating[] predictProducts = model.recommendProducts(uid, num);
        List<Integer> list = new ArrayList<>();
        for(Rating r1:predictProducts){
            list.add(r1.product());
        }
        return list;
    }

    //向指定id的商品推荐给n给用户
    public List<Integer> recommendUsers(int pid, int num) {
        Rating[] predictUser = model.recommendUsers(pid, num);
        List<Integer> list = new ArrayList<>();
        for(Rating r1:predictUser){
            list.add(r1.user());
        }
        return list;
    }

    // 向所有用户推荐N个商品
    public Map<Integer,Integer> recommendProductsForUsers(int num) {
        Map<Integer,Integer> map = new HashMap<>();
        RDD<Tuple2<Object, Rating[]>> predictProductsForUsers = model.recommendProductsForUsers(num);
        predictProductsForUsers.toJavaRDD().foreach(new VoidFunction<Tuple2<Object, Rating[]>>() {
            public void call(Tuple2<Object, Rating[]> tuple2) throws Exception {
                for(Rating r1:tuple2._2){
                    map.put(r1.user(),r1.product());
                }
            }
        });
        return map;
    }

    // 将所有商品推荐给n个用户
    public Map<Integer,Integer> recommendUsersForProducts(int num) {
        Map<Integer,Integer> map = new HashMap<>();
        RDD<Tuple2<Object, Rating[]>> predictUsersForProducts = model.recommendUsersForProducts(num);
        predictUsersForProducts.toJavaRDD().foreach(new VoidFunction<Tuple2<Object, Rating[]>>() {
            public void call(Tuple2<Object, Rating[]> tuple2) throws Exception {
                for(Rating r1:tuple2._2){
                    map.put(r1.user(),r1.product());
                }
            }
        });
        return map;
    }

}
