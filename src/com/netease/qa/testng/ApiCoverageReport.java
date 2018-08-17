package com.netease.qa.testng;

import com.netease.qa.testng.utils.ConfigReader;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.log4j.Logger;
import org.testng.*;
import org.testng.xml.XmlSuite;

import java.io.*;
import java.util.*;

/**
 * 统计API的覆盖率
 *
 * @author 孔庆云
 */
public class ApiCoverageReport implements IReporter {
    public  final String FILE_NAME = "api-coverage-report.html";
    private Logger logger = Logger.getLogger(ApiCoverageReport.class);
    private PrintWriter output;

    public static void main(String[] args) {
        ApiCoverageReport api = new ApiCoverageReport();
        api.generateReport(null, null, "test-output");
    }

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        ConfigReader cr = ConfigReader.getInstance();
        String apiUrls = cr.getApiUrl();
        String ignorePath = cr.getignorePath();
        JSONArray ignorePathJsonArray = getIgnorePathJsonArray(ignorePath);
        String JsonContext = ReadFile(outputDirectory + "/testng-api.json");
        JSONArray fileApiJsonArray = JSONArray.fromObject(JsonContext);
        System.out.println(fileApiJsonArray.toString());

        try {
            output = createWriter(outputDirectory);
        } catch (IOException e) {
            logger.error("output file", e);
            return;
        }
        startHtml(output);
        // 调用接口获取当前服务的最新接口数据
//        String[] urls = new String[]{"http://c.x.test.you.163.com/dealer-app-api/v2/api-docs","http://c.x.test.you.163.com/dealer-member-api/v2/api-docs"};
        String[] urls = apiUrls.split(",");
        for(String url: urls) {
            String serverUrl = url.split("/")[4];
            String[] arrayUrl = serverUrl.split(".");
            String server = "";
            if(arrayUrl.length == 0) {
                 server = serverUrl;
            } else {
                 server = arrayUrl[1];
            }
            int serverAllApiCount = 0;
            int coverageApiCount = 0;
            String isCoverage = "";
            tableStart();
            output.print("<th>Method</th>");
            output.print("<th>API</th>");
            output.print("<th>是否覆盖</th>");
            JSONArray apiJsonArray = new JSONArray();
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().get().url(url).build();
            Call call = client.newCall(request);
            try {
                Response response = call.execute();
                JSONObject jsonObject = JSONObject.fromObject(response.body().string());
                JSONObject pathsJson = jsonObject.getJSONObject("paths");
                Iterator<String> keys = pathsJson.keys();
                //循环获取API的PATH，比如"/api/v1/authority/mobile"
                while (keys.hasNext()) {
                    String api = keys.next();
                    JSONObject apiJson = pathsJson.getJSONObject(api);
                    Iterator<String> apiJsonKeys = apiJson.keys();
                    //循环获取当前path的请求方式，比如get、post
                    while(apiJsonKeys.hasNext()){
                        serverAllApiCount++;
                        String method = apiJsonKeys.next();
                        JSONObject apiMethod = new JSONObject();
                        apiMethod.put("method",method);
                        apiMethod.put("api",api);
                        apiJsonArray.add(apiMethod);
                        output.print("<tr>");
                        output.print("<td>" + method + "</td>");
                        output.print("<td>" + api + "</td>");
                        if(fileApiJsonArray.contains(apiMethod)) {
                            coverageApiCount++;
                            isCoverage = "是";
                        }
                        else if(ignorePathJsonArray.contains(apiMethod)){
                            coverageApiCount++;
                            isCoverage = "忽略";
                        } else {
                            isCoverage = "";
                        }
                        output.print("<td>" + isCoverage + "</td>");
                        output.print("</tr>");
                    }
                }
                tableEnd();
                System.out.println("coverageApiCount=" + coverageApiCount);
                System.out.println("serverAllApiCount=" + serverAllApiCount);

                output.println(server + "接口覆盖率:" + (coverageApiCount*100/serverAllApiCount) + "%");
                output.println("<br>");
                output.println("<br>");
            } catch (Exception e) {
                logger.info(e.getMessage());
            }
        }

        endHtml(output);
        output.flush();
        output.close();

    }


    /**
     * 获取需要忽略的json数组
     * @param ignorePath
     * @return
     */
    private JSONArray getIgnorePathJsonArray(String ignorePath) {
        String[] ignorePaths = ignorePath.split(",");
        JSONArray ignorePathArray = new JSONArray();
        for(String path: ignorePaths) {
            addJsonArray("get",path, ignorePathArray);
            addJsonArray("put",path, ignorePathArray);
            addJsonArray("post",path, ignorePathArray);
            addJsonArray("delete",path, ignorePathArray);
            addJsonArray("head",path, ignorePathArray);
            addJsonArray("options",path, ignorePathArray);
            addJsonArray("patch",path, ignorePathArray);
        }
        return ignorePathArray;
    }

    private void addJsonArray(String method, String path, JSONArray ignorePathArray) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method",method);
        jsonObject.put("api",path);
        ignorePathArray.add(jsonObject);
    }

    public String ReadFile(String Path){
        BufferedReader reader = null;
        String laststr = "";
        try{
            FileInputStream fileInputStream = new FileInputStream(Path);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            reader = new BufferedReader(inputStreamReader);
            String tempString = null;
            while((tempString = reader.readLine()) != null){
                laststr += tempString;
            }
            reader.close();
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return laststr;
    }





    protected PrintWriter createWriter(String outdir) throws IOException {
        new File(outdir).mkdirs();
        return new PrintWriter(new BufferedWriter(new FileWriter(new File(outdir, "/" + FILE_NAME))));
    }



    /** Starts HTML stream */
    protected void startHtml(PrintWriter out) {
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");
        out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        out.println("<head>");
        out.println("<title>TestNG Report</title>");
        out.println("<style type=\"text/css\">");
        out.println("table {margin-bottom:10px;border-collapse:collapse;empty-cells:show}");
        out.println("td,th {border:1px solid #009;padding:.25em .5em}");
        out.println(".result th {vertical-align:bottom}");
        out.println(".param th {padding-left:1em;padding-right:1em}");
        out.println(".param td {padding-left:.5em;padding-right:2em}");
        out.println(".stripe td,.stripe th {background-color: #E6EBF9}");
        out.println(".numi,.numi_attn {text-align:right}");
        out.println(".total td {font-weight:bold}");
        out.println(".passedodd td {background-color: #0A0}");
        out.println(".passedeven td {background-color: #3F3}");
        out.println(".skippedodd td {background-color: #CCC}");
        out.println(".skippedodd td {background-color: #DDD}");
        out.println(".failedodd td,.numi_attn {background-color: #F33}");
        out.println(".failedeven td,.stripe .numi_attn {background-color: #D00}");
        out.println(".stacktrace {white-space:pre;font-family:monospace}");
        out.println(".totop {font-size:85%;text-align:center;border-bottom:2px solid #000}");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
    }

    /** Finishes HTML stream */
    protected void endHtml(PrintWriter out) {
        out.println("</body></html>");
    }

    private void tableStart() {
        output.println("<table>");
    }
    private void tableEnd() {
        output.println("</table>");
    }



}
