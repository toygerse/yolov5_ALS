package FoodDetect;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.translator.YoloV5Translator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Pipeline;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;


import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class FoodDetect {
    private int imageSize = 640;
    private List index = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");//标签索引，共10类食物
    /*
    model文件下存放模型权重，类别文本文件，卡路里文本文件
     */
    private String modelFile = "FoodDetect/model/best.torchscript.pt";
    private String labelFile = "FoodDetect/model/voc_classes.txt";
    private String calorieFile = "FoodDetect/model/calorie.txt";

    private Predictor<Image, DetectedObjects> predictor;

    public FoodDetect() {
        Pipeline pipeline = new Pipeline();
        pipeline.add(new Resize(imageSize));
        pipeline.add(new ToTensor());

        Translator<Image, DetectedObjects> translator =  YoloV5Translator
                .builder()
                .setPipeline(pipeline)
                .optSynset(index)
                .optThreshold(0.5f)
                .build();

        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optModelPath(Paths.get(modelFile))
                .optTranslator(translator)
                .optProgress(new ProgressBar())
                .build();

        try {
            ZooModel<Image,DetectedObjects> model = ModelZoo.loadModel(criteria);
            predictor = model.newPredictor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ModelNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedModelException e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> detect(MultipartFile file) {
        byte [] byteArr = new byte[0];
        try {
            byteArr = file.getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStream inputStream = new ByteArrayInputStream(byteArr);
        Image img = null;
        DetectedObjects results = null;
        try {
            img = ImageFactory.getInstance().fromInputStream(inputStream);
            results = predictor.predict(img);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TranslateException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        List label = readTxt(new File(labelFile));
        List calorie = readTxt(new File(calorieFile));
        for (Classifications.Classification item : results.items()) {
            Map<String, Object> info = new HashMap<>();
            info.put("label", label.get(Integer.valueOf(item.getClassName())));
            info.put("calorie",calorie.get(Integer.valueOf(item.getClassName())));
            list.add(info);
        }
        return list;
    }

    public List<Map<String, Object>> detect(InputStream fileString) {
        Image img = null;
        DetectedObjects results = null;
        try {
            img = ImageFactory.getInstance().fromInputStream(fileString);
            results = predictor.predict(img);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TranslateException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        List label = readTxt(new File(labelFile));
        List calorie = readTxt(new File(calorieFile));
        for (Classifications.Classification item : results.items()) {
            Map<String, Object> info = new HashMap<>();
            info.put("label", label.get(Integer.valueOf(item.getClassName())));
            info.put("calorie",calorie.get(Integer.valueOf(item.getClassName())));
            list.add(info);
        }
        return list;
    }

    public static List<String> readTxt(File file){

        List<String> list = new ArrayList<>();
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));
            String s = null;
            while((s = br.readLine())!=null){
                list.add(s);
            }
            br.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return list;
    }
}
