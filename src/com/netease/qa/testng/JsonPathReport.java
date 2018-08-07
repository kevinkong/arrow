package com.netease.qa.testng;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.testng.*;
import org.testng.collections.Lists;
import org.testng.internal.Utils;
import org.testng.xml.XmlSuite;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * 读取接口测试注释中的@path和@method标签，用于生成当前测试代码中的接口数据到json文件
 * 方便自动统计当前已实现的接口，和开发的所有接口进行对比统计接口覆盖率
 *
 * @author 孔庆云
 */
public class JsonPathReport implements IReporter {
    public static final String FILE_NAME = "testng-apipaths.json";
    private Set<Integer> testIds = new HashSet<Integer>();
    private JavaProjectBuilder builder = new JavaProjectBuilder();
    private JSONArray pathJsonArray = new JSONArray();
    private static Logger logger = Logger.getLogger(JsonPathReport.class);

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {

        for (ISuite suite : suites) {
            Map<String, ISuiteResult> r = suite.getResults();
            for (ISuiteResult r2 : r.values()) {
                ITestContext testContext = r2.getTestContext();
                resultSummary(suite, testContext.getSkippedTests());
                resultSummary(suite, testContext.getFailedTests());
                resultSummary(suite, testContext.getPassedTests());
            }
        }

        try{
            File file = new File(outputDirectory + "/" + FILE_NAME);
            file.createNewFile();
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            output.write(pathJsonArray.toString());
            output.close();
        } catch (Exception e) {
            System.out.println("create json File error=" + e.getMessage());
        }


    }


    /**
     * 创建接口的json数据
     * @param suite
     * @param tests
     */
    private void resultSummary(ISuite suite, IResultMap tests) {
        for (ITestNGMethod method : getMethodSet(tests, suite)) {
            ITestClass testClass = method.getTestClass();
            String className = testClass.getName();
            String[] paths = getApis(className, method);
            JSONObject JsonObject = new JSONObject();
            JsonObject.put("path", paths[0]);
            JsonObject.put("method", paths[1]);
            logger.info("path method=" + JsonObject.toString());
            pathJsonArray.add(JsonObject);
        }
    }


    /**
     * 获取注释中配置的path和method变量
     *
     * @param className
     * @param method
     * @return
     */
    private String[] getApis(String className, ITestNGMethod method) {
        JavaClass cls = builder.getClassByName(className);
        String[] allpaths = new String[2];

        // get method path name
        List<JavaMethod> mtds = cls.getMethods();
        for (JavaMethod mtd : mtds) {
            if (mtd.getName().equals(method.getMethodName())) {
                logger.info("getClassByName=" + cls.getName());
                logger.info("getMethodName=" + method.getMethodName());
                DocletTag api = mtd.getTagByName("api");
//                logger.info("api=" + api.getValue());
//                logger.info("api2=" + api.getParameters());
                if(api != null) {
//                    String path = api.getNamedParameter("path");
//                    String methodType = api.getNamedParameter("method");
                    String path = api.getValue();
                    String methodType = api.getValue();
                    logger.info("path=" + path);
                    logger.info("method=" + methodType);
                    allpaths[0] = path;
                    allpaths[1] = methodType;
                }
                break;
            }
        }
        return allpaths;
    }

    /**
     * Since the methods will be sorted chronologically, we want to return the
     * ITestNGMethod from the invoked methods.
     */
    private Collection<ITestNGMethod> getMethodSet(IResultMap tests, ISuite suite) {
        List<IInvokedMethod> r = Lists.newArrayList();
        List<IInvokedMethod> invokedMethods = suite.getAllInvokedMethods();

        // Eliminate the repeat retry methods
        for (IInvokedMethod im : invokedMethods) {
            if (tests.getAllMethods().contains(im.getTestMethod())) {
                int testId = getId(im.getTestResult());
                if (!testIds.contains(testId)) {
                    testIds.add(testId);
                    r.add(im);
                }
            }
        }
        Arrays.sort(r.toArray(new IInvokedMethod[r.size()]), new JsonPathReport.TestSorter());
        List<ITestNGMethod> result = Lists.newArrayList();

        // Add all the invoked methods
        for (IInvokedMethod m : r) {
            result.add(m.getTestMethod());
        }

        for (ITestResult allResult : tests.getAllResults()) {
            int testId = getId(allResult);
            if (!testIds.contains(testId)) {
                result.add(allResult.getMethod());
            }
        }

        return result;
    }


    /**
     * Get ITestResult id by class + method + parameters hash code.
     *
     * @param result
     * @return
     * @author kevinkong
     */
    private int getId(ITestResult result) {
        int id = result.getTestClass().getName().hashCode();
        id = id + result.getMethod().getMethodName().hashCode();
        id = id + (result.getParameters() != null ? Arrays.hashCode(result.getParameters()) : 0);
        return id;
    }


    // ~ Inner Classes --------------------------------------------------------

    /**
     * Arranges methods by classname and method name
     */
    private class TestSorter implements Comparator<IInvokedMethod> {
        // ~ Methods
        // -------------------------------------------------------------

        /**
         * Arranges methods by classname and method name
         */
        @Override
        public int compare(IInvokedMethod o1, IInvokedMethod o2) {
            // System.out.println("Comparing " + o1.getMethodName() + " " +
            // o1.getDate()
            // + " and " + o2.getMethodName() + " " + o2.getDate());
            return (int) (o1.getDate() - o2.getDate());
            // int r = ((T) o1).getTestClass().getName().compareTo(((T)
            // o2).getTestClass().getName());
            // if (r == 0) {
            // r = ((T) o1).getMethodName().compareTo(((T) o2).getMethodName());
            // }
            // return r;
        }
    }

}
